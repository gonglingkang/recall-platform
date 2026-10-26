package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上线准备项新增请求。
 *
 * @author recall
 */
@Data
@Schema(description = "上线准备项新增请求")
public class RequirementPrepCreateReq {

    @Schema(description = "准备项名称")
    @NotBlank(message = "准备项名称不能为空")
    @Size(max = 100, message = "名称最长100字符")
    private String title;

    @Schema(description = "详细内容（SQL/脚本说明等，可空）")
    private String content;
}
