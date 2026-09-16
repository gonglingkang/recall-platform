package com.recall.service.oa.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.common.util.AesCipher;
import com.recall.dao.oa.OaSyncLogMapper;
import com.recall.entity.oa.OaSyncLog;
import com.recall.entity.oa.OaUserConfig;
import com.recall.enums.OaSyncStatus;
import com.recall.enums.OaSyncTriggerType;
import com.recall.service.oa.OaConfigService;
import com.recall.service.oa.OaContentBuilder;
import com.recall.service.oa.OaSyncService;
import com.recall.service.oa.browser.OaSyncExecutor;
import com.recall.service.oa.browser.OaUserLockManager;
import com.recall.vo.oa.OaSyncLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 同步 Service 实现。
 * <p>
 * 并发控制：进程内 key(userId:weekStart) 集合 + DB「运行中」日志双保险；
 * 超过 10 分钟仍「运行中」的残留日志（如进程崩溃）按失败处理并释放。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaSyncServiceImpl implements OaSyncService {

    /** 残留「运行中」日志的判定阈值 */
    private static final Duration STALE_RUNNING = Duration.ofMinutes(10);

    private final OaSyncLogMapper oaSyncLogMapper;
    private final OaConfigService oaConfigService;
    private final OaContentBuilder oaContentBuilder;
    private final OaSyncExecutor oaSyncExecutor;

    @Value("${recall.oa.crypto-key}")
    private String cryptoKey;

    @Value("${recall.oa.debug-headless:true}")
    private boolean headless;

    /** OA 浏览器用户级互斥锁（与考勤抓取共用，防 OA 单点登录互踢） */
    private final OaUserLockManager oaUserLockManager;

    @Override
    public OaSyncLogVO startSync(Long userId, LocalDate anyDate, OaSyncTriggerType trigger) {
        LocalDate weekStart = anyDate.with(DayOfWeek.MONDAY);
        // OA 工时填报有时效：仅本周与上周的协同还在待办可填，更早的周不予同步
        LocalDate thisWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        if (!weekStart.equals(thisWeek) && !weekStart.equals(thisWeek.minusDays(7))) {
            throw new BusinessException(ResultCode.OA_SYNC_WEEK_OUT_OF_RANGE);
        }
        OaUserConfig config = oaConfigService.loadByUserId(userId);
        validateConfig(config);

        markStaleRunning(userId, weekStart);
        if (!oaUserLockManager.tryLock(userId)) {
            throw new BusinessException(ResultCode.OA_SYNC_RUNNING);
        }
        try {
            OaSyncLog runningLog = selectRunningLog(userId, weekStart);
            if (runningLog != null) {
                throw new BusinessException(ResultCode.OA_SYNC_RUNNING);
            }

            OaSyncLog logRow = new OaSyncLog();
            logRow.setUserId(userId);
            logRow.setWeekStart(weekStart);
            logRow.setTriggerType(trigger.getCode());
            logRow.setStatus(OaSyncStatus.RUNNING.getCode());
            logRow.setStep("准备同步");
            logRow.setStartTime(LocalDateTime.now());
            oaSyncLogMapper.insert(logRow);

            try {
                OaContentBuilder.WeekPlan plan = oaContentBuilder.buildWeekPlan(userId, weekStart, config);
                if (plan.days().isEmpty()) {
                    finish(logRow, OaSyncStatus.SUCCESS, "本周无日报与请假，无需同步", null);
                    return toVO(logRow);
                }
                String password = AesCipher.decrypt(config.getPasswordCipher(), cryptoKey);
                oaSyncExecutor.execute(logRow.getId(), config.getOaBaseUrl(), config.getUsername(), password,
                        plan, headless, () -> oaUserLockManager.unlock(userId));
                return toVO(logRow);
            } catch (BusinessException e) {
                finish(logRow, OaSyncStatus.FAILED, "准备同步", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("OA 同步任务提交失败: userId={}, weekStart={}", userId, weekStart, e);
                finish(logRow, OaSyncStatus.FAILED, "准备同步", "同步任务提交失败，请稍后重试");
                throw new BusinessException(ResultCode.OA_SYNC_FAILED, "同步任务提交失败，请稍后重试");
            }
        } catch (RuntimeException e) {
            // 未成功移交锁给异步任务时（准备阶段失败），此处释放
            oaUserLockManager.unlock(userId);
            throw e;
        }
    }

    @Override
    public OaSyncLogVO getStatus(Long userId, LocalDate anyDate) {
        LocalDate weekStart = anyDate.with(DayOfWeek.MONDAY);
        OaSyncLog latest = oaSyncLogMapper.selectOne(new LambdaQueryWrapper<OaSyncLog>()
                .eq(OaSyncLog::getUserId, userId)
                .eq(OaSyncLog::getWeekStart, weekStart)
                .orderByDesc(OaSyncLog::getId)
                .last("LIMIT 1"));
        return latest == null ? null : toVO(latest);
    }

    @Override
    public List<OaSyncLogVO> listLogs(Long userId, int limit) {
        return oaSyncLogMapper.selectList(new LambdaQueryWrapper<OaSyncLog>()
                        .eq(OaSyncLog::getUserId, userId)
                        .orderByDesc(OaSyncLog::getId)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 100))))
                .stream().map(this::toVO).toList();
    }

    // ===================== 辅助 =====================

    private void validateConfig(OaUserConfig config) {
        if (config == null || isBlank(config.getOaBaseUrl()) || isBlank(config.getUsername())
                || isBlank(config.getPasswordCipher()) || isBlank(config.getDevProjectName())
                || isBlank(config.getLeaveProjectName())) {
            throw new BusinessException(ResultCode.OA_CONFIG_INCOMPLETE);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void markStaleRunning(Long userId, LocalDate weekStart) {
        OaSyncLog stale = selectRunningLog(userId, weekStart);
        if (stale != null && stale.getStartTime() != null
                && Duration.between(stale.getStartTime(), LocalDateTime.now()).compareTo(STALE_RUNNING) > 0) {
            log.warn("OA 同步日志残留运行中超时，按失败处理: logId={}, startTime={}", stale.getId(), stale.getStartTime());
            finish(stale, OaSyncStatus.FAILED, stale.getStep(), "同步超时（进程可能中断），已自动重置");
        }
    }

    private OaSyncLog selectRunningLog(Long userId, LocalDate weekStart) {
        return oaSyncLogMapper.selectOne(new LambdaQueryWrapper<OaSyncLog>()
                .eq(OaSyncLog::getUserId, userId)
                .eq(OaSyncLog::getWeekStart, weekStart)
                .eq(OaSyncLog::getStatus, OaSyncStatus.RUNNING.getCode())
                .orderByDesc(OaSyncLog::getId)
                .last("LIMIT 1"));
    }

    private void finish(OaSyncLog logRow, OaSyncStatus status, String step, String errorMsg) {
        logRow.setStatus(status.getCode());
        logRow.setStep(step);
        logRow.setErrorMsg(errorMsg);
        logRow.setEndTime(LocalDateTime.now());
        oaSyncLogMapper.updateById(logRow);
    }

    private OaSyncLogVO toVO(OaSyncLog logRow) {
        OaSyncLogVO vo = new OaSyncLogVO();
        vo.setId(logRow.getId());
        vo.setWeekStart(logRow.getWeekStart());
        vo.setTriggerType(logRow.getTriggerType());
        vo.setStatus(logRow.getStatus());
        vo.setStep(logRow.getStep());
        vo.setErrorMsg(logRow.getErrorMsg());
        vo.setDeadline(logRow.getDeadline());
        vo.setOaSubmitted(logRow.getOaSubmitted());
        vo.setStartTime(logRow.getStartTime());
        vo.setEndTime(logRow.getEndTime());
        return vo;
    }
}
