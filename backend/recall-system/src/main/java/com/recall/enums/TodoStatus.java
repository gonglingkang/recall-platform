package com.recall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 待办状态。
 * <p>
 * pending(0,待处理) ──标记完成──> done(1,已完成) ──撤销──> pending。
 * <p>
 * 数据库存数字码（字符串形式），API 返回数字码。
 *
 * @author recall
 */
@Getter
public enum TodoStatus {
    /** 待处理 */
    PENDING("0"),
    /** 已完成 */
    DONE("1");

    private final String value;

    TodoStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TodoStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (TodoStatus s : values()) {
            if (s.value.equals(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的待办状态: " + value);
    }
}
