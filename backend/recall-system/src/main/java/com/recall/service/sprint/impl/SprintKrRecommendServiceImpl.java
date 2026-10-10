package com.recall.service.sprint.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.entity.sprint.SprintItem;
import com.recall.jev.JevClient;
import com.recall.jev.model.ChoiceAnswer;
import com.recall.jev.model.Question;
import com.recall.jev.model.SystemOneResponse;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.sprint.SprintKrRecommendService;
import com.recall.service.sprint.SprintService;
import com.recall.vo.objectives.KeyResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import com.recall.vo.sprint.SprintKrRecommendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 冲刺任务 AI 推荐关键成果 Service 实现（jev 模型）。
 * <p>
 * 素材 = 任务标题/备注 + 当月全部有效 KR（含目标名）；用一道 choice 题让模型
 * 在全部 KR 中选归属，命中标签映射回 KR。推荐仅供参考，不落库。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SprintKrRecommendServiceImpl implements SprintKrRecommendService {

    private static final String QUESTION_NAME = "kr";

    private final SprintService sprintService;
    private final ObjectiveService objectiveService;
    private final ObjectProvider<JevClient> jevClientProvider;

    @Override
    public SprintKrRecommendVO recommend(Long sprintItemId) {
        JevClient jevClient = jevClientProvider.getIfAvailable();
        if (jevClient == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "未启用 jev 模型（recall.jev.enabled=false），无法推荐");
        }
        SprintItem item = sprintService.getById(sprintItemId, true);

        // 当月有效 KR 平铺：label = "K{id}"；同时建 krId → 目标名 映射（展示用）
        Map<String, KeyResultVO> krByLabel = new LinkedHashMap<>();
        Map<Long, String> objectiveNameByKrId = new HashMap<>();
        Map<String, Object> criteria = new LinkedHashMap<>();
        for (ObjectiveVO objective : objectiveService.list(item.getMonth())) {
            for (KeyResultVO kr : objective.getKeyResults()) {
                if ("cancelled".equals(kr.getStatus())) {
                    continue;
                }
                String label = "K" + kr.getId();
                String description = kr.getName();
                if (kr.getDescription() != null && !kr.getDescription().isBlank()) {
                    description = description + "——" + kr.getDescription();
                }
                krByLabel.put(label, kr);
                objectiveNameByKrId.put(kr.getId(), objective.getName());
                criteria.put(label, objective.getName() + " / " + description);
            }
        }
        if (krByLabel.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "该月暂无绩效关键成果，无法推荐");
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("taskTitle", item.getTitle());
        state.put("taskNote", item.getNote() == null ? "" : item.getNote());
        state.put("month", item.getMonth());

        try {
            SystemOneResponse response = jevClient.systemOne(state,
                    Map.of(QUESTION_NAME, Question.choice("该团队冲刺任务最应关联归属到哪个关键成果？", criteria)));
            if (response.getAnswers().get(QUESTION_NAME) instanceof ChoiceAnswer answer) {
                KeyResultVO kr = krByLabel.get(answer.getChoice());
                if (kr == null) {
                    log.warn("jev 推荐返回未知标签: choice={}", answer.getChoice());
                    throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
                }
                return SprintKrRecommendVO.builder()
                        .krId(kr.getId())
                        .krName(kr.getName())
                        .objectiveName(objectiveNameByKrId.get(kr.getId()))
                        .confidence(answer.getConfidence())
                        .build();
            }
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("jev 推荐调用失败: sprintItemId={}, err={}", sprintItemId, e.getMessage());
            throw new BusinessException(ResultCode.LLM_CALL_FAILED);
        }
    }
}
