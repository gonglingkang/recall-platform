package com.recall.enums;

import lombok.Getter;

/**
 * OA 同步状态。
 * <p>
 * 数字码：运行中(1)、成功(2)、失败(3)。
 *
 * @author recall
 */
@Getter
public enum OaSyncStatus {
    /** 运行中 */
    RUNNING(1, "同步中"),
    /** 成功 */
    SUCCESS(2, "已同步"),
    /** 失败 */
    FAILED(3, "失败");

    private final int code;
    private final String desc;

    OaSyncStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按数字码反查枚举。
     *
     * @param code 数字码
     * @return 匹配的枚举；不合法返回 null
     */
    public static OaSyncStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OaSyncStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
