package com.recall.service.plan.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.enums.KeyResultStatus;
import com.recall.enums.SprintStatus;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.plan.PlanService;
import com.recall.service.sprint.SprintService;
import com.recall.vo.objectives.KeyResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import com.recall.vo.plan.MonthTrendVO;
import com.recall.vo.sprint.SprintItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度趋势 Service 实现。
 * <p>
 * 通过 {@link ObjectiveService} / {@link SprintService} 聚合每月完成率，不直接注入 Mapper。
 * 绩效率 = 有效 K（排除已取消）中已完成占比；冲刺率 = 冲刺中已完成占比；无数据时为 0。
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
        List<MonthTrendVO> result = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String month = ym.toString();
            result.add(MonthTrendVO.builder()
                    .month(month)
                    .perfRate(round(perfRate(month)))
                    .sprintRate(round(sprintRate(month)))
                    .build());
        }
        return result;
    }

    /** 绩效完成率：有效 K（排除已取消）中已完成占比，无有效 K 时 0 */
    private double perfRate(String month) {
        List<ObjectiveVO> objectives = objectiveService.list(month);
        int total = 0;
        int done = 0;
        for (ObjectiveVO o : objectives) {
            for (KeyResultVO kr : o.getKeyResults()) {
                if (KeyResultStatus.CANCELLED.getValue().equals(kr.getStatus())) {
                    continue;
                }
                total++;
                if (KeyResultStatus.DONE.getValue().equals(kr.getStatus())) {
                    done++;
                }
            }
        }
        return total == 0 ? 0 : (double) done / total;
    }

    /** 冲刺完成率：已完成冲刺占比，无冲刺时 0 */
    private double sprintRate(String month) {
        List<SprintItemVO> sprints = sprintService.list(month, null);
        if (sprints.isEmpty()) {
            return 0;
        }
        long done = sprints.stream()
                .filter(s -> SprintStatus.DONE.getValue().equals(s.getStatus()))
                .count();
        return (double) done / sprints.size();
    }

    private YearMonth parseMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "月份格式应为 YYYY-MM");
        }
        return YearMonth.parse(month);
    }

    private double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
