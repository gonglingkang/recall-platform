package com.recall.service.daily.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.daily.DailyLeaveRecordMapper;
import com.recall.dto.daily.DailyLeaveSaveReq;
import com.recall.entity.daily.DailyLeaveRecord;
import com.recall.enums.LeavePeriod;
import com.recall.enums.LeaveType;
import com.recall.service.daily.DailyLeaveService;
import com.recall.vo.daily.DailyLeaveVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日报请假记录 Service 实现。
 * <p>
 * 持有 DailyLeaveRecordMapper（唯一归属），负责请假记录的 upsert/删除/查询。
 * 请假为纯记录：无审批流，审批以 OA 为准。
 * <p>
 * 核心规则：
 * <ul>
 *   <li>leave_date 不可为未来（复用日报 4601）。</li>
 *   <li>请假类型/时段数字码不合法抛 4603。</li>
 *   <li>保存为 upsert：同用户同日期不存在则建，存在则覆盖。</li>
 * </ul>
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyLeaveServiceImpl implements DailyLeaveService {

    private final DailyLeaveRecordMapper dailyLeaveRecordMapper;

    @Override
    public DailyLeaveVO save(LocalDate date, DailyLeaveSaveReq req) {
        // 1. 未来日期校验（请假不可预记未来）
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException(ResultCode.DAILY_REPORT_FUTURE_DATE);
        }
        // 2. 类型/时段数字码校验
        LeaveType type = LeaveType.of(req.getLeaveType());
        LeavePeriod period = LeavePeriod.of(req.getPeriod());
        if (type == null || period == null) {
            throw new BusinessException(ResultCode.DAILY_LEAVE_PARAM_INVALID);
        }
        Long userId = UserContextHolder.requireUserId();
        // 3. upsert（同用户同日期唯一）
        DailyLeaveRecord record = dailyLeaveRecordMapper.selectOne(new LambdaQueryWrapper<DailyLeaveRecord>()
                .eq(DailyLeaveRecord::getUserId, userId)
                .eq(DailyLeaveRecord::getLeaveDate, date));
        if (record == null) {
            record = new DailyLeaveRecord();
            record.setUserId(userId);
            record.setLeaveDate(date);
        }
        record.setLeaveType(type.getCode());
        record.setPeriod(period.getCode());
        record.setReason(normalizeReason(req.getReason()));
        if (record.getId() == null) {
            dailyLeaveRecordMapper.insert(record);
            log.info("创建请假记录: userId={}, date={}, type={}, period={}", userId, date, type, period);
        } else {
            dailyLeaveRecordMapper.updateById(record);
            log.info("更新请假记录: userId={}, date={}, type={}, period={}", userId, date, type, period);
        }
        return toVO(record);
    }

    @Override
    public void delete(LocalDate date) {
        Long userId = UserContextHolder.requireUserId();
        DailyLeaveRecord record = loadOwnedByDate(userId, date);
        dailyLeaveRecordMapper.deleteById(record.getId());
        log.info("删除请假记录: userId={}, date={}", userId, date);
    }

    @Override
    public List<DailyLeaveVO> listByMonth(String month) {
        YearMonth ym = YearMonth.parse(month);
        Long userId = UserContextHolder.requireUserId();
        List<DailyLeaveRecord> records = dailyLeaveRecordMapper.selectList(new LambdaQueryWrapper<DailyLeaveRecord>()
                .eq(DailyLeaveRecord::getUserId, userId)
                .ge(DailyLeaveRecord::getLeaveDate, ym.atDay(1))
                .lt(DailyLeaveRecord::getLeaveDate, ym.plusMonths(1).atDay(1))
                .orderByAsc(DailyLeaveRecord::getLeaveDate));
        return records.stream().map(this::toVO).toList();
    }

    @Override
    public DailyLeaveRecord getByDate(LocalDate date) {
        return dailyLeaveRecordMapper.selectOne(new LambdaQueryWrapper<DailyLeaveRecord>()
                .eq(DailyLeaveRecord::getUserId, UserContextHolder.requireUserId())
                .eq(DailyLeaveRecord::getLeaveDate, date));
    }

    @Override
    public Map<LocalDate, DailyLeaveRecord> mapByDates(Collection<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DailyLeaveRecord> records = dailyLeaveRecordMapper.selectList(new LambdaQueryWrapper<DailyLeaveRecord>()
                .eq(DailyLeaveRecord::getUserId, UserContextHolder.requireUserId())
                .in(DailyLeaveRecord::getLeaveDate, dates));
        return records.stream().collect(Collectors.toMap(DailyLeaveRecord::getLeaveDate, r -> r));
    }

    @Override
    public DailyLeaveVO toVO(DailyLeaveRecord record) {
        LeaveType type = LeaveType.of(record.getLeaveType());
        LeavePeriod period = LeavePeriod.of(record.getPeriod());
        return DailyLeaveVO.builder()
                .leaveDate(record.getLeaveDate())
                .leaveType(record.getLeaveType())
                .leaveTypeName(type == null ? null : type.getDesc())
                .period(record.getPeriod())
                .periodName(period == null ? null : period.getDesc())
                .reason(record.getReason())
                .build();
    }

    /**
     * 事由去首尾空白，空白视为未填（存 null，避免存空串）。
     */
    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 按日期加载当前用户的请假记录，不存在或越权统一抛 404。
     */
    private DailyLeaveRecord loadOwnedByDate(Long userId, LocalDate date) {
        DailyLeaveRecord record = dailyLeaveRecordMapper.selectOne(new LambdaQueryWrapper<DailyLeaveRecord>()
                .eq(DailyLeaveRecord::getUserId, userId)
                .eq(DailyLeaveRecord::getLeaveDate, date));
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "请假记录不存在或无权访问");
        }
        return record;
    }
}
