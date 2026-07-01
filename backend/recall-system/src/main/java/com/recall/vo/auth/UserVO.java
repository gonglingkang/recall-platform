package com.recall.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 当前用户信息（PRD 11.1 GET /api/auth/me）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "当前用户信息")
public class UserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "时区")
    private String timezone;

    @Schema(description = "提醒开关")
    private Boolean reminderEnabled;
}
