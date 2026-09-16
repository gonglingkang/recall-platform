package com.recall.entity.daily;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 考勤打卡记录实体（OA 每日定时抓取，一天一条）。
 * <p>
 * 打卡时间存 OA 展示文本（HH:mm:ss）；休息日无打卡为空。
 * 出勤状态存 OA 原文（如 迟到8分钟/缺勤7.50小时/正常出勤），渲染直接展示。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_attendance_records")
public class DailyAttendanceRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 考勤日期 */
    private LocalDate workDate;

    /** 日期类型: 工作日/休息日 */
    private String dateType;

    /** 上班打卡时间 HH:mm:ss（休息日为空） */
    private String clockIn;

    /** 下班打卡时间 HH:mm:ss */
    private String clockOut;

    /** 考勤结果: 正常/异常 */
    private String attendanceResult;

    /** 出勤状态原文: 正常出勤/迟到8分钟/缺勤7.50小时/育儿假… */
    private String attendanceStatus;

    /** 当天是否请假: false否 true是 */
    private Boolean onLeave;
}
