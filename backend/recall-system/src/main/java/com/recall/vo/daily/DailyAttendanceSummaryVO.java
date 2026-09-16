package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 月度考勤统计视图（个人日报标题框渲染）。
 * <p>
 * 迟到次数：出勤状态含「迟到」的工作日天数；请假天数：上午/下午各 0.5 天、全天 1 天；
 * 加班时长：工作日按 18:30 后计，休息日/节假日按 8:30-12:00、13:30-17:30、18:30 后
 * 三段区间与打卡时间的交集计（上班卡早于段起点按段起点算）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "月度考勤统计视图")
public class DailyAttendanceSummaryVO {

    @Schema(description = "迟到次数")
    private Integer lateCount;

    @Schema(description = "请假天数(半天计0.5)")
    private Double leaveDays;

    @Schema(description = "加班时长(小时,保留1位小数)")
    private Double overtimeHours;
}
