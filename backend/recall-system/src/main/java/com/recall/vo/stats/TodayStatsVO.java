package com.recall.vo.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 今日概览 VO（PRD 6.7.1 / 11.5 GET /api/stats/today）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "今日概览")
public class TodayStatsVO {

    @Schema(description = "总数")
    private int total;

    @Schema(description = "已完成")
    private int done;

    @Schema(description = "完成率")
    private double rate;

    @Schema(description = "分类分布")
    private List<CategoryCount> categoryCounts;

    @Data
    @Builder
    @Schema(description = "分类统计")
    public static class CategoryCount {

        @Schema(description = "分类ID")
        private Long categoryId;

        @Schema(description = "分类名")
        private String categoryName;

        @Schema(description = "数量")
        private int count;
    }
}
