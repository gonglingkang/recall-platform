package com.recall.enums;

import lombok.Getter;

/**
 * 请假时段。
 * <p>
 * 数字码：全天(1)、上午(2)、下午(3)。仅支持全天或半天（上午/下午）。
 * 数据库与 API 均存/传数字码，文案由 desc 提供（VO 返回名称字段，前端不自行映射）。
 *
 * @author recall
 */
@Getter
public enum LeavePeriod {
    /** 全天 */
    FULL_DAY(1, "全天"),
    /** 上午（半天） */
    AM(2, "上午"),
    /** 下午（半天） */
    PM(3, "下午");

    private final int code;
    private final String desc;

    LeavePeriod(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按数字码反查枚举。
     *
     * @param code 数字码
     * @return 匹配的枚举；不合法返回 null（由调用方决定报错方式）
     */
    public static LeavePeriod of(Integer code) {
        if (code == null) {
            return null;
        }
        for (LeavePeriod p : values()) {
            if (p.code == code) {
                return p;
            }
        }
        return null;
    }
}
