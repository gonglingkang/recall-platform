package com.recall.jev;

import com.recall.jev.exception.JevApiException;
import com.recall.jev.exception.JevConnectionException;
import com.recall.jev.exception.JevException;
import com.recall.jev.exception.JevTimeoutException;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * jev 客户端重试策略（对应 Python SDK RetryPolicy 的内置语义）：
 * 可重试条件 = 超时 / 连接失败 / 状态码在 {408,429,5xx}；
 * 等待时间优先取服务端 Retry-After，否则指数退避 0.5s 起、上限 5s、随机抖动 ≤25%。
 *
 * @author recall
 */
public final class JevRetryPolicy {

    /** 可重试的 HTTP 状态码 */
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(408, 429, 500, 501, 502, 503, 504);

    private static final long BACKOFF_INITIAL_MS = 500;
    private static final long BACKOFF_MAX_MS = 5_000;
    private static final double BACKOFF_JITTER = 0.25;

    private JevRetryPolicy() {
    }

    /** 该异常是否值得重试 */
    public static boolean isRetryable(JevException error) {
        if (error instanceof JevTimeoutException || error instanceof JevConnectionException) {
            return true;
        }
        if (error instanceof JevApiException api) {
            return RETRYABLE_STATUSES.contains(api.getStatus());
        }
        return false;
    }

    /** 本次重试等待毫秒数：Retry-After 优先，否则指数退避（attempt 从 0 计，首次重试等 500ms） */
    public static long computeDelayMs(JevException error, int attempt) {
        if (error instanceof JevApiException api) {
            Long retryAfter = api.getRetryAfterMs();
            if (retryAfter != null) {
                return retryAfter;
            }
        }
        double exponential = Math.min(BACKOFF_MAX_MS, BACKOFF_INITIAL_MS * Math.pow(2, attempt));
        double delay = exponential * (1 - ThreadLocalRandom.current().nextDouble() * BACKOFF_JITTER);
        return (long) delay;
    }
}
