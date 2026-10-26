package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上线准备项更新请求（字段为 null 表示不变；content 传空串表示清空）。
 *
 * @author recall
 */
@Data
@Schema(description = "上线准备项更新请求")
public class RequirementPrepUpdateReq {

    @Schema(description = "准备项名称（null=不变）")
    @Size(max = 100, message = "名称最长100字符")
    private String title;

    @Schema(description = "详细内容（null=不变，空串=清空）")
    private String content;

    @Schema(description = "是否完成（null=不变）")
    private Boolean done;
}
