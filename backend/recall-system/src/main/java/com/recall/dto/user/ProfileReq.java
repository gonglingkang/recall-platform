package com.recall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改请求（PRD 6.9.1 / 11.6）。
 *
 * @author recall
 */
@Data
@Schema(description = "个人资料修改请求")
public class ProfileReq {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不合法")
    @Size(max = 128, message = "邮箱过长")
    private String email;
}
