package com.recall.service.requirement;

/**
 * 会议纪要生成需求标题 Service（LLM 网关）。
 * <p>
 * 纪要标题形如「20261010 XX需求补充传递会议纪要」，带日期与会议性后缀，
 * 直接当需求标题需手工删改；本服务将其转写为简洁规范的需求标题。
 *
 * @author recall
 */
public interface MeetingMinutesTitleService {

    /**
     * 把纪要标题转写为需求标题（去日期/去会议后缀，保留核心需求点）。
     *
     * @param minutesTitle 纪要标题
     * @return 生成的需求标题
     */
    String generateRequirementTitle(String minutesTitle);
}
