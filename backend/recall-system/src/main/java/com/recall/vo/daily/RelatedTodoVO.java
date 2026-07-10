package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 关联待办概要（日报内嵌，避免返回完整 TodoVO）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "关联待办概要(日报内嵌)")
public class RelatedTodoVO {

    @Schema(description = "待办ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "完成时间")
    private java.time.LocalDateTime doneAt;

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createdAt;
}
