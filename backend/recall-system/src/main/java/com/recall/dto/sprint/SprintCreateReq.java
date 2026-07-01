package com.recall.dto.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 团队冲刺任务创建请求。
 * <p>
 * 创建只需 month + title（+ 可选 note）；需我介入、关联关键成果创建后通过专门接口维护。
 *
 * @author recall
 */
@Data
@Schema(description = "团队冲刺任务创建请求")
public class SprintCreateReq {

    @Schema(description = "月份 YYYY-MM", example = "2026-07")
    @NotBlank(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String month;

    @Schema(description = "标题")
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "标题最长100字符")
    private String title;

    @Schema(description = "备注/说明")
    @Size(max = 2000, message = "备注最长2000字符")
    private String note;
}
