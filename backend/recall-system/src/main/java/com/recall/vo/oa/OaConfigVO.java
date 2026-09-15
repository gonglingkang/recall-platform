package com.recall.vo.oa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * OA 对接配置视图。
 * <p>
 * 不回传密码明文，仅回传 hasPassword 供前端提示。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "OA 对接配置视图")
public class OaConfigVO {

    @Schema(description = "OA 根地址")
    private String oaBaseUrl;

    @Schema(description = "OA 登录账号")
    private String username;

    @Schema(description = "是否已配置密码")
    private Boolean hasPassword;

    @Schema(description = "研发项目名")
    private String devProjectName;

    @Schema(description = "请假项目名")
    private String leaveProjectName;

    @Schema(description = "日报保存后自动同步: true开 false关")
    private Boolean autoSync;
}
