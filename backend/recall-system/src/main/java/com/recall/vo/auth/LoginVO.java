package com.recall.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录成功返回（PRD 6.2.2）。签发 JWT + 基本用户信息。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "登录成功返回")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;
}
