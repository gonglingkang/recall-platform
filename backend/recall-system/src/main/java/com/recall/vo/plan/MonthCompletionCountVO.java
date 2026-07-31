package com.recall.vo.plan;

import lombok.Data;

/**
 * 按月聚合的完成计数投影。
 * <p>
 * 供 Service 间内部传递（月度趋势按月区间批量取数用），不对外暴露为接口返回值。
 * total 已由查询侧过滤掉不参与统计的条目（如已取消的 K、无需我介入的冲刺）。
 *
 * @author recall
 */
@Data
public class MonthCompletionCountVO {

    /** 月份 YYYY-MM */
    private String month;

    /** 参与统计的有效条目总数 */
    private long total;

    /** 其中已完成的条目数 */
    private long done;
}
