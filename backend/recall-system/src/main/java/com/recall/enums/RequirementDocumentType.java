package com.recall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 需求文档类型。
 * <p>原型设计(1) / 需求文档(2) / 会议纪要(3)。
 * <p>数据库存数字码（字符串形式），API 返回数字码，前端自行映射文案。
 *
 * @author recall
 */
@Getter
public enum RequirementDocumentType {
    /** 原型设计 */
    PROTOTYPE("1"),
    /** 需求文档 */
    REQUIREMENT("2"),
    /** 会议纪要 */
    MEETING("3");

    private final String value;

    RequirementDocumentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RequirementDocumentType of(String value) {
        if (value == null) {
            return null;
        }
        for (RequirementDocumentType t : values()) {
            if (t.value.equals(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知的需求文档类型: " + value);
    }
}
