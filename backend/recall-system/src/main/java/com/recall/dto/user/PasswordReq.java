package com.recall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码请求（PRD 6.9.2 / 11.6）。
 *
 * @author recall
 */
@Data
@Schema(description = "修改密码请求")
public class PasswordReq {

    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*]{8,32}$",
            message = "新密码为8~32位且至少包含字母和数字")
    private String newPassword;
}
