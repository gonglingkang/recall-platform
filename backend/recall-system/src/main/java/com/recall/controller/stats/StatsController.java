package com.recall.controller.stats;

import com.recall.common.api.Result;
import com.recall.service.stats.StatsService;
import com.recall.vo.stats.TodayStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计 Controller（PRD 11.5）。
 *
 * @author recall
 */
@Tag(name = "统计", description = "今日概览、完成趋势、分类占比")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "今日概览", description = "总数/已完成/完成率/分类分布（PRD 6.7.1）")
    @GetMapping("/today")
    public Result<TodayStatsVO> today() {
        return Result.ok(statsService.today());
    }

    // TODO(P2): GET /api/stats/trend?range=7d 完成趋势
    // TODO(P2): GET /api/stats/category-share?range=30d 分类占比
}
