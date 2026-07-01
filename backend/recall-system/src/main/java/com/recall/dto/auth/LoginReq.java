package com.recall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（PRD 6.2.2）。账号或邮箱 + 密码。
 *
 * @author recall
 */
@Data
@Schema(description = "登录请求")
public class LoginReq {

    @Schema(description = "账号或邮箱")
    @NotBlank(message = "账号或邮箱不能为空")
    private String account;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "记住我")
    private Boolean rememberMe = false;
}
