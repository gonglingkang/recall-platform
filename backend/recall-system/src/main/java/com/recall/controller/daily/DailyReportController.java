package com.recall.controller.daily;

import com.recall.common.api.Result;
import com.recall.dto.daily.DailyReportSaveReq;
import com.recall.service.daily.DailyReportService;
import com.recall.vo.daily.DailyReportMonthVO;
import com.recall.vo.daily.DailyReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 日报 Controller（v1.0）。
 * <p>
 * 资源标识用日期（YYYY-MM-DD）而非 id：一天一份日报，按日期操作最直观。
 * 报告日期不可为未来（后端校验，抛 4601）。
 *
 * @author recall
 */
@Tag(name = "日报", description = "日报的查询、保存(全量覆盖)、删除、可关联待办查询")
@RestController
@RequestMapping("/api/daily-reports")
@RequiredArgsConstructor
@Validated
public class DailyReportController {

    private final DailyReportService dailyReportService;

    @Operation(summary = "月度日报列表", description = "返回某月有日报的天（只含填了的，按日期升序），每个日报内嵌工作内容项与关联待办概要")
    @GetMapping
    public Result<DailyReportMonthVO> monthList(@Parameter(description = "月份 YYYY-MM")
                                                @RequestParam @NotBlank(message = "月份不能为空")
                                                @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
                                                String month) {
        return Result.ok(dailyReportService.monthList(month));
    }

    @Operation(summary = "日报详情", description = "按日期查询当天日报详情；不存在返回 404")
    @GetMapping("/{date}")
    public Result<DailyReportVO> getByDate(@Parameter(description = "日期 YYYY-MM-DD")
                                           @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.ok(dailyReportService.getByDate(date));
    }

    @Operation(summary = "保存日报", description = "全量覆盖：不存在则建，存在则覆盖其下工作内容项与关联；日期不可为未来(4601)，关联待办须为当天相关(4602)")
    @PutMapping("/{date}")
    public Result<DailyReportVO> save(@Parameter(description = "日期 YYYY-MM-DD")
                                      @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                      @Valid @RequestBody DailyReportSaveReq req) {
        return Result.ok(dailyReportService.save(date, req));
    }

    @Operation(summary = "删除日报", description = "物理删除当天日报，连带删工作内容项与关联")
    @DeleteMapping("/{date}")
    public Result<Void> delete(@Parameter(description = "日期 YYYY-MM-DD")
                               @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        dailyReportService.delete(date);
        return Result.ok();
    }
}
