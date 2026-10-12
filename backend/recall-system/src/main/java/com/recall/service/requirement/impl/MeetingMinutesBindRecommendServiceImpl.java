package com.recall.service.requirement.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.enums.RequirementStatus;
import com.recall.jev.JevClient;
import com.recall.jev.model.ChoiceAnswer;
import com.recall.jev.model.Question;
import com.recall.jev.model.SystemOneResponse;
import com.recall.service.requirement.MeetingMinutesBindRecommendService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.MeetingMinutesRecommendVO;
import com.recall.vo.requirement.RequirementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会议纪要 AI 推荐需求 Service 实现（jev 模型）。
 * <p>
 * 候选 = 当前用户「讨论中/进行中/开发完成」的需求；一道 choice 题
 * 让模型按纪要标题选最相关需求，命中标签映射回需求。推荐仅供参考，不落库。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingMinutesBindRecommendServiceImpl implements MeetingMinutesBindRecommendService {

    private static final String QUESTION_NAME = "req";

    /** 候选之外的兜底选项：模型判定都不合适时选它，返回 null 不推荐 */
    private static final String NONE_LABEL = "NONE";

    /** 可绑定候选状态：讨论中 / 进行中 / 开发完成 */
    private static final java.util.Set<RequirementStatus> BINDABLE_STATUSES =
            EnumSet.of(RequirementStatus.DISCUSSING, RequirementStatus.IN_PROGRESS, RequirementStatus.DEV_DONE);

    private final RequirementService requirementService;
    private final ObjectProvider<JevClient> jevClientProvider;

    @Override
    public MeetingMinutesRecommendVO recommend(String minutesTitle) {
        JevClient jevClient = jevClientProvider.getIfAvailable();
        if (jevClient == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "未启用 jev 模型（recall.jev.enabled=false），无法推荐");
        }
        if (minutesTitle == null || minutesTitle.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "纪要标题不能为空");
        }

        java.util.List<RequirementVO> options = requirementService.listByStatuses(BINDABLE_STATUSES);
        if (options.isEmpty()) {
            // 无可选需求不算异常：返回 null（前端不显示推荐并引导先建需求）
            return null;
        }

        Map<String, RequirementVO> byLabel = new LinkedHashMap<>();
        Map<String, Object> criteria = new LinkedHashMap<>();
        for (RequirementVO option : options) {
            String label = "R" + option.getId();
            String category = joinCategory(option);
            String description = (category.isEmpty() ? "" : category + " / ") + option.getTitle();
            byLabel.put(label, option);
            criteria.put(label, description);
        }
        // 兜底项：都不合适时模型选它 → 不返回推荐（宁可没有推荐，不硬凑）
        criteria.put(NONE_LABEL, "以上需求都不合适（本次纪要对应新需求或与其他需求无关）");

        Map<String, Object> state = Map.of("minutesTitle", minutesTitle.trim());
        try {
            SystemOneResponse response = jevClient.systemOne(state,
                    Map.of(QUESTION_NAME, Question.choice(
                            "该会议纪要的内容最相关、最应绑定到哪个需求？如果都不合适请选择 NONE。", criteria)));
            if (response.getAnswers().get(QUESTION_NAME) instanceof ChoiceAnswer answer) {
                if (NONE_LABEL.equals(answer.getChoice())) {
                    log.info("jev 判定纪要无合适需求: title={}", minutesTitle);
                    return null;
                }
                RequirementVO hit = byLabel.get(answer.getChoice());
                if (hit == null) {
                    log.warn("jev 推荐需求返回未知标签: choice={}", answer.getChoice());
                    throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
                }
                return MeetingMinutesRecommendVO.builder()
                        .requirementId(hit.getId())
                        .requirementTitle(hit.getTitle())
                        .categoryName(joinCategory(hit))
                        .status(hit.getStatus())
                        .confidence(answer.getConfidence())
                        .build();
            }
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("jev 推荐需求调用失败: title={}, err={}", minutesTitle, e.getMessage());
            throw new BusinessException(ResultCode.LLM_CALL_FAILED);
        }
    }

    /** 主分类/子分类 展示名（都空返回空串） */
    private String joinCategory(RequirementVO option) {
        String main = option.getCategoryName() == null ? "" : option.getCategoryName();
        String sub = option.getSubCategoryName() == null ? "" : option.getSubCategoryName();
        if (main.isEmpty()) {
            return sub;
        }
        return sub.isEmpty() ? main : main + "/" + sub;
    }
}
