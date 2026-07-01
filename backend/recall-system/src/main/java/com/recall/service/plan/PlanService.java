package com.recall.service.plan;

import com.recall.vo.plan.MonthTrendVO;

import java.util.List;

/**
 * 月度趋势 Service。
 * <p>
 * 提供指定月范围的绩效完成率与冲刺完成率，供曲线图展示。
 *
 * @author recall
 */
public interface PlanService {

    /**
     * 查询月范围内的每月绩效/冲刺完成率趋势。
     * <p>枚举 startMonth→endMonth 每个月，返回每月的完成率（0-1，保留2位）。
     * 绩效率 = 有效 K（排除已取消）中已完成占比；冲刺率 = 冲刺中已完成占比；无数据时为 0。
     *
     * @param startMonth 起始月份 YYYY-MM（含）
     * @param endMonth   截止月份 YYYY-MM（含）
     * @return 月度趋势列表，按月份升序
     */
    List<MonthTrendVO> trend(String startMonth, String endMonth);
}
