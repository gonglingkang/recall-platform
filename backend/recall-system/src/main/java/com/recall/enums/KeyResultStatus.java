package com.recall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 关键成果 K 的状态（月度绩效 v2.0）。
 * <p>
 * 状态码：未开始(0)、进行中(1)、已完成(2)、已取消(3)。
 * <p>
 * 合法流转（严格状态机）：
 * <ul>
 *   <li>未开始(0) → 进行中(1)：开始</li>
 *   <li>未开始(0) → 已完成(2)：跳级完成（事情已做完）</li>
 *   <li>未开始(0) → 已取消(3)：取消（需求未定/无法落实）</li>
 *   <li>进行中(1) → 已完成(2)：完成</li>
 *   <li>进行中(1) → 已取消(3)：取消</li>
 *   <li>已完成(2) → 进行中(1)：取消完成（返工）</li>
 *   <li>已取消(3) → 未开始(0)：恢复</li>
 * </ul>
 * 其余流转非法（已完成→已取消、已取消→进行中/已完成、同状态等）。
 * 已取消的 K 不参与 O 的派生计算（progress/status 均排除）。
 * <p>
 * 数据库存数字码（字符串形式），API 返回数字码（int），前端自行映射文案。
 *
 * @author recall
 */
@Getter
public enum KeyResultStatus {
    /** 未开始 */
    NOT_STARTED("0"),
    /** 进行中 */
    IN_PROGRESS("1"),
    /** 已完成 */
    DONE("2"),
    /** 已取消（需求未定/无法落实，保留记录不参与进度统计） */
    CANCELLED("3");

    private final String value;

    KeyResultStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static KeyResultStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (KeyResultStatus s : values()) {
            if (s.value.equals(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的关键成果状态: " + value);
    }
}
