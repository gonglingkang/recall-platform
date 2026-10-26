package com.recall.vo.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 需求上线准备项 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "需求上线准备项")
public class RequirementPrepVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "归属需求ID")
    private Long requirementId;

    @Schema(description = "准备项名称")
    private String title;

    @Schema(description = "详细内容（SQL/脚本说明等，可空）")
    private String content;

    @Schema(description = "是否完成")
    private Boolean done;

    @Schema(description = "排序")
    private Integer sortOrder;
}
