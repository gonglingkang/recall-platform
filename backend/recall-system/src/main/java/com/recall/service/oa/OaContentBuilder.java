package com.recall.service.oa;

import com.recall.entity.daily.DailyLeaveRecord;
import com.recall.entity.daily.DailyReport;
import com.recall.entity.daily.DailyReportItem;
import com.recall.entity.oa.OaUserConfig;
import com.recall.enums.LeavePeriod;
import com.recall.service.daily.DailyLeaveService;
import com.recall.service.daily.DailyReportItemService;
import com.recall.service.daily.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OA 工时日填报内容构建器。
 * <p>
 * 职责：
 * <ul>
 *   <li>工作内容文本：复刻前端「复制日报」格式（{@code 1、内容，进度xx%}，按条目序号拼接），
 *       前端基准见 DailyReportView.vue 的 copyStructuredDayReport。</li>
 *   <li>整周填写计划：按平台周（周一~周五工作日）逐日产出行计划，
 *       请假行/工作行的拆分与占比规则：</li>
 * </ul>
 * <p>行规则（与 OA 填报要求一致）：
 * <ul>
 *   <li>无请假：1 行研发项目，占比 1；无日报则该天跳过。</li>
 *   <li>全天请假：1 行请假项目，占比 1。</li>
 *   <li>上午请假：请假行 0.47 + 工作行 0.53；下午请假：请假行 0.53 + 工作行 0.47。</li>
 *   <li>半天请假但当天无日报：只填请假行（合计不足 1，用户补写日报后重新同步覆盖）。</li>
 * </ul>
 *
 * @author recall
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OaContentBuilder {

    /** 上午请假行占比 */
    public static final double AM_LEAVE_RATIO = 0.47;
    /** 上午请假时工作行占比 */
    public static final double AM_WORK_RATIO = 0.53;
    /** 下午请假行占比 */
    public static final double PM_LEAVE_RATIO = 0.53;
    /** 下午请假时工作行占比 */
    public static final double PM_WORK_RATIO = 0.47;

    /** OA 明细表只列工作日（周一~周五），周六为 OA 周起点不生成行 */
    private static final int WORKDAY_COUNT = 5;

    private final DailyReportService dailyReportService;
    private final DailyReportItemService dailyReportItemService;
    private final DailyLeaveService dailyLeaveService;

    /**
     * 整周填写计划。
     *
     * @param weekStart 平台周起始（周一）
     * @param days      有内容的天（无日报且无请假的天不出现）
     */
    public record WeekPlan(LocalDate weekStart, List<DayFillPlan> days) {
    }

    /**
     * 单日填写计划。
     *
     * @param date 填报日期
     * @param rows 该天的行（半天请假为 2 行）
     */
    public record DayFillPlan(LocalDate date, List<FillRow> rows) {
    }

    /**
     * 单行填写计划。
     *
     * @param projectName 项目组名称（OA 选择器匹配用）
     * @param leave       是否请假行
     * @param content     工作内容（请假行为空）
     * @param ratio       日工时占比
     */
    public record FillRow(String projectName, boolean leave, String content, double ratio) {
    }

    /**
     * 构建填写计划。
     *
     * @param userId    用户
     * @param weekStart 平台周起始（周一）
     * @param config    OA 配置（取研发/请假项目名）
     * @param onlyDate  仅构建该天的计划（自动同步场景，null=整周）
     * @return 填写计划（仅含有内容的天）
     */
    public WeekPlan buildWeekPlan(Long userId, LocalDate weekStart, OaUserConfig config, LocalDate onlyDate) {
        LocalDate end = weekStart.plusDays(WORKDAY_COUNT - 1L);
        List<DailyReport> reports = dailyReportService.listByDateRange(userId, weekStart, end);
        Map<LocalDate, DailyReport> reportByDate = reports.stream()
                .collect(Collectors.toMap(DailyReport::getReportDate, Function.identity()));
        Map<Long, List<DailyReportItem>> itemsByReportId = reports.isEmpty()
                ? Map.of()
                : dailyReportItemService.listByReportIds(reports.stream().map(DailyReport::getId).toList());
        Map<LocalDate, DailyLeaveRecord> leaveByDate =
                dailyLeaveService.mapByDates(userId, weekStart.datesUntil(end.plusDays(1)).toList());

        List<DayFillPlan> days = new ArrayList<>();
        for (int i = 0; i < WORKDAY_COUNT; i++) {
            LocalDate date = weekStart.plusDays(i);
            if (onlyDate != null && !date.equals(onlyDate)) {
                continue;
            }
            DailyReport report = reportByDate.get(date);
            String content = report == null ? "" : buildWorkContent(itemsByReportId.getOrDefault(report.getId(), List.of()));
            DailyLeaveRecord leave = leaveByDate.get(date);
            List<FillRow> rows = buildDayRows(date, leave, content, config);
            if (!rows.isEmpty()) {
                days.add(new DayFillPlan(date, rows));
            }
        }
        return new WeekPlan(weekStart, days);
    }

    /**
     * 生成单日的行计划。
     *
     * @param date    填报日期
     * @param leave   当天请假记录（可为 null）
     * @param content 当天日报内容文本（可为空串）
     * @param config  OA 配置
     * @return 行列表；该天既无日报也无请假返回空列表
     */
    public List<FillRow> buildDayRows(LocalDate date, DailyLeaveRecord leave, String content, OaUserConfig config) {
        List<FillRow> rows = new ArrayList<>();
        if (leave == null) {
            if (content.isBlank()) {
                return rows;
            }
            rows.add(new FillRow(config.getDevProjectName(), false, content, 1.0));
            return rows;
        }
        LeavePeriod period = LeavePeriod.of(leave.getPeriod());
        if (period == null) {
            log.warn("请假时段不合法，跳过 OA 行生成: userId={}, date={}, period={}", leave.getUserId(), date, leave.getPeriod());
            return rows;
        }
        switch (period) {
            case FULL_DAY -> rows.add(new FillRow(config.getLeaveProjectName(), true, "", 1.0));
            case AM -> {
                rows.add(new FillRow(config.getLeaveProjectName(), true, "", AM_LEAVE_RATIO));
                if (!content.isBlank()) {
                    rows.add(new FillRow(config.getDevProjectName(), false, content, AM_WORK_RATIO));
                }
            }
            case PM -> {
                rows.add(new FillRow(config.getLeaveProjectName(), true, "", PM_LEAVE_RATIO));
                if (!content.isBlank()) {
                    rows.add(new FillRow(config.getDevProjectName(), false, content, PM_WORK_RATIO));
                }
            }
        }
        return rows;
    }

    /**
     * 生成工作内容文本（与前端复制日报格式逐字一致）：
     * {@code 1、工作内容，进度100%\n2、...}，末尾去换行。
     *
     * @param items 当天日报项（按展示顺序）
     * @return 文本；无条目返回空串
     */
    public String buildWorkContent(List<DailyReportItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            DailyReportItem item = items.get(i);
            sb.append(i + 1).append("、").append(item.getContent()).append("，进度").append(item.getProgress()).append("%\n");
        }
        return sb.toString().trim();
    }
}
