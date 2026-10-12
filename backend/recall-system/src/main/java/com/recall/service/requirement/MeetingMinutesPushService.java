package com.recall.service.requirement;

import com.recall.dto.requirement.MeetingMinutesSaveReq;
import com.recall.vo.requirement.RequirementDocumentVO;

/**
 * 会议纪要推送 Service（另一系统在源头拿到纪要后推送到本平台）。
 * <p>
 * 鉴权 = 配置的 push-token（请求头 X-Push-Token）；入库统一挂配置的归属用户名下，
 * 按 URL 幂等（重复推送只更新标题/日期，不动绑定关系）。
 *
 * @author recall
 */
public interface MeetingMinutesPushService {

    /**
     * 接收一条会议纪要推送并入库（入池，未绑定需求）。
     *
     * @param token 请求头携带的推送 Token
     * @param req   标题/链接/会议日期
     * @return 落库后的纪要
     */
    RequirementDocumentVO push(String token, MeetingMinutesSaveReq req);
}
