package com.recall.enums;

import lombok.Getter;

/**
 * 请假类型。
 * <p>
 * 数字码：病假(1)、年假(2)、事假(3)、育儿假(4)。
 * 数据库与 API 均存/传数字码，文案由 desc 提供（VO 返回名称字段，前端不自行映射）。
 *
 * @author recall
 */
@Getter
public enum LeaveType {
    /** 病假 */
    SICK(1, "病假"),
    /** 年假 */
    ANNUAL(2, "年假"),
    /** 事假 */
    PERSONAL(3, "事假"),
    /** 育儿假 */
    CHILDCARE(4, "育儿假");

    private final int code;
    private final String desc;

    LeaveType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按数字码反查枚举。
     *
     * @param code 数字码
     * @return 匹配的枚举；不合法返回 null（由调用方决定报错方式）
     */
    public static LeaveType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (LeaveType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
