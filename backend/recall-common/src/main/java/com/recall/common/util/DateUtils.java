package com.recall.common.util;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 日期工具类。
 *
 * @author recall
 */
public final class DateUtils {

    private DateUtils() {
    }

    /**
     * 解析 YYYY-MM 月份为该月最后一天（如 2026-07 → 2026-07-31，闰年 2 月自动处理）。
     *
     * @param month 月份字符串，格式 YYYY-MM
     * @return 该月最后一天
     */
    public static LocalDate endOfMonth(String month) {
        return YearMonth.parse(month).atEndOfMonth();
    }
}
