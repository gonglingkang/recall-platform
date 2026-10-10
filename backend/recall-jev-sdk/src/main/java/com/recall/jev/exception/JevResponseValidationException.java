package com.recall.jev.exception;

/**
 * HTTP 成功但响应体缺失/结构不符合预期（对应 TypeSafeAPIResponseValidationError）。
 *
 * @author recall
 */
public class JevResponseValidationException extends JevApiException {

    private final String fieldPath;

    public JevResponseValidationException(int status, Object body, java.util.Map<String, String> headers,
                                          String fieldPath, String endpoint) {
        super(status, body, headers, endpoint, "Invalid response data at '" + fieldPath + "'.");
        this.fieldPath = fieldPath;
    }

    /** 出错字段的点分路径，如 answers.tone.confidence */
    public String getFieldPath() {
        return fieldPath;
    }
}
