package com.recall.service.daily;

import com.recall.dto.daily.DailyReportItemReq;
import com.recall.entity.daily.DailyReportItem;

import java.util.List;
import java.util.Map;

/**
 * 日报工作内容项 Service，管理 daily_report_items 表的数据访问。
 * <p>
 * 其他 Service（如 DailyReportService）操作日报项数据须经本接口，不直接注入
 * DailyReportItemMapper。日报项随日报全量覆盖，无独立增删改接口。
 *
 * @author recall
 */
public interface DailyReportItemService {

    /**
     * 查询指定日报下的工作内容项实体（按 id 升序，供 DailyReportService 组装 VO 用，不透传至 Controller/前端）。
     *
     * @param reportId 日报 ID
     * @return 日报项实体列表
     */
    List<DailyReportItem> listByReportId(Long reportId);

    /**
     * 批量查询多个日报下的工作内容项实体（按 id 升序，供月度列表批量加载用）。
     *
     * @param reportIds 日报 ID 列表；为空返回空 map
     * @return reportId -> 日报项实体列表
     */
    Map<Long, List<DailyReportItem>> listByReportIds(List<Long> reportIds);

    /**
     * 全量覆盖指定日报的工作内容项：先删旧（含关联）、再插新（含关联）。
     * <p>
     * items 为空列表时清空该日报下全部日报项及其关联。删 + 插为多条写，加事务保证原子性，
     * 被调用方上层事务覆盖（REQUIRED 传播）。
     *
     * @param reportId 日报 ID
     * @param items    工作内容项请求列表；空列表表示清空
     * @return 插入后的日报项实体列表（含 id，供调用方组装 VO）
     */
    List<DailyReportItem> replaceByReportId(Long reportId, List<DailyReportItemReq> items);

    /**
     * 删除指定日报下的全部工作内容项及其关联（删日报时级联调用）。
     *
     * @param reportId 日报 ID
     */
    void deleteByReportId(Long reportId);
}

