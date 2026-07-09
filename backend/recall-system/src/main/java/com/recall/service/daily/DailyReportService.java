package com.recall.service.daily;

import com.recall.dto.daily.DailyReportSaveReq;
import com.recall.vo.daily.DailyReportMonthVO;
import com.recall.vo.daily.DailyReportVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 日报 Service（v1.0）。
 * <p>
 * 核心约束：
 * <ul>
 *   <li>所有查询/操作强制按当前用户 userId 过滤（数据隔离）。</li>
 *   <li>一天一份日报，report_date 不可为未来（后端校验，抛 4601）。</li>
 *   <li>编辑采用全量覆盖：保存日报时整体提交工作内容，后端先删后插。</li>
 *   <li>关联待办须为当天相关的待办（生命周期覆盖 report_date），否则抛 4602。</li>
 * </ul>
 * <p>
 * 日报项与关联数据经 {@link DailyReportItemService}、{@link DailyReportItemTodoService} 管理，
 * 待办数据与校验经 {@link com.recall.service.todo.TodoService}，本 Service 不直接注入这些 Mapper。
 *
 * @author recall
 */
public interface DailyReportService {

    /**
     * 查询某月日报列表（只返回有日报的天，按日期升序，每个日报内嵌其下工作内容项与关联待办）。
     *
     * @param month 月份 YYYY-MM
     * @return 月度日报视图
     */
    DailyReportMonthVO monthList(String month);

    /**
     * 获取指定日期的日报详情。
     *
     * @param date 日期 YYYY-MM-DD
     * @return 日报详情；不存在抛 404
     */
    DailyReportVO getByDate(LocalDate date);

    /**
     * 保存日报（全量覆盖）：不存在则建，存在则覆盖其下工作内容项与关联。
     * <p>
     * 校验：date 不可为未来；关联待办须为当天相关。
     *
     * @param date 日期 YYYY-MM-DD
     * @param req  保存请求（含全部工作内容项）
     * @return 保存后的日报详情
     */
    DailyReportVO save(LocalDate date, DailyReportSaveReq req);

    /**
     * 删除指定日期的日报（物理删除，连带删工作内容项与关联）。
     *
     * @param date 日期 YYYY-MM-DD
     */
    void delete(LocalDate date);
}
