package com.recall.vo.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 需求文档 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "需求文档")
public class RequirementDocumentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "归属需求ID")
    private Long requirementId;

    @Schema(description = "文档类型 1原型设计/2需求文档/3会议纪要")
    private String type;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "外部链接")
    private String url;

    @Schema(description = "文档时间")
    private LocalDate documentDate;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
