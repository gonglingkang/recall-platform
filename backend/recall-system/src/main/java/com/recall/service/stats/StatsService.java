package com.recall.service.stats;

import com.recall.vo.stats.TodayStatsVO;

/**
 * 统计 Service（PRD 6.7 / 11.5）。
 *
 * @author recall
 */
public interface StatsService {

    /**
     * 今日概览（PRD 6.7.1）。
     *
     * @return 今日统计数据
     */
    TodayStatsVO today();

    // TODO(P2): 完成趋势 trend、分类占比 categoryShare
}
