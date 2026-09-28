package com.recall.controller.holiday;

import com.recall.common.api.Result;
import com.recall.service.holiday.HolidayService;
import com.recall.vo.holiday.HolidayDayVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 法定节假日/补班 Controller。
 * <p>
 * 数据按年缓存（来源 timor 节假日 API），供个人日报的隐藏周末/节假日与日期标签渲染。
 *
 * @author recall
 */
@Tag(name = "节假日", description = "法定节假日/补班日查询（按年）")
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "查询某年节假日与补班日", description = "首次访问按年拉取外部数据缓存，之后读库")
    @GetMapping("/{year}")
    public Result<List<HolidayDayVO>> year(@Parameter(description = "年份") @PathVariable int year) {
        return Result.ok(holidayService.getYear(year));
    }
}
