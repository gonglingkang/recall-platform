package com.recall.dto.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 团队冲刺任务编辑请求。
 * <p>
 * title 必填；note 可选，为 null 表示不改。状态、需我介入、关联关键成果均通过专门接口维护。
 *
 * @author recall
 */
@Data
@Schema(description = "团队冲刺任务编辑请求")
public class SprintUpdateReq {

    @Schema(description = "标题")
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "标题最长100字符")
    private String title;

    @Schema(description = "备注/说明")
    @Size(max = 2000, message = "备注最长2000字符")
    private String note;
}
