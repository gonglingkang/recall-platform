package com.recall.service.oa;

import com.recall.enums.OaSyncTriggerType;
import com.recall.vo.oa.OaSyncLogVO;

import java.time.LocalDate;
import java.util.List;

/**
 * OA 同步 Service。
 * <p>
 * 同步动作 = 用 Playwright 无头浏览器把整周日报/请假填进 OA「工时日填报」并「暂存待办」。
 * 单次 1-3 分钟，异步执行；同一用户同一周同时只允许一个任务（幂等）。
 * 动作边界：只暂存不提交，OA 端最终提交由用户人工完成。
 *
 * @author recall
 */
public interface OaSyncService {

    /**
     * 触发一次同步（异步执行）。
     * <p>
     * 校验：OA 配置完整（4801）、同周无进行中任务（4802）、仅本周/上周（4804）。
     *
     * @param userId   用户
     * @param anyDate  该周任一日期（用于定位平台周，周一为起始）
     * @param trigger  触发方式
     * @param onlyDate 仅同步该天的日报（自动同步场景）；null=整周重填（手动按钮兜底）
     * @return 同步日志视图（status=运行中）
     */
    OaSyncLogVO startSync(Long userId, LocalDate anyDate, OaSyncTriggerType trigger, LocalDate onlyDate);

    /**
     * 查询某周最新一条同步日志。
     *
     * @param userId  用户
     * @param anyDate 该周任一日期
     * @return 最新日志；从未同步返回 null
     */
    OaSyncLogVO getStatus(Long userId, LocalDate anyDate);

    /**
     * 查询当前用户最近的同步日志（按开始时间倒序）。
     *
     * @param userId 用户
     * @param limit  条数
     * @return 日志列表
     */
    List<OaSyncLogVO> listLogs(Long userId, int limit);
}
