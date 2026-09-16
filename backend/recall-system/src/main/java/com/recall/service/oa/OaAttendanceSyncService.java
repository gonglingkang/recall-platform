package com.recall.service.oa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.util.AesCipher;
import com.recall.dao.oa.OaUserConfigMapper;
import com.recall.entity.daily.DailyAttendanceRecord;
import com.recall.entity.oa.OaUserConfig;
import com.recall.service.daily.DailyAttendanceService;
import com.recall.service.oa.browser.OaAttendanceClient;
import com.recall.service.oa.browser.OaUserLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


/**
 * OA 考勤打卡记录定时抓取服务。
 * <p>
 * OA 打卡数据非实时（第二天才完整），每天早上抓取前一天；
 * 扫描范围为当月 1 日 ~ 昨天（月初自动重扫整月，缺口自然补齐）。
 * 休息日不入库（补班日 OA 会产生打卡记录，自然保留）。
 * 全部用户串行处理，每个用户抓取前抢 {@link OaUserLockManager} 锁
 * （与工时同步互斥，防 OA 单点登录互踢）。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaAttendanceSyncService {

    private final OaUserConfigMapper oaUserConfigMapper;
    private final DailyAttendanceService dailyAttendanceService;
    private final OaAttendanceClient oaAttendanceClient;
    private final OaUserLockManager oaUserLockManager;

    @Value("${recall.oa.debug-headless:true}")
    private boolean headless;

    @Value("${recall.oa.crypto-key}")
    private String cryptoKey;

    /**
     * 每日 08:00（Asia/Shanghai）抓取：当月 1 日~昨天内缺失考勤记录的日期。
     */
    @Scheduled(cron = "${recall.oa.attendance.cron:0 0 8 * * ?}", zone = "Asia/Shanghai")
    public void scheduledFetch() {
        log.info("OA 考勤定时抓取开始");
        syncAll();
    }

    /**
     * 抓取全部 OA 配置用户当月缺失的考勤记录（定时入口与手动触发共用）。
     *
     * @return 处理的用户数
     */
    public int syncAll() {
        List<OaUserConfig> configs = oaUserConfigMapper.selectList(new LambdaQueryWrapper<>());
        int handled = 0;
        for (OaUserConfig config : configs) {
            try {
                if (syncUser(config)) {
                    handled++;
                }
            } catch (Exception e) {
                log.warn("OA 考勤抓取失败: userId={}, err={}", config.getUserId(), e.getMessage());
            }
        }
        log.info("OA 考勤抓取结束: 处理用户数={}", handled);
        return handled;
    }

    /**
     * 抓取单个用户当月（1日~昨天）缺失的考勤记录。
     * <p>
     * 休息日不入库（补班日 OA 会产生打卡记录，自然保留）。
     *
     * @return true=有抓取动作（打开过浏览器）
     */
    public boolean syncUser(OaUserConfig config) {
        Long userId = config.getUserId();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        // 候选范围：当月 1 日 ~ 昨天（每月初自动重扫整月，缺口自然补齐）
        List<LocalDate> candidates = today.withDayOfMonth(1).datesUntil(today).toList();
        List<LocalDate> missing = dailyAttendanceService.findMissingDates(userId, candidates);
        if (missing.isEmpty()) {
            return false;
        }
        if (!oaUserLockManager.tryLock(userId)) {
            log.info("OA 考勤抓取跳过（浏览器被其他任务占用）: userId={}", userId);
            return false;
        }
        try {
            String password = AesCipher.decrypt(config.getPasswordCipher(), cryptoKey);
            // 就近到远，同一浏览器会话内逐日筛选读取
            List<LocalDate> ordered = missing.stream().sorted().toList();
            var rows = oaAttendanceClient.fetchDays(config.getOaBaseUrl(), config.getUsername(),
                    password, ordered, headless);
            for (LocalDate date : ordered) {
                OaAttendanceClient.AttendanceRow r = rows.get(date);
                if (r == null) {
                    log.info("OA 无该日考勤数据（下次再补抓）: userId={}, date={}", userId, date);
                    continue;
                }
                // 休息日不入库；补班日 OA 会产生打卡记录（有上班卡），自然保留
                if ("休息日".equals(r.dateType()) && (r.clockIn() == null || r.clockIn().isBlank())) {
                    log.info("休息日无打卡，跳过入库: userId={}, date={}", userId, date);
                    continue;
                }
                DailyAttendanceRecord record = new DailyAttendanceRecord();
                record.setUserId(userId);
                record.setWorkDate(r.workDate());
                record.setDateType(emptyToNull(r.dateType()));
                record.setClockIn(emptyToNull(r.clockIn()));
                record.setClockOut(emptyToNull(r.clockOut()));
                record.setAttendanceResult(emptyToNull(r.attendanceResult()));
                record.setAttendanceStatus(emptyToNull(r.attendanceStatus()));
                record.setOnLeave(r.onLeave());
                dailyAttendanceService.upsert(record);
                log.info("OA 考勤记录入库: userId={}, date={}, in={}, status={}",
                        userId, date, r.clockIn(), r.attendanceStatus());
            }
            return true;
        } finally {
            oaUserLockManager.unlock(userId);
        }
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
