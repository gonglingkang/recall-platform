package com.recall.jev.exception;

/**
 * 请求超出配置超时（对应 TypeSafeAPITimeoutError），可重试。
 *
 * @author recall
 */
public class JevTimeoutException extends JevException {

    private final long timeoutMillis;

    public JevTimeoutException(long timeoutMillis) {
        super("Request timed out (timeout=" + timeoutMillis + "ms).");
        this.timeoutMillis = timeoutMillis;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}
