package com.recall.common.api;

import lombok.Getter;

/**
 * 统一错误码枚举。
 * <p>
 * 约定：code 取 HTTP 语义相关值，便于前端统一处理。
 *
 * @author recall
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),

    // --- 客户端通用错误 4xx ---
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    CONFLICT(409, "资源冲突"),

    // --- 业务错误 4xx ---
    PARAM_VALIDATE_FAILED(422, "参数校验失败"),
    USERNAME_EXISTS(4101, "用户名已被占用"),
    EMAIL_EXISTS(4102, "邮箱已被占用"),
    LOGIN_FAILED(4103, "账号或密码错误"),
    ACCOUNT_LOCKED(4104, "登录失败次数过多，请稍后再试"),
    OLD_PASSWORD_WRONG(4105, "原密码错误"),
    TODO_TITLE_DUPLICATED(4201, "今天已存在同名待办"),

    // --- 服务器错误 5xx ---
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
