package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 目标 O 创建请求（月度绩效 v2.0）。
 * <p>
 * 名称在同用户同月内唯一（Service 层校验）。
 *
 * @author recall
 */
@Data
@Schema(description = "目标创建请求")
public class ObjectiveCreateReq {

    @Schema(description = "月份 YYYY-MM", example = "2026-07")
    @NotBlank(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String month;

    @Schema(description = "目标名称")
    @NotBlank(message = "目标名称不能为空")
    @Size(max = 100, message = "目标名称最长100字符")
    private String name;

    @Schema(description = "目标描述")
    @Size(max = 2000, message = "目标描述最长2000字符")
    private String description;
}
