package com.recall.vo.performance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 绩效面谈内容 VO。
 *
 * @author recall
 */
@Data
@Builder
@AllArgsConstructor
@Schema(description = "绩效面谈内容")
public class PerformanceReviewVO {

    @Schema(description = "本期工作亮点")
    private String highlights;

    @Schema(description = "本期工作不足")
    private String shortcomings;

    @Schema(description = "偏差原因分析")
    private String deviationAnalysis;

    @Schema(description = "下期工作重点或改进建议")
    private String nextFocus;
}
