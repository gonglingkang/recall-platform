package com.recall.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 节假日/补班日视图。
 *
 * @author recall
 */
@Data
@Schema(description = "节假日/补班日视图")
public class HolidayDayVO {

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "类型: 1法定节假日 2补班日")
    private Integer type;

    @Schema(description = "名称(如 中秋节/国庆节前补班)")
    private String name;

    public HolidayDayVO() {
    }

    public HolidayDayVO(LocalDate date, Integer type, String name) {
        this.date = date;
        this.type = type;
        this.name = name;
    }
}
