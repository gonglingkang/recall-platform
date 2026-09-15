package com.recall.dto.oa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * OA 对接配置保存请求（upsert：不存在则建，存在则覆盖）。
 * <p>
 * password 语义：首次必填；已配置后传空串/null 表示不修改。
 *
 * @author recall
 */
@Data
@Schema(description = "OA 对接配置保存请求")
public class OaConfigSaveReq {

    @Schema(description = "OA 根地址")
    @NotBlank(message = "OA 地址不能为空")
    @Size(max = 200, message = "OA 地址不能超过200字")
    private String oaBaseUrl;

    @Schema(description = "OA 登录账号")
    @NotBlank(message = "OA 账号不能为空")
    @Size(max = 100, message = "OA 账号不能超过100字")
    private String username;

    @Schema(description = "OA 登录密码；已配置后留空表示不修改")
    @Size(max = 100, message = "OA 密码不能超过100字")
    private String password;

    @Schema(description = "研发项目名（OA 项目组选择器匹配用）")
    @NotBlank(message = "研发项目名不能为空")
    @Size(max = 200, message = "研发项目名不能超过200字")
    private String devProjectName;

    @Schema(description = "请假项目名（OA 项目组选择器匹配用）")
    @NotBlank(message = "请假项目名不能为空")
    @Size(max = 200, message = "请假项目名不能超过200字")
    private String leaveProjectName;

    @Schema(description = "日报保存后自动同步: true开 false关")
    private Boolean autoSync = Boolean.FALSE;
}
