package com.recall.service.daily.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dto.daily.DailyRewriteReq;
import com.recall.service.daily.DailyForbiddenWords;
import com.recall.service.daily.DailyRewriteService;
import com.recall.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 日报条目 AI 合规改写 Service 实现。
 * <p>
 * Prompt 素材 = 条目原文（主） + 待办标题/描述（辅）；要求只换措辞不换事实。
 * 改写结果复检禁用词，命中抛 4902 引导重试（前端另有重试按钮）。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRewriteServiceImpl implements DailyRewriteService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是日报措辞合规改写助手。公司规定日报中不得出现以下词汇：%s。
            请改写用户提供的日报条目：
            1. 保持事实与含义完全不变，不新增任何未提及的工作内容，不虚构细节；
            2. 只调整措辞，长度与原文相当；
            3. 常见替换方向（仅供参考，不限于）：优化/改造→完善、调整、演进；迭代→版本推进；
               升级→演进、版本更新；培训/学习→专项能力建设；PPT→汇报材料；新人→新成员；
               面试/答辩/晋升/晋级/竞赛→相应人事或评审活动的中性表述；工作交接→任务移交；
               技术交流→技术研讨；试点→先行验证；Deepseek→AI 工具；
            4. 输出仅改写后的条目文本本身，不要任何解释、引号或前后缀。""";

    private final LlmService llmService;
    private final DailyForbiddenWords forbiddenWords;

    @Override
    public String rewrite(DailyRewriteReq req) {
        if (forbiddenWords.getWords().isEmpty()) {
            // 未配置禁用词时改写无意义，直接原样返回
            return req.getContent().trim();
        }
        StringBuilder user = new StringBuilder("日报条目原文：").append(req.getContent().trim());
        if (req.getTodoTitle() != null && !req.getTodoTitle().isBlank()) {
            user.append("\n关联待办标题：").append(req.getTodoTitle().trim());
        }
        if (req.getTodoNote() != null && !req.getTodoNote().isBlank()) {
            user.append("\n关联待办描述：").append(req.getTodoNote().trim());
        }

        String output = llmService.chat(
                String.format(SYSTEM_PROMPT_TEMPLATE, String.join("、", forbiddenWords.getWords())),
                user.toString());
        if (output == null || output.isBlank()) {
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID, "改写结果为空，请重试");
        }
        List<String> hits = forbiddenWords.findHits(output);
        if (!hits.isEmpty()) {
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID,
                    "改写结果仍包含禁用词：" + String.join("、", hits) + "，请重试");
        }
        return output.trim();
    }
}
