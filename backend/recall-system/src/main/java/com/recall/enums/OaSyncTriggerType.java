package com.recall.enums;

import lombok.Getter;

/**
 * OA 同步触发方式。
 * <p>
 * 数字码：自动(1，日报保存后触发)、手动(2，周视图按钮兜底)。
 *
 * @author recall
 */
@Getter
public enum OaSyncTriggerType {
    /** 自动（日报/请假保存后触发） */
    AUTO(1, "自动"),
    /** 手动（周视图「同步OA」按钮） */
    MANUAL(2, "手动");

    private final int code;
    private final String desc;

    OaSyncTriggerType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }
}
