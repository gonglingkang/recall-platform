package com.recall.service.daily.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.dao.daily.DailyAttendanceRecordMapper;
import com.recall.entity.daily.DailyAttendanceRecord;
import com.recall.service.daily.DailyAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考勤打卡记录 Service 实现。
 * <p>
 * 持有 DailyAttendanceRecordMapper（唯一归属）；写入来自 OA 考勤抓取，查询供日报渲染。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAttendanceServiceImpl implements DailyAttendanceService {

    private final DailyAttendanceRecordMapper dailyAttendanceRecordMapper;

    @Override
    public Map<LocalDate, DailyAttendanceRecord> mapByDates(Long userId, Collection<LocalDate> dates) {
        if (userId == null || dates == null || dates.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DailyAttendanceRecord> records = dailyAttendanceRecordMapper.selectList(
                new LambdaQueryWrapper<DailyAttendanceRecord>()
                        .eq(DailyAttendanceRecord::getUserId, userId)
                        .in(DailyAttendanceRecord::getWorkDate, dates));
        return records.stream().collect(Collectors.toMap(DailyAttendanceRecord::getWorkDate, r -> r));
    }

    @Override
    public DailyAttendanceRecord getByDate(Long userId, LocalDate date) {
        return dailyAttendanceRecordMapper.selectOne(new LambdaQueryWrapper<DailyAttendanceRecord>()
                .eq(DailyAttendanceRecord::getUserId, userId)
                .eq(DailyAttendanceRecord::getWorkDate, date));
    }

    @Override
    public List<LocalDate> findMissingDates(Long userId, Collection<LocalDate> dates) {
        Map<LocalDate, DailyAttendanceRecord> existing = mapByDates(userId, dates);
        return dates.stream().filter(d -> !existing.containsKey(d)).toList();
    }

    @Override
    public List<DailyAttendanceRecord> listByMonth(Long userId, String month) {
        YearMonth ym = YearMonth.parse(month);
        return dailyAttendanceRecordMapper.selectList(new LambdaQueryWrapper<DailyAttendanceRecord>()
                .eq(DailyAttendanceRecord::getUserId, userId)
                .ge(DailyAttendanceRecord::getWorkDate, ym.atDay(1))
                .lt(DailyAttendanceRecord::getWorkDate, ym.plusMonths(1).atDay(1))
                .orderByAsc(DailyAttendanceRecord::getWorkDate));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsert(DailyAttendanceRecord record) {
        DailyAttendanceRecord existing = getByDate(record.getUserId(), record.getWorkDate());
        if (existing == null) {
            dailyAttendanceRecordMapper.insert(record);
        } else {
            record.setId(existing.getId());
            dailyAttendanceRecordMapper.updateById(record);
        }
    }
}
