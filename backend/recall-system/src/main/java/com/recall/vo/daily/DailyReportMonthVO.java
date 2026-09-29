package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 月度日报视图（v1.0）。
 * <p>
 * 只返回有日报的天（按日期升序），没填的天不返回，前端自行补空格。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "月度日报视图")
public class DailyReportMonthVO {

    @Schema(description = "月份 YYYY-MM")
    private String month;

    @Schema(description = "有日报的天(只含填了的,按日期升序)")
    private List<DailyReportVO> reports;

    @Schema(description = "当月请假记录(按日期升序,独立于日报存在,可能含未写日报的请假天)")
    private List<DailyLeaveVO> leaves;

    @Schema(description = "当月考勤统计(迟到次数/请假天数/加班时长)")
    private DailyAttendanceSummaryVO attendanceSummary;

    @Schema(description = "当月全量考勤(key=日期YYYY-MM-DD,含未写日报的天,打卡徽标渲染用)")
    private Map<String, DailyAttendanceVO> attendances;
}
