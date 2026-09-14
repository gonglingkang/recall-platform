package com.recall.entity.daily;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recall.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日报请假记录实体。
 * <p>
 * 个人日报的请假纯记录（无审批流，审批以 OA 为准），一天一条，支持全天/半天。
 * 请假类型/时段存数字码，含义见 {@link com.recall.enums.LeaveType}、{@link com.recall.enums.LeavePeriod}。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_leave_records")
public class DailyLeaveRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 请假日期（自然日，不可为未来） */
    private LocalDate leaveDate;

    /** 请假类型：1病假 2年假 3事假 4育儿假 */
    private Integer leaveType;

    /** 请假时段：1全天 2上午 3下午 */
    private Integer period;

    /** 请假事由（选填，仅作查看记录） */
    private String reason;
}
