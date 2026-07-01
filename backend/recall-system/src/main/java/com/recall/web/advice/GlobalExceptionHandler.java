package com.recall.web.advice;

import com.recall.common.api.Result;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。把各类异常统一转换为 {@link Result}，保证响应结构一致。
 * <p>
 * 位于 system 模块，可处理 Spring Security 相关异常（common 模块不依赖 security）。
 *
 * @author recall
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：使用其携带的 code */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：@Valid 校验 body */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + defaultMsg(fe))
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败(body): {}", msg, e);
        return Result.fail(ResultCode.PARAM_VALIDATE_FAILED, msg);
    }

    /** 参数校验失败：表单绑定 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + defaultMsg(fe))
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败(bind): {}", msg, e);
        return Result.fail(ResultCode.PARAM_VALIDATE_FAILED, msg);
    }

    /** 参数校验失败：@RequestParam / @PathVariable 上的校验 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        log.warn("参数校验失败(constraint): {}", e.getMessage(), e);
        return Result.fail(ResultCode.PARAM_VALIDATE_FAILED, e.getMessage());
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName(), e);
        return Result.fail(ResultCode.BAD_REQUEST, "缺少必填参数: " + e.getParameterName());
    }

    /** 请求体不可读（JSON 格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage(), e);
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    /** 不支持的请求方法 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMessage(), e);
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    /** 认证失败（Spring Security 抛出） */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuth(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage(), e);
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /** 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage(), e);
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }

    private String defaultMsg(FieldError fe) {
        return fe.getDefaultMessage() == null ? "不合法" : fe.getDefaultMessage();
    }
}
