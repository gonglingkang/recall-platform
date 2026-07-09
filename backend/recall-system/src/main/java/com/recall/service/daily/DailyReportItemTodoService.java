package com.recall.service.daily;

import com.recall.entity.daily.DailyReportItemTodo;

import java.util.List;
import java.util.Map;

/**
 * 日报项-待办关联 Service，管理 daily_report_item_todos 表的数据访问。
 * <p>
 * 其他 Service（如 DailyReportItemService）操作关联数据须经本接口，不直接注入
 * DailyReportItemTodoMapper。关联随日报项全量覆盖，无独立增删改接口。
 *
 * @author recall
 */
public interface DailyReportItemTodoService {

    /**
     * 查询指定日报项关联的待办 ID 列表（按 id 升序，即关联顺序）。
     *
     * @param itemId 日报项 ID
     * @return 待办 ID 列表
     */
    List<Long> listTodoIdsByItemId(Long itemId);

    /**
     * 批量查询多个日报项关联的待办 ID 列表（供日报列表批量加载用）。
     *
     * @param itemIds 日报项 ID 列表；为空返回空 map
     * @return itemId -> 待办 ID 列表
     */
    Map<Long, List<Long>> listTodoIdsByItemIds(List<Long> itemIds);

    /**
     * 保存指定日报项的关联：先删旧、再插新。
     * <p>
     * todoIds 为 null 或空时仅清空该日报项下全部关联。
     *
     * @param itemId  日报项 ID
     * @param todoIds 待办 ID 列表；null 或空表示清空
     */
    void saveByItemId(Long itemId, List<Long> todoIds);

    /**
     * 删除指定日报项的全部关联（日报项覆盖/删除时级联调用）。
     *
     * @param itemId 日报项 ID
     */
    void deleteByItemId(Long itemId);

    /**
     * 批量删除多个日报项的全部关联（删日报时清理用，避免 N 次调用）。
     *
     * @param itemIds 日报项 ID 列表；为空不执行
     */
    void deleteByItemIds(List<Long> itemIds);

    /**
     * 删除指定待办的全部日报关联（待办删除时级联调用，避免悬空关联）。
     *
     * @param todoId 待办 ID
     */
    void deleteByTodoId(Long todoId);

    /**
     * 按 id 列表查询关联实体（供 DailyReportService 组装 VO 用，不透传至 Controller/前端）。
     *
     * @param itemIds 日报项 ID 列表
     * @return 关联实体列表
     */
    List<DailyReportItemTodo> listByItemIds(List<Long> itemIds);
}
