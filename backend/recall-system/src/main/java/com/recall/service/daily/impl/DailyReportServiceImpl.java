package com.recall.service.daily.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.daily.DailyReportMapper;
import com.recall.dto.daily.DailyReportItemReq;
import com.recall.dto.daily.DailyReportSaveReq;
import com.recall.entity.daily.DailyAttendanceRecord;
import com.recall.entity.daily.DailyLeaveRecord;
import com.recall.entity.daily.DailyReport;
import com.recall.entity.daily.DailyReportItem;
import com.recall.entity.todo.Todo;
import com.recall.enums.LeavePeriod;
import com.recall.service.daily.DailyAttendanceService;
import com.recall.service.daily.DailyLeaveService;
import com.recall.service.daily.DailyReportItemService;
import com.recall.service.daily.DailyReportItemTodoService;
import com.recall.service.daily.DailyReportService;
import com.recall.service.oa.event.DailyChangedEvent;
import com.recall.service.todo.TodoService;
import com.recall.vo.daily.DailyAttendanceSummaryVO;
import com.recall.vo.daily.DailyAttendanceVO;
import com.recall.vo.daily.DailyLeaveVO;
import com.recall.vo.daily.DailyReportItemVO;
import com.recall.vo.daily.DailyReportMonthVO;
import com.recall.vo.daily.DailyReportVO;
import com.recall.vo.daily.RelatedTodoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日报 Service 实现（v1.0）。
 * <p>
 * 持有 DailyReportMapper，负责日报主表的增删改查与日报项/关联的编排。
 * 日报项经 {@link DailyReportItemService}、关联经 {@link DailyReportItemTodoService}、
 * 待办数据与归属校验经 {@link TodoService}，本 Service 不直接注入这些 Mapper。
 * <p>
 * 核心规则：
 * <ul>
 *   <li>report_date 不可为未来（抛 4601）。</li>
 *   <li>关联待办须为当天相关：createdAt ≤ date 且（doneAt 为空 或 doneAt ≥ date），否则抛 4602。</li>
 *   <li>编辑全量覆盖：保存日报时整体替换其下工作内容项与关联。</li>
 * </ul>
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportServiceImpl implements DailyReportService {

    private final DailyReportMapper dailyReportMapper;
    private final DailyReportItemService dailyReportItemService;
    private final DailyReportItemTodoService dailyReportItemTodoService;
    private final DailyLeaveService dailyLeaveService;
    private final DailyAttendanceService dailyAttendanceService;
    private final TodoService todoService;
    private final ApplicationEventPublisher eventPublisher;

    /** 工作日加班起算时间 18:30 */
    private static final LocalTime OVERTIME_START = LocalTime.of(18, 30);
    /** 休息日/节假日加班计费时段：上午 8:30-12:00、下午 13:30-17:30、晚间 18:30-次日零点 */
    private static final LocalTime[][] OVERTIME_SEGMENTS = {
            {LocalTime.of(8, 30), LocalTime.of(12, 0)},
            {LocalTime.of(13, 30), LocalTime.of(17, 30)},
            {LocalTime.of(18, 30), LocalTime.of(23, 59, 59)}
    };

    // ===================== 查询 =====================

    @Override
    public DailyReportMonthVO monthList(String month) {
        validateMonth(month);
        Long userId = UserContextHolder.requireUserId();
        List<DailyLeaveVO> leaves = dailyLeaveService.listByMonth(month);
        DailyAttendanceSummaryVO summary = buildAttendanceSummary(userId, month, leaves);
        // 当月日报主表（按日期升序）
        List<DailyReport> reports = dailyReportMapper.selectList(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getReportDate, YearMonth.parse(month).atDay(1))
                .lt(DailyReport::getReportDate, YearMonth.parse(month).plusMonths(1).atDay(1))
                .orderByAsc(DailyReport::getReportDate));
        if (reports.isEmpty()) {
            return DailyReportMonthVO.builder()
                    .month(month)
                    .reports(Collections.emptyList())
                    .leaves(leaves)
                    .attendanceSummary(summary)
                    .build();
        }
        List<DailyReportVO> vos = buildReportVOs(reports);
        return DailyReportMonthVO.builder()
                .month(month)
                .reports(vos)
                .leaves(leaves)
                .attendanceSummary(summary)
                .build();
    }

    /**
     * 计算当月考勤统计：迟到次数 / 请假天数(半天0.5) / 加班时长。
     * <p>
     * 加班规则：工作日按 18:30 后到下班卡计；休息日/节假日按
     * 8:30-12:00、13:30-17:30、18:30-下班 三段与打卡时间的交集计，
     * 上班卡早于段起点按段起点算（用户规则）。
     */
    private DailyAttendanceSummaryVO buildAttendanceSummary(Long userId, String month, List<DailyLeaveVO> leaves) {
        List<DailyAttendanceRecord> records = dailyAttendanceService.listByMonth(userId, month);
        int lateCount = (int) records.stream()
                .filter(r -> r.getAttendanceStatus() != null && r.getAttendanceStatus().contains("迟到"))
                .count();
        double leaveDays = leaves.stream().mapToDouble(l -> {
            LeavePeriod period = LeavePeriod.of(l.getPeriod());
            return period == LeavePeriod.FULL_DAY ? 1.0 : period == null ? 0.0 : 0.5;
        }).sum();
        long overtimeMinutes = records.stream().mapToLong(this::overtimeMinutesOf).sum();
        double overtimeHours = Math.round(overtimeMinutes / 6.0) / 10.0;
        return DailyAttendanceSummaryVO.builder()
                .lateCount(lateCount)
                .leaveDays(leaveDays)
                .overtimeHours(overtimeHours)
                .build();
    }

    /** 单日加班分钟数；无下班卡无法计时返回 0 */
    private long overtimeMinutesOf(DailyAttendanceRecord record) {
        LocalTime out = parseClock(record.getClockOut());
        if (out == null) {
            return 0;
        }
        LocalTime in = parseClock(record.getClockIn());
        boolean workday = record.getDateType() == null || "工作日".equals(record.getDateType());
        if (workday) {
            return Math.max(0, Duration.between(OVERTIME_START, out).toMinutes());
        }
        // 休息日/节假日：三段标准区间与打卡区间的交集；缺卡用段边界补
        long minutes = 0;
        for (LocalTime[] seg : OVERTIME_SEGMENTS) {
            LocalTime start = in == null ? seg[0] : maxTime(in, seg[0]);
            LocalTime end = minTime(out, seg[1]);
            if (end.isAfter(start)) {
                minutes += Duration.between(start, end).toMinutes();
            }
        }
        return minutes;
    }

    private static LocalTime parseClock(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(s.trim().length() == 5 ? s.trim() + ":00" : s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalTime maxTime(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime minTime(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    @Override
    public DailyReportVO getByDate(LocalDate date) {
        DailyReport report = loadOwnedByDate(date);
        return buildReportVOs(List.of(report)).get(0);
    }

    @Override
    public List<DailyReport> listByDateRange(Long userId, LocalDate start, LocalDate end) {
        return dailyReportMapper.selectList(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getReportDate, start)
                .le(DailyReport::getReportDate, end)
                .orderByAsc(DailyReport::getReportDate));
    }

    // ===================== 写操作 =====================

    /**
     * 保存日报（全量覆盖）。
     * <p>
     * 校验未来日期 + 关联待办当天相关；upsert 主表后全量覆盖日报项与关联。
     * 多条写 + 跨 Service 调用 -> 事务。
     *
     * @param date 日期
     * @param req  保存请求
     * @return 保存后的日报详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyReportVO save(LocalDate date, DailyReportSaveReq req) {
        // 1. 未来日期校验
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException(ResultCode.DAILY_REPORT_FUTURE_DATE);
        }
        Long userId = UserContextHolder.requireUserId();
        // 2. 收集全部关联待办 id（去重），校验归属 + 当天相关
        validateRelatedTodos(date, req.getItems());

        // 3. upsert 日报主表（同用户同日期唯一）
        DailyReport report = dailyReportMapper.selectOne(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId)
                .eq(DailyReport::getReportDate, date));
        if (report == null) {
            report = new DailyReport();
            report.setUserId(userId);
            report.setReportDate(date);
            dailyReportMapper.insert(report);
            log.info("创建日报: userId={}, date={}", userId, date);
        }
        // 4. 全量覆盖日报项与关联（多条写，并入本事务）
        dailyReportItemService.replaceByReportId(report.getId(), req.getItems());
        // 5. 发布日报变更事件（OA 自动同步监听，AFTER_COMMIT 消费）
        eventPublisher.publishEvent(new DailyChangedEvent(userId, date));
        // 重新查主表拿 updatedAt
        DailyReport saved = dailyReportMapper.selectById(report.getId());
        return buildReportVOs(List.of(saved)).get(0);
    }

    /**
     * 删除指定日期的日报（物理删除，连带删日报项与关联）。
     * <p>多条写 -> 事务。
     *
     * @param date 日期
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(LocalDate date) {
        DailyReport report = loadOwnedByDate(date);
        dailyReportItemService.deleteByReportId(report.getId());
        dailyReportMapper.deleteById(report.getId());
        eventPublisher.publishEvent(new DailyChangedEvent(report.getUserId(), date));
        log.info("删除日报: userId={}, date={}", UserContextHolder.requireUserId(), date);
    }

    // ===================== 辅助 =====================

    /**
     * 校验所有日报项的关联待办：归属（不存在/越权统一 404）+ 当天相关（4602）。
     *
     * @param date  日报日期
     * @param items 日报项请求列表
     */
    private void validateRelatedTodos(LocalDate date, List<DailyReportItemReq> items) {
        List<Long> todoIds = items.stream()
                .filter(i -> i.getTodoIds() != null && !i.getTodoIds().isEmpty())
                .flatMap(i -> i.getTodoIds().stream())
                .distinct()
                .toList();
        if (todoIds.isEmpty()) {
            return;
        }
        // 归属校验：任一不存在或不属于当前用户抛 404
        List<Todo> todos = todoService.listByIds(todoIds, true);
        // 当天相关校验：createdAt ≤ date 且（doneAt 为空 或 doneAt ≥ date）
        for (Todo todo : todos) {
            if (!isRelatedOnDate(todo, date)) {
                throw new BusinessException(ResultCode.DAILY_TODO_NOT_RELATED);
            }
        }
    }

    /**
     * 判定待办在 date 当天是否相关（生命周期区间覆盖 date）。
     * <p>区间 = [createdAt日, doneAt 为空 ? 今天 : doneAt日]。
     *
     * @param todo 待办实体
     * @param date 日报日期
     * @return true 表示当天相关
     */
    private boolean isRelatedOnDate(Todo todo, LocalDate date) {
        if (todo.getCreatedAt() == null) {
            return false;
        }
        if (todo.getCreatedAt().toLocalDate().isAfter(date)) {
            // 当天尚未创建
            return false;
        }
        if (todo.getDoneAt() != null && todo.getDoneAt().toLocalDate().isBefore(date)) {
            // 当天之前已完成
            return false;
        }
        return true;
    }

    /**
     * 按日期加载当前用户的日报，不存在或越权抛 404。
     */
    private DailyReport loadOwnedByDate(LocalDate date) {
        DailyReport report = dailyReportMapper.selectOne(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, UserContextHolder.requireUserId())
                .eq(DailyReport::getReportDate, date));
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日报不存在或无权访问");
        }
        return report;
    }

    /**
     * 批量构建日报 VO（避免 N+1：一次查全部日报项 -> 一次查全部关联 -> 一次查全部待办）。
     *
     * @param reports 日报主表实体列表
     * @return 日报 VO 列表（顺序与入参一致）
     */
    private List<DailyReportVO> buildReportVOs(List<DailyReport> reports) {
        List<Long> reportIds = reports.stream().map(DailyReport::getId).toList();
        // 1. 批量查日报项，按 reportId 分组
        Map<Long, List<DailyReportItem>> itemsByReport = dailyReportItemService.listByReportIds(reportIds);
        // 2. 批量查关联，按 itemId 分组
        List<Long> itemIds = itemsByReport.values().stream()
                .flatMap(List::stream)
                .map(DailyReportItem::getId)
                .toList();
        Map<Long, List<Long>> todoIdsByItem = dailyReportItemTodoService.listTodoIdsByItemIds(itemIds);
        // 3. 一次性查全部关联待办实体（按 id 去重，不校验归属——日报已属当前用户，其下关联必为自己的待办）
        List<Long> allTodoIds = todoIdsByItem.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        Map<Long, Todo> todoMap = allTodoIds.isEmpty()
                ? Collections.emptyMap()
                : todoService.listByIds(allTodoIds, false).stream()
                .collect(Collectors.toMap(Todo::getId, t -> t));
        // 4. 一次查全部请假记录，按日期分组
        List<LocalDate> reportDates = reports.stream().map(DailyReport::getReportDate).toList();
        Map<LocalDate, DailyLeaveRecord> leaveByDate =
                dailyLeaveService.mapByDates(reports.get(0).getUserId(), reportDates);
        // 5. 一次查全部考勤打卡记录，按日期分组（OA 每日抓取，可能无记录）
        Map<LocalDate, DailyAttendanceRecord> attendanceByDate =
                dailyAttendanceService.mapByDates(reports.get(0).getUserId(), reportDates);

        List<DailyReportVO> result = new ArrayList<>(reports.size());
        for (DailyReport report : reports) {
            List<DailyReportItem> items = itemsByReport.getOrDefault(report.getId(), Collections.emptyList());
            List<DailyReportItemVO> itemVOs = new ArrayList<>(items.size());
            for (DailyReportItem item : items) {
                List<Long> todoIds = todoIdsByItem.getOrDefault(item.getId(), Collections.emptyList());
                List<RelatedTodoVO> todoVOs = todoIds.stream()
                        .map(todoMap::get)
                        .filter(java.util.Objects::nonNull)
                        .map(this::toRelatedTodoVO)
                        .toList();
                itemVOs.add(DailyReportItemVO.builder()
                        .id(item.getId())
                        .content(item.getContent())
                        .progress(item.getProgress() == null ? 0 : item.getProgress())
                        .todos(todoVOs)
                        .build());
            }
            DailyLeaveRecord leave = leaveByDate.get(report.getReportDate());
            DailyLeaveVO leaveVO = leave == null ? null : dailyLeaveService.toVO(leave);
            DailyAttendanceRecord attendance = attendanceByDate.get(report.getReportDate());
            result.add(DailyReportVO.builder()
                    .id(report.getId())
                    .reportDate(report.getReportDate())
                    .items(itemVOs)
                    .leave(leaveVO)
                    .attendance(toAttendanceVO(attendance))
                    .createdAt(report.getCreatedAt())
                    .updatedAt(report.getUpdatedAt())
                    .build());
        }
        return result;
    }

    /**
     * 考勤记录实体转 VO；无记录返回 null。
     */
    private DailyAttendanceVO toAttendanceVO(DailyAttendanceRecord record) {
        if (record == null) {
            return null;
        }
        return DailyAttendanceVO.builder()
                .dateType(record.getDateType())
                .clockIn(record.getClockIn())
                .clockOut(record.getClockOut())
                .attendanceResult(record.getAttendanceResult())
                .attendanceStatus(record.getAttendanceStatus())
                .onLeave(record.getOnLeave())
                .build();
    }

    private RelatedTodoVO toRelatedTodoVO(Todo todo) {
        return RelatedTodoVO.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .status(todo.getStatus())
                .doneAt(todo.getDoneAt())
                .createdAt(todo.getCreatedAt())
                .build();
    }

    private void validateMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "月份格式应为 YYYY-MM");
        }
    }
}
