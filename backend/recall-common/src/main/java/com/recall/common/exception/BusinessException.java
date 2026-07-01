package com.recall.common.exception;

import com.recall.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常。Service 层抛出后由 GlobalExceptionHandler 统一转换为 Result。
 *
 * @author recall
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
