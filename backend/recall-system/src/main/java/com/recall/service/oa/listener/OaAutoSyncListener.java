package com.recall.service.oa.listener;

import com.recall.entity.oa.OaUserConfig;
import com.recall.enums.OaSyncTriggerType;
import com.recall.service.oa.OaConfigService;
import com.recall.service.oa.OaSyncService;
import com.recall.service.oa.event.DailyChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OA 自动同步监听器。
 * <p>
 * 日报/请假保存后触发（用户配置 auto_sync 开启时），同步该周整周内容到 OA。
 * AFTER_COMMIT + @Async：保证读到已提交数据且不阻塞保存接口；
 * 同步失败只记日志，不影响日报保存结果（手动按钮兜底）。
 *
 * @author recall
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OaAutoSyncListener {

    private final OaConfigService oaConfigService;
    private final OaSyncService oaSyncService;

    @Async("oaSyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDailyChanged(DailyChangedEvent event) {
        try {
            OaUserConfig config = oaConfigService.loadByUserId(event.userId());
            if (config == null || !Boolean.TRUE.equals(config.getAutoSync())) {
                return;
            }
            oaSyncService.startSync(event.userId(), event.date(), OaSyncTriggerType.AUTO);
        } catch (Exception e) {
            // 自动同步失败不打扰用户（手动同步兜底），仅记录
            log.warn("OA 自动同步未执行或失败: userId={}, date={}, reason={}",
                    event.userId(), event.date(), e.getMessage());
        }
    }
}
