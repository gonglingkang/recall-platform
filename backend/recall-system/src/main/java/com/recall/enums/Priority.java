package com.recall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 待办优先级（PRD 4.3）。
 * <p>
 * 状态码：低(0)、中(1)、高(2)；新建默认中(1)。
 * weight 为排序权重，越大越靠前。
 * 数据库存数字码（字符串形式），API 返回数字码。
 *
 * @author recall
 */
@Getter
public enum Priority {
    /** 低：灰色 */
    LOW("0", 1),
    /** 中：橙色/默认 */
    MEDIUM("1", 2),
    /** 高：红色标记 */
    HIGH("2", 3);

    private final String value;
    /** 排序权重，越大越靠前 */
    private final int weight;

    Priority(String value, int weight) {
        this.value = value;
        this.weight = weight;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Priority of(String value) {
        if (value == null) {
            return null;
        }
        for (Priority p : values()) {
            if (p.value.equals(value) || p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        return MEDIUM;
    }
}
