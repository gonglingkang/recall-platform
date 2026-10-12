package com.recall.service.requirement;

import com.recall.vo.requirement.MeetingMinutesRecommendVO;

/**
 * 会议纪要 AI 推荐需求 Service（jev 模型辅助）。
 * <p>
 * 按纪要标题，从「讨论中/进行中/开发完成」的需求中推荐最合适的一个，
 * 提升绑定效率。推荐仅供参考，绑定仍由用户确认。
 *
 * @author recall
 */
public interface MeetingMinutesBindRecommendService {

    /**
     * 推荐纪要最应绑定的需求。
     * <p>模型可在候选外选「都不合适」，此时返回 null（宁可无推荐不硬凑）。
     * 无可选需求同样返回 null；未启用 jev、纪要标题为空抛业务异常；jev 调用失败包装为 4901。
     *
     * @param minutesTitle 纪要标题
     * @return 推荐的需求与置信度；无合适推荐返回 null
     */
    MeetingMinutesRecommendVO recommend(String minutesTitle);
}
