package com.recall.dto.performance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 绩效面谈生成请求。
 *
 * @author recall
 */
@Data
@Schema(description = "绩效面谈生成请求")
public class PerformanceReviewGenerateReq {

    @Schema(description = "月份 YYYY-MM")
    @NotBlank(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String month;
}
