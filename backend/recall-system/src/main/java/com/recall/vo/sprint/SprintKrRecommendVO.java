package com.recall.vo.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 冲刺任务 AI 推荐关键成果视图（jev 模型产出，仅供参考，用户可改选）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "冲刺任务 AI 推荐关键成果")
public class SprintKrRecommendVO {

    @Schema(description = "推荐的关键成果 ID")
    private Long krId;

    @Schema(description = "关键成果名称")
    private String krName;

    @Schema(description = "所属目标名称")
    private String objectiveName;

    @Schema(description = "置信度 0~1，低值建议人工复核")
    private Double confidence;
}
