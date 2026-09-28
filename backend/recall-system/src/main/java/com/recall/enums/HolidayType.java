package com.recall.enums;

import lombok.Getter;

/**
 * 节假日类型。
 *
 * @author recall
 */
@Getter
public enum HolidayType {
    /** 法定节假日 */
    HOLIDAY(1, "节假日"),
    /** 补班日 */
    MAKEUP(2, "补班日");

    private final int code;
    private final String desc;

    HolidayType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
