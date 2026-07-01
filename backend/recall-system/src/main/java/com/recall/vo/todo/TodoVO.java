package com.recall.vo.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办出参 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "待办出参对象")
public class TodoVO {

    @Schema(description = "待办ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "完成时间")
    private LocalDateTime doneAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
