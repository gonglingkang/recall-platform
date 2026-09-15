package com.recall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置。
 * <p>
 * OA 同步单次约 1-3 分钟（驱动无头浏览器），不能占用请求线程；
 * 队列有界，打满时快速失败由调用方提示，避免任务堆积。
 *
 * @author recall
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("oaSyncTaskExecutor")
    public ThreadPoolTaskExecutor oaSyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("oa-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
