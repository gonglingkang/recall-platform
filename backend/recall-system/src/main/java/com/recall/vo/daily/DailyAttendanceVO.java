package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 考勤打卡视图（日报渲染用，OA 每日抓取）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "考勤打卡视图")
public class DailyAttendanceVO {

    @Schema(description = "日期类型: 工作日/休息日")
    private String dateType;

    @Schema(description = "上班打卡时间 HH:mm:ss；休息日为 null")
    private String clockIn;

    @Schema(description = "下班打卡时间 HH:mm:ss")
    private String clockOut;

    @Schema(description = "考勤结果: 正常/异常")
    private String attendanceResult;

    @Schema(description = "出勤状态原文: 正常出勤/迟到8分钟/缺勤7.50小时/育儿假…")
    private String attendanceStatus;

    @Schema(description = "当天是否请假")
    private Boolean onLeave;
}
