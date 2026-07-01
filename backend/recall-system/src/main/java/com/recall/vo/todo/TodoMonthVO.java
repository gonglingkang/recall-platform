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
     */
    @Data
    @Builder
    @Schema(description = "单日待办分组")
    public static class DayGroup {

        @Schema(description = "日期 YYYY-MM-DD")
        private LocalDate date;

        @Schema(description = "当天创建的待办")
        private List<TodoVO> created;

        @Schema(description = "当天完成的历史待办")
        private List<TodoVO> completed;
    }
}
