package com.recall.controller.plan;

import com.recall.common.api.Result;
import com.recall.service.plan.PlanService;
import com.recall.vo.plan.MonthTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 月度趋势 Controller（曲线图）。
 *
 * @author recall
 */
@Tag(name = "月度趋势", description = "月范围的绩效/冲刺完成率趋势，供曲线图展示")
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "月度趋势", description = "返回月范围内每月的绩效完成率与冲刺完成率，供曲线图用")
    @GetMapping("/trend")
    public Result<List<MonthTrendVO>> trend(
            @Parameter(description = "起始月份 YYYY-MM") @RequestParam String startMonth,
            @Parameter(description = "截止月份 YYYY-MM") @RequestParam String endMonth) {
        return Result.ok(planService.trend(startMonth, endMonth));
    }
}
