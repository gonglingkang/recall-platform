package com.recall.service.requirement.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dto.requirement.MeetingMinutesSaveReq;
import com.recall.service.requirement.MeetingMinutesPushService;
import com.recall.service.requirement.RequirementDocumentService;
import com.recall.vo.requirement.RequirementDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 会议纪要推送 Service 实现。
 * <p>
 * 推送为系统行为（无用户上下文）：归属用户与鉴权 Token 均来自配置
 * （recall.minutes.owner-user-id / recall.minutes.push-token）。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingMinutesPushServiceImpl implements MeetingMinutesPushService {

    private final RequirementDocumentService requirementDocumentService;

    @Value("${recall.minutes.push-token}")
    private String pushToken;

    @Value("${recall.minutes.owner-user-id}")
    private Long ownerUserId;

    @Override
    public RequirementDocumentVO push(String token, MeetingMinutesSaveReq req) {
        if (pushToken == null || pushToken.isBlank() || !pushToken.equals(token)) {
            log.warn("会议纪要推送 Token 校验失败");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "推送 Token 无效");
        }
        if (ownerUserId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "未配置推送归属用户（recall.minutes.owner-user-id）");
        }
        LocalDate defaultDate = LocalDate.now();
        return requirementDocumentService.upsertPushedMinute(ownerUserId, req, defaultDate);
    }
}
