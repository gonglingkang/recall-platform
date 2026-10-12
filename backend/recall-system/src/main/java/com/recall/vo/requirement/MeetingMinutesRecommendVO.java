package com.recall.vo.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 会议纪要 AI 推荐需求 VO（jev 模型）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "会议纪要 AI 推荐需求")
public class MeetingMinutesRecommendVO {

    @Schema(description = "推荐需求 ID")
    private Long requirementId;

    @Schema(description = "推荐需求标题")
    private String requirementTitle;

    @Schema(description = "需求分类名（主分类/子分类，无则为 null）")
    private String categoryName;

    @Schema(description = "需求状态码")
    private String status;

    @Schema(description = "推荐置信度 0~1")
    private Double confidence;
}
