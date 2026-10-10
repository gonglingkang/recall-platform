package com.recall.service.objectives.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recall.common.exception.BusinessException;
import com.recall.common.api.ResultCode;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.objectives.ObjectiveImportItemReq;
import com.recall.dto.objectives.ObjectiveImportParseReq;
import com.recall.dto.objectives.ObjectiveImportReq;
import com.recall.service.llm.LlmService;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveImportService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.vo.objectives.ObjectiveImportPreviewVO;
import com.recall.vo.objectives.ObjectiveImportResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 绩效目标导入 Service 实现。
 * <p>
 * parse：LLM 结构化输出（严格 JSON 数组），解析失败/为空抛 4902；
 * importObjectives：复用 ObjectiveService/KeyResultService 的创建入口，
 * 同月目标名唯一校验天然生效，任一冲突整批回滚。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectiveImportServiceImpl implements ObjectiveImportService {

    private static final String PARSE_SYSTEM_PROMPT = """
            你是绩效目标数据解析助手。把用户粘贴的月度绩效表格文本解析为 JSON。
            表格每行结构：维度名称<TAB>任务目标O<TAB>关键成果<TAB>KR权重。其中关键成果单元格可能包含多行：多条时以"1、2、3、"编号，也可能是一段无编号的文字。
            要求：
            1. 维度名称可能因合并单元格只在组的第一行出现，后续行为空，需沿用上一非空行的维度；
            2. 关键成果按编号拆分为数组；无编号的整段文字视为单条；
            3. 权重保留原文（如 "50%"）；
            4. 跳过空行、表头和与绩效目标无关的行；
            5. 目标行没有关键成果内容时，krs 返回空数组（允许只有目标没有关键成果）；
            6. 只输出 JSON 数组，不要输出任何解释或代码块标记，格式：
            [{"dimension":"...","objective":"...","weight":"50%","krs":["...","..."]}]""";

    private final LlmService llmService;
    private final ObjectiveService objectiveService;
    private final KeyResultService keyResultService;
    private final ObjectMapper objectMapper;

    @Override
    public List<ObjectiveImportPreviewVO> parse(ObjectiveImportParseReq req) {
        String output = llmService.chat(PARSE_SYSTEM_PROMPT, req.getText());
        JsonNode root;
        try {
            root = objectMapper.readTree(stripCodeFence(output));
        } catch (Exception e) {
            log.warn("绩效导入解析结果非JSON: {}", abbreviate(output));
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID, "解析失败，模型未返回有效结构，请重试");
        }
        if (!root.isArray()) {
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID, "解析失败，模型未返回有效结构，请重试");
        }
        List<ObjectiveImportPreviewVO> result = new ArrayList<>();
        for (JsonNode node : root) {
            String name = textOf(node, "objective");
            // 只有无名目标才跳过；无关键成果的空数组保留（支持只建 O 不建 K）
            if (name.isBlank()) {
                continue;
            }
            result.add(ObjectiveImportPreviewVO.builder()
                    .name(name)
                    .dimension(textOf(node, "dimension"))
                    .weight(textOf(node, "weight"))
                    .keyResults(stringListOf(node, "krs"))
                    .build());
        }
        if (result.isEmpty()) {
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID, "未从文本中解析出绩效目标，请检查粘贴内容");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ObjectiveImportResultVO importObjectives(ObjectiveImportReq req) {
        if (req.isReplace()) {
            objectiveService.clearMonth(req.getMonth());
        }
        int krCount = 0;
        for (ObjectiveImportItemReq item : req.getObjectives()) {
            ObjectiveVO created = objectiveService.create(buildObjectiveReq(req.getMonth(), item));
            for (String krName : item.getKeyResults()) {
                if (krName == null || krName.isBlank()) {
                    continue;
                }
                KeyResultCreateReq krReq = new KeyResultCreateReq();
                krReq.setObjectiveId(created.getId());
                krReq.setName(krName.trim());
                // 导入的 KR 无独立描述来源，描述默认取名称
                krReq.setDescription(krName.trim());
                keyResultService.create(krReq);
                krCount++;
            }
        }
        return ObjectiveImportResultVO.builder()
                .objectives(req.getObjectives().size())
                .keyResults(krCount)
                .build();
    }

    /** 维度/权重平台无独立字段，折叠进目标描述作为参考信息 */
    private ObjectiveCreateReq buildObjectiveReq(String month, ObjectiveImportItemReq item) {
        StringBuilder description = new StringBuilder();
        if (item.getDimension() != null && !item.getDimension().isBlank()) {
            description.append("维度：").append(item.getDimension().trim());
        }
        if (item.getWeight() != null && !item.getWeight().isBlank()) {
            if (description.length() > 0) {
                description.append("；");
            }
            description.append("权重：").append(item.getWeight().trim());
        }
        ObjectiveCreateReq req = new ObjectiveCreateReq();
        req.setMonth(month);
        req.setName(item.getName().trim());
        req.setDescription(description.length() > 0 ? description.toString() : null);
        return req;
    }

    /** 兼容模型偶尔输出的 markdown 代码块包裹 */
    private String stripCodeFence(String output) {
        String t = output == null ? "" : output.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private String textOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText().trim();
    }

    private List<String> stringListOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        List<String> list = new ArrayList<>();
        if (v != null && v.isArray()) {
            for (JsonNode e : v) {
                String s = e.isNull() ? "" : e.asText().trim();
                if (!s.isBlank()) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    private String abbreviate(String s) {
        return s == null ? "" : (s.length() <= 200 ? s : s.substring(0, 200) + "...");
    }
}
