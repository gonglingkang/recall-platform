package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
}
