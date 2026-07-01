package com.recall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 团队冲刺任务状态。
 * <p>
 * 状态码：未开始(0)、进行中(1)、已完成(2)。
 * <p>手动变更（仅「需我介入 + 无关联 K」的冲刺任务允许）遵循严格状态机，合法流转：
 * <ul>
 *   <li>未开始(0) → 进行中(1)：开始</li>
 *   <li>进行中(1) → 已完成(2)：完成</li>
 *   <li>已完成(2) → 进行中(1)：返工（取消完成）</li>
 * </ul>
 * 其余流转（未开始→已完成、进行中→未开始、已完成→未开始、同状态等）非法，抛 409。
 * <p>关联关键成果 K 后，冲刺状态由 {@code SprintService#deriveSprintStatus} 派生，
 * {@link com.recall.service.sprint.SprintService#linkKeyResults}、
 * {@link com.recall.service.sprint.SprintService#syncStatusByKeyResult}、
 * {@link com.recall.service.sprint.SprintService#recomputeStatus} 三处共用同一套规则：
 * <ul>
 *   <li>排除已取消的 K，剩余为「有效 K」</li>
 *   <li>有效 K 全未开始 → 未开始(0)</li>
 *   <li>有效 K 全已完成 → 已完成(2)</li>
 *   <li>其他（含混合、含进行中）→ 进行中(1)</li>
 *   <li>无有效 K（全部已取消或无关联）→ 未开始(0)</li>
 * </ul>
 * 已关联 K 的冲刺任务状态由上述派生接管，禁止手动变更（{@code changeStatus} 抛 409）。
 * 数据库存数字码（字符串形式），API 返回数字码。
 *
 * @author recall
 */
@Getter
public enum SprintStatus {
    /** 未开始 */
    NOT_STARTED("0"),
    /** 进行中 */
    IN_PROGRESS("1"),
    /** 已完成 */
    DONE("2");

    private final String value;

    SprintStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SprintStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (SprintStatus s : values()) {
            if (s.value.equals(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的冲刺状态: " + value);
    }
}
