package com.recall.dto.todo;

import com.recall.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 待办编辑请求。标题、分类必填，优先级为空时默认中。
 *
 * @author recall
 */
@Data
@Schema(description = "待办编辑请求")
public class TodoUpdateReq {

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100字符")
    private String title;

    @Schema(description = "备注")
    @Size(max = 2000, message = "备注最长2000字符")
    private String note;

    @Schema(description = "分类ID")
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Schema(description = "优先级，为空默认中")
    private Priority priority = Priority.MEDIUM;
}
