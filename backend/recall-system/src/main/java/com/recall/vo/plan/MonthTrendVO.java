package com.recall.vo.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 月度趋势 VO（供曲线图用）。
 * <p>
 * 表示某一个月的绩效完成率与冲刺完成率，按月范围聚合返回多条。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "月度趋势(绩效/冲刺完成率)")
public class MonthTrendVO {

    @Schema(description = "月份 YYYY-MM")
    private String month;

    @Schema(description = "绩效完成率 0-1(有效K中已完成占比)")
    private double perfRate;

    @Schema(description = "冲刺完成率 0-1(冲刺中已完成占比)")
    private double sprintRate;
}
