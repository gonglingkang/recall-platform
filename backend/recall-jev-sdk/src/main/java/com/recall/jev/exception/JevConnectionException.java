package com.recall.jev.exception;

/**
 * 请求无法到达服务端（连接失败/读失败），可重试（对应 TypeSafeAPIConnectionError）。
 *
 * @author recall
 */
public class JevConnectionException extends JevException {

    public JevConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
