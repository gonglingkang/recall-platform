package com.recall.service.oa.browser;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.dao.oa.OaSyncLogMapper;
import com.recall.entity.oa.OaSyncLog;
import com.recall.enums.OaSyncStatus;
import com.recall.service.oa.OaContentBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * OA 同步异步执行器。
 * <p>
 * 独立 Bean 承载 @Async（避免同类自调用失效）。整轮失败自动重试 1 次
 * （OA 页面较重，实测偶发崩溃/选择器超时；每次重试全新浏览器实例，暂存是幂等覆盖，重试安全）。
 *
 * @author recall
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OaSyncExecutor {

    private final OaSyncLogMapper oaSyncLogMapper;
    private final OaPlaywrightClient oaPlaywrightClient;

    /**
     * 执行同步（异步）。
     *
     * @param logId     同步日志 id
     * @param baseUrl   OA 根地址
     * @param username  OA 账号
     * @param password  OA 密码明文
     * @param plan      整周填写计划
     * @param headless  是否无头
     * @param onDone    结束回调（释放进程内并发锁）
     */
    @Async("oaSyncTaskExecutor")
    public void execute(Long logId, String baseUrl, String username, String password,
                        OaContentBuilder.WeekPlan plan, boolean headless, Runnable onDone) {
        Exception lastError = null;
        // 同步范围标注（仅某天/整周），写入日志 step 便于用户识别
        String scope = plan.days().size() == 1
                ? "（仅" + plan.days().get(0).date() + "）" : "（整周）";
        try {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    if (attempt > 1) {
                        log.info("OA 同步重试: logId={}, attempt={}", logId, attempt);
                        updateStep(logId, "重试同步" + scope);
                    }
                    oaPlaywrightClient.run(baseUrl, username, password, plan, headless,
                            step -> updateStep(logId, step + scope),
                            meta -> saveMeta(logId, meta));
                    finish(logId, OaSyncStatus.SUCCESS, "暂存待办完成" + scope, null);
                    log.info("OA 同步成功: logId={}, scope={}", logId, scope);
                    return;
                } catch (OaPlaywrightClient.CollabNotFoundException ce) {
                    // 找不到协同不重试：若该周此前同步成功过（协同曾存在），判定周报已在 OA 提交，
                    // 状态置为「已在OA提交」并停止提供同步；否则维持「协同未发起」失败
                    if (hasSuccessLog(logId)) {
                        finishSubmitted(logId);
                        log.info("OA 周报判定已提交（协同已不在待办）: logId={}", logId);
                    } else {
                        finish(logId, OaSyncStatus.FAILED, "同步失败", ce.getMessage());
                    }
                    return;
                } catch (Exception e) {
                    lastError = e;
                    log.warn("OA 同步第 {} 次尝试失败: logId={}", attempt, logId, e);
                }
            }
            String msg = lastError == null ? "未知错误"
                    : (lastError.getMessage() == null ? lastError.getClass().getSimpleName() : lastError.getMessage());
            finish(logId, OaSyncStatus.FAILED, "同步失败", msg);
        } finally {
            onDone.run();
        }
    }

    /** 该周是否有过成功的同步记录（logId 之外）——有则说明协同曾存在 */
    private boolean hasSuccessLog(Long logId) {
        OaSyncLog current = oaSyncLogMapper.selectById(logId);
        if (current == null) {
            return false;
        }
        Long count = oaSyncLogMapper.selectCount(new LambdaQueryWrapper<OaSyncLog>()
                .eq(OaSyncLog::getUserId, current.getUserId())
                .eq(OaSyncLog::getWeekStart, current.getWeekStart())
                .eq(OaSyncLog::getStatus, OaSyncStatus.SUCCESS.getCode())
                .ne(OaSyncLog::getId, logId));
        return count != null && count > 0;
    }

    /** 周报已提交收尾：成功状态 + 已提交标记（deadline 等已有字段不受影响） */
    private void finishSubmitted(Long logId) {
        OaSyncLog logRow = new OaSyncLog();
        logRow.setId(logId);
        logRow.setStatus(OaSyncStatus.SUCCESS.getCode());
        logRow.setStep("已在OA提交（协同已不在待办）");
        logRow.setOaSubmitted(true);
        logRow.setEndTime(LocalDateTime.now());
        oaSyncLogMapper.updateById(logRow);
    }

    /** 持久化表单元数据（截止时间/该周是否已填报）——读到即写，与同步成败无关 */
    private void saveMeta(Long logId, OaPlaywrightClient.OaFormMeta meta) {
        OaSyncLog logRow = new OaSyncLog();
        logRow.setId(logId);
        logRow.setDeadline(meta.deadline());
        logRow.setOaSubmitted(meta.submitted());
        oaSyncLogMapper.updateById(logRow);
        log.info("OA 表单元数据: logId={}, deadline={}, submitted={}", logId, meta.deadline(), meta.submitted());
    }

    private void updateStep(Long logId, String step) {
        OaSyncLog logRow = new OaSyncLog();
        logRow.setId(logId);
        logRow.setStep(step);
        oaSyncLogMapper.updateById(logRow);
    }

    private void finish(Long logId, OaSyncStatus status, String step, String errorMsg) {
        OaSyncLog logRow = new OaSyncLog();
        logRow.setId(logId);
        logRow.setStatus(status.getCode());
        logRow.setStep(step);
        logRow.setErrorMsg(errorMsg);
        logRow.setEndTime(LocalDateTime.now());
        oaSyncLogMapper.updateById(logRow);
    }
}
