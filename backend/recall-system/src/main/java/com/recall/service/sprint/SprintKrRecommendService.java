package com.recall.service.sprint;

import com.recall.vo.sprint.SprintKrRecommendVO;

/**
 * 冲刺任务 AI 推荐关键成果 Service（jev 模型辅助）。
 * <p>
 * 按任务标题/备注，从该任务所属月份的绩效关键成果中推荐最合适的一个，
 * 提升用户在「绑定关键成果」弹窗中的选择效率。推荐仅供参考，绑定仍由用户确认。
 *
 * @author recall
 */
public interface SprintKrRecommendService {

    /**
     * 推荐冲刺任务最应关联的关键成果。
     * <p>未启用 jev（recall.jev.enabled=false）或当月无关键成果时抛业务异常；
     * jev 调用失败包装为 4901。
     *
     * @param sprintItemId 冲刺任务 ID（校验归属当前用户）
     * @return 推荐的关键成果与置信度
     */
    SprintKrRecommendVO recommend(Long sprintItemId);
}
