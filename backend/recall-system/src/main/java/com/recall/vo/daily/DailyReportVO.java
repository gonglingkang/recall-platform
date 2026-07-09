package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日报 VO（v1.0）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "日报")
public class DailyReportVO {

    @Schema(description = "日报ID")
    private Long id;

    @Schema(description = "日期 YYYY-MM-DD")
    private LocalDate reportDate;

    @Schema(description = "工作内容列表")
    private List<DailyReportItemVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
