package com.recall.jev.exception;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * HTTP 非成功响应异常（对应 TypeSafeAPIError），携带状态码、响应体与请求元数据。
 * <p>
 * 错误信息优先取服务端结构化字段（error/message/detail），退化截取原始体前 200 字符。
 *
 * @author recall
 */
public class JevApiException extends JevException {

    private static final int MAX_ERROR_BODY_LENGTH = 200;

    private final int status;
    private final transient Object body;
    private final transient Map<String, String> headers;
    private final String endpoint;
    private final String message;

    public JevApiException(int status, Object body, Map<String, String> headers, String endpoint) {
        super(buildMessage(status, body));
        this.status = status;
        this.body = body;
        this.headers = headers;
        this.endpoint = endpoint;
        this.message = super.getMessage();
    }

    public JevApiException(int status, Object body, Map<String, String> headers, String endpoint, String message) {
        super(message);
        this.status = status;
        this.body = body;
        this.headers = headers;
        this.endpoint = endpoint;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public Object getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    /** 请求方法与 URL（不含凭据/查询参数），可能为 null */
    public String getEndpoint() {
        return endpoint;
    }

    /** x-typesafe-request-id 响应头，无则 null */
    public String getRequestId() {
        return headers == null ? null : headers.get("x-typesafe-request-id");
    }

    @Override
    public String getMessage() {
        String base = message == null || message.isBlank() ? String.valueOf(status) : status + " " + message;
        if (endpoint != null) {
            base = endpoint + ": " + base;
        }
        String requestId = getRequestId();
        return requestId == null ? base : base + " (request_id=" + requestId + ")";
    }

    /** 服务端要求的等待毫秒数（retry-after-ms / retry-after，含 HTTP-date 形式），无则 null */
    public Long getRetryAfterMs() {
        return parseRetryAfterMs(headers);
    }

    public static String buildMessage(int status, Object body) {
        String detail = extractMessage(body);
        if (detail != null) {
            return detail;
        }
        if (body == null) {
            return status + " (no body)";
        }
        String raw = body.toString();
        return raw.length() > MAX_ERROR_BODY_LENGTH ? raw.substring(0, MAX_ERROR_BODY_LENGTH) + "…" : raw;
    }

    /** 从响应头解析重试等待毫秒数；无有效值返回 null（对应 parse_retry_after） */
    public static Long parseRetryAfterMs(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        String ms = headers.get("retry-after-ms");
        if (ms != null && !ms.isBlank()) {
            Long value = parseMillis(ms, 1);
            if (value != null) {
                return value;
            }
        }
        String seconds = headers.get("retry-after");
        if (seconds != null && !seconds.isBlank()) {
            Long value = parseMillis(seconds, 1000);
            if (value != null) {
                return value;
            }
            try {
                OffsetDateTime date = OffsetDateTime.parse(seconds.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                return Math.max(0L, date.toInstant().toEpochMilli() - System.currentTimeMillis());
            } catch (DateTimeParseException ignored) {
                // 非 HTTP-date 形式，忽略
            }
        }
        return null;
    }

    private static Long parseMillis(String raw, long multiplier) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (Double.isFinite(value) && value >= 0) {
                double delay = value * multiplier;
                return Double.isFinite(delay) ? (long) delay : null;
            }
        } catch (NumberFormatException ignored) {
            // 走调用方的其他解析分支
        }
        return null;
    }

    /** 从 JSON 体提取错误信息（error/error.message/message/detail/detail.message/detail[].msg） */
    public static String extractMessage(Object body) {
        if (body instanceof String s) {
            return s.isBlank() ? null : s;
        }
        if (!(body instanceof Map<?, ?> map)) {
            return null;
        }
        Object error = map.get("error");
        if (error instanceof String s) {
            return s;
        }
        if (error instanceof Map<?, ?> errorMap && errorMap.get("message") instanceof String s) {
            return s;
        }
        if (map.get("message") instanceof String s) {
            return s;
        }
        Object detail = map.get("detail");
        if (detail instanceof String s) {
            return s;
        }
        if (detail instanceof Map<?, ?> detailMap && detailMap.get("message") instanceof String s) {
            return s;
        }
        if (detail instanceof java.util.List<?> list) {
            StringBuilder parts = new StringBuilder();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> entryMap && entryMap.get("msg") instanceof String msg) {
                    Object loc = entryMap.get("loc");
                    String path = "";
                    if (loc instanceof java.util.List<?> locList) {
                        StringBuilder sb = new StringBuilder();
                        for (Object item : locList) {
                            if ("body".equals(item)) {
                                continue;
                            }
                            sb.append(sb.length() == 0 ? item : "." + item);
                        }
                        path = sb.toString();
                    }
                    parts.append(parts.length() == 0 ? (path.isEmpty() ? msg : path + ": " + msg)
                            : (path.isEmpty() ? ";" + msg : ";" + path + ": " + msg));
                }
            }
            return parts.length() == 0 ? null : parts.toString();
        }
        return null;
    }
}
