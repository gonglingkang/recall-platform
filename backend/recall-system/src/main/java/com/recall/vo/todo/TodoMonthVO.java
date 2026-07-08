package com.recall.vo.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 月度待办日历视图。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "月度待办日历")
public class TodoMonthVO {

    @Schema(description = "月份 YYYY-MM")
    private String month;

    @Schema(description = "按天分组的待办列表")
    private List<DayGroup> days;

    /**
     * 单日待办分组。
     * <p>
     * todos 含当天该看到的所有待办：当天创建的、当天完成的、以及生命周期覆盖当天仍处理中的
     * （如上月创建、下月才完成的跨月待办，在当月每一天都会出现）。
     * 同一条待办每天只出现一次；前端可据 TodoVO 的 createdAt/doneAt/status 判断当天是创建日、完成日还是处理中。
     */
    @Data
    @Builder
    @Schema(description = "单日待办分组")
    public static class DayGroup {

        @Schema(description = "日期 YYYY-MM-DD")
        private LocalDate date;

        @Schema(description = "当天可见的待办列表（含当天创建、当天完成、跨越当天处理中）")
        private List<TodoVO> todos;
    }
}
