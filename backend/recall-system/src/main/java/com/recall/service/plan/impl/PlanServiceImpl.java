package com.recall.service.plan.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.plan.PlanService;
import com.recall.service.sprint.SprintService;
import com.recall.vo.plan.MonthCompletionCountVO;
import com.recall.vo.plan.MonthTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 月度趋势 Service 实现。
 * <p>
 * 通过 {@link ObjectiveService} / {@link SprintService} 聚合每月完成率，不直接注入 Mapper。
 * 两者各按月份区间批量取数，再在内存中按月装配；查询次数与月份跨度无关，避免按月循环查库。
 * <p>
 * 口径：
 * <ul>
 *   <li>绩效率 = 有效 K（排除已取消）中已完成占比</li>
 *   <li>冲刺率 = 需我介入的冲刺中已完成占比（无需我介入的冲刺不可能完成，不进分母）</li>
 *   <li>无数据的月份为 0</li>
 * </ul>
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private static final int MAX_MONTH_SPAN = 12;

    private final ObjectiveService objectiveService;
    private final SprintService sprintService;

    @Override
    public List<MonthTrendVO> trend(String startMonth, String endMonth) {
        YearMonth start = parseMonth(startMonth);
        YearMonth end = parseMonth(endMonth);
        if (start.isAfter(end)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "起始月份不能晚于截止月份");
        }
        if (ChronoUnit.MONTHS.between(start, end) + 1 > MAX_MONTH_SPAN) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED,
                    "月范围跨度不能超过 " + MAX_MONTH_SPAN + " 个月");
        }
        Map<String, MonthCompletionCountVO> perfCounts =
                indexByMonth(objectiveService.countKeyResultsByMonthRange(startMonth, endMonth));
        Map<String, MonthCompletionCountVO> sprintCounts =
                indexByMonth(sprintService.countInvolvedByMonthRange(startMonth, endMonth));
        List<MonthTrendVO> result = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String month = ym.toString();
            result.add(MonthTrendVO.builder()
                    .month(month)
                    .perfRate(rate(perfCounts.get(month)))
                    .sprintRate(rate(sprintCounts.get(month)))
                    .build());
        }
        return result;
    }

    private Map<String, MonthCompletionCountVO> indexByMonth(List<MonthCompletionCountVO> counts) {
        return counts.stream()
                .collect(Collectors.toMap(MonthCompletionCountVO::getMonth, Function.identity()));
    }

    /** 完成率 0-1，保留两位小数；该月无数据（查询未返回行）时为 0 */
    private double rate(MonthCompletionCountVO count) {
        if (count == null || count.getTotal() == 0) {
            return 0;
        }
        return Math.round((double) count.getDone() / count.getTotal() * 100) / 100.0;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "月份格式应为 YYYY-MM");
        }
        return YearMonth.parse(month);
    }
}
