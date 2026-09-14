package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 请假记录 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "请假记录")
public class DailyLeaveVO {

    @Schema(description = "请假日期 YYYY-MM-DD")
    private LocalDate leaveDate;

    @Schema(description = "请假类型：1病假 2年假 3事假 4育儿假")
    private Integer leaveType;

    @Schema(description = "请假类型名称")
    private String leaveTypeName;

    @Schema(description = "请假时段：1全天 2上午 3下午")
    private Integer period;

    @Schema(description = "请假时段名称")
    private String periodName;

    @Schema(description = "请假事由(选填)")
    private String reason;
}
