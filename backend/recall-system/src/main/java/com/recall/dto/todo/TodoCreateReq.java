package com.recall.dto.todo;

import com.recall.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 待办创建请求。
 * <p>
 * 快速创建仅需 title，其余取默认值。
 *
 * @author recall
 */
@Data
@Schema(description = "待办创建请求")
public class TodoCreateReq {

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100字符")
    private String title;

    @Schema(description = "备注")
    @Size(max = 2000, message = "备注最长2000字符")
    private String note;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "优先级")
    private Priority priority = Priority.MEDIUM;
}
