package com.recall.service.todo.impl;

import com.recall.service.todo.TodoTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 周期待办生成任务。
 * <p>
 * 每分钟扫描（到期判定=next_date + 配置时间点 已到，分钟级精度）；
 * 另在应用启动完成后补跑一次——个人电脑可能错过触发时刻（关机），补跑与定时
 * 共用同一幂等逻辑（next_date 推进后同日重复执行自然跳过），关机错过的周期不丢失。
 *
 * @author recall
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoTemplateGenerateJob {

    private final TodoTemplateService todoTemplateService;

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void scheduledGenerate() {
        run("定时扫描");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        run("启动补跑");
    }

    private void run(String source) {
        try {
            int generated = todoTemplateService.generateDueTodos();
            if (generated > 0) {
                log.info("周期待办生成完成({}): 生成 {} 条", source, generated);
            }
        } catch (Exception e) {
            log.error("周期待办生成失败({}): {}", source, e.getMessage(), e);
        }
    }
}
