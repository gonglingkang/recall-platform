package com.recall.service.daily;

import com.recall.entity.daily.DailyAttendanceRecord;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 考勤打卡记录 Service。
 * <p>
 * 数据来源：OA「考勤打卡记录」每日定时抓取（写入经 {@code OaAttendanceSyncService}），
 * 本 Service 负责查询与 upsert；同用户同日期唯一。
 *
 * @author recall
 */
public interface DailyAttendanceService {

    /**
     * 查询指定用户多天的考勤记录（日报渲染用）。
     *
     * @param userId 用户
     * @param dates  考勤日期集合
     * @return 日期 -> 考勤记录；无记录的日期不在 Map 中
     */
    Map<LocalDate, DailyAttendanceRecord> mapByDates(Long userId, Collection<LocalDate> dates);

    /**
     * 查询指定用户指定日期的考勤记录。
     *
     * @param userId 用户
     * @param date   考勤日期
     * @return 考勤记录；无记录返回 null
     */
    DailyAttendanceRecord getByDate(Long userId, LocalDate date);

    /**
     * 查询指定用户缺失考勤记录的日期（补抓扫描用）。
     *
     * @param userId 用户
     * @param dates  候选日期集合
     * @return 其中无考勤记录的日期
     */
    List<LocalDate> findMissingDates(Long userId, Collection<LocalDate> dates);

    /**
     * 查询指定用户某月的全部考勤记录（按日期升序）。
     *
     * @param userId 用户
     * @param month  月份 YYYY-MM
     * @return 考勤记录列表
     */
    List<DailyAttendanceRecord> listByMonth(Long userId, String month);

    /**
     * upsert 一条考勤记录：同用户同日期存在则覆盖，不存在则建。
     *
     * @param record 考勤记录（userId/workDate 必填）
     */
    void upsert(DailyAttendanceRecord record);
}
