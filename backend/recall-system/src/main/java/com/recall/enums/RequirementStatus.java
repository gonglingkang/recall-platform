package com.recall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 需求状态。
 * <p>
 * 状态码：讨论中(0)、不涉及(1)、进行中(2)、开发完成(3)、验收完成(4)、发布完成(5)。
 * <p>
 * K 状态映射（绑 K 时，讨论中/进行中/开发完成三态由 K 状态驱动，禁止手动改这三态）：
 * <ul>
 *   <li>K 未开始(0) -> 讨论中(0)</li>
 *   <li>K 进行中(1) -> 进行中(2)</li>
 *   <li>K 已完成(2) -> 开发完成(3)</li>
 *   <li>K 已取消(3) -> 解绑 K，回讨论中(0)</li>
 * </ul>
 * 手动合法流转：
 * <ul>
 *   <li>讨论中 -> 不涉及 / 进行中</li>
 *   <li>不涉及 -> 讨论中 / 进行中</li>
 *   <li>进行中 -> 讨论中 / 不涉及 / 开发完成</li>
 *   <li>开发完成 -> 进行中 / 不涉及 / 验收完成 / 发布完成</li>
 *   <li>验收完成 -> 发布完成（不可回退）</li>
 *   <li>发布完成为终态，不可流转</li>
 * </ul>
 * 进入不涉及时解绑 K；进入验收完成/发布完成时保留 K 关联（仅断开联动，便于事后追溯），
 * 之后 K 不再驱动。
 * <p>
 * 数据库存数字码（字符串形式），API 返回数字码，前端自行映射文案。
 *
 * @author recall
 */
@Getter
public enum RequirementStatus {
    /** 讨论中（K 活跃态：绑 K 时跟 K 未开始） */
    DISCUSSING("0"),
    /** 不涉及（决定不做，自动解绑 K） */
    NOT_INVOLVED("1"),
    /** 进行中（K 活跃态：绑 K 时跟 K 进行中） */
    IN_PROGRESS("2"),
    /** 开发完成（K 活跃态：绑 K 时跟 K 已完成） */
    DEV_DONE("3"),
    /** 验收完成（手动，进入时解绑 K） */
    ACCEPTANCE_DONE("4"),
    /** 发布完成（终态，进入时解绑 K） */
    RELEASED("5");

    private final String value;

    RequirementStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RequirementStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (RequirementStatus s : values()) {
            if (s.value.equals(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的需求状态: " + value);
    }

    /**
     * 是否为 K 活跃态（绑 K 时由 K 状态驱动，禁止手动改这三态）。
     * <p>讨论中 / 进行中 / 开发完成。
     *
     * @return true 表示当前状态由绑定的 K 驱动
     */
    public boolean isActiveKrState() {
        return this == DISCUSSING || this == IN_PROGRESS || this == DEV_DONE;
    }

    /**
     * 是否为终态（不可再流转）。
     *
     * @return true 表示终态
     */
    public boolean isTerminal() {
        return this == RELEASED;
    }
}
