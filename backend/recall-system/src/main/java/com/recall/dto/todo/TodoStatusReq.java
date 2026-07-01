package com.recall.dto.todo;

import com.recall.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 待办状态变更请求（PATCH /api/todos/:id/status）。
 * <p>
 * 完成/撤销。完成时间由后端按当前时间自动设置，无需前端传入。
 *
 * @author recall
 */
@Data
@Schema(description = "待办状态变更请求")
public class TodoStatusReq {

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private TodoStatus status;
}
