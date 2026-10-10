package com.recall.jev.exception;

/**
 * jev SDK 基础异常（对应 Python SDK 的 TypeSafeError）。
 *
 * @author recall
 */
public class JevException extends RuntimeException {

    public JevException(String message) {
        super(message);
    }

    public JevException(String message, Throwable cause) {
        super(message, cause);
    }
}
