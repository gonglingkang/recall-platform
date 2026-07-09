package com.recall.service.todo;

import com.recall.common.api.PageResp;
import com.recall.dto.todo.TodoCreateReq;
import com.recall.dto.todo.TodoListReq;
import com.recall.dto.todo.TodoStatusReq;
import com.recall.dto.todo.TodoUpdateReq;
import com.recall.entity.todo.Todo;
import com.recall.vo.todo.TodoMonthVO;
import com.recall.vo.todo.TodoVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 待办 Service。
 * <p>
 * 核心约束：
 * <ul>
 *   <li>所有查询/操作强制按当前用户 userId 过滤。</li>
 *   <li>列表排序遵循：优先级降序(high>medium>low) > 状态(pending在前) > 创建时间升序。</li>
 *   <li>状态机：pending ⇄ done。</li>
 * </ul>
 *
 * @author recall
 */
public interface TodoService {

    /**
     * 查询今日待办（不分页）：今日创建的全部待办（含已完成）+ 历史未完成待办（提醒用户）。
     *
     * @return 今日待办视图列表
     */
    List<TodoVO> listToday();

    /**
     * 查询指定月份的待办日历：按天分组，每天含当天创建、当天完成，以及生命周期跨越当天仍处理中的待办
     * （如上月创建、下月才完成的跨月待办，在当月每一天都会出现）。
     *
     * @param month 月份 YYYY-MM
     * @return 月度日历视图
     */
    TodoMonthVO monthCalendar(String month);

    /**
     * 分页查询待办：支持按分类（联动子分类）、优先级、完成状态、关键词过滤，所有条件 AND 生效。
     *
     * @param req 查询请求（含过滤条件与分页参数）
     * @return 分页结果
     */
    PageResp<TodoVO> page(TodoListReq req);

    /**
     * 获取待办详情。
     *
     * @param id 待办 ID
     * @return 待办详情
     */
    TodoVO getById(Long id);

    /**
     * 创建待办。
     *
     * @param req 创建请求
     * @return 创建后的待办详情
     */
    TodoVO create(TodoCreateReq req);

    /**
     * 编辑待办。
     *
     * @param id  待办 ID
     * @param req 编辑请求
     * @return 编辑后的待办详情
     */
    TodoVO update(Long id, TodoUpdateReq req);

    /**
     * 完成/撤销待办（状态机 pending ⇄ done）。
     *
     * @param id  待办 ID
     * @param req 状态变更请求
     * @return 变更后的待办详情
     */
    TodoVO changeStatus(Long id, TodoStatusReq req);

    /**
     * 删除待办（物理删除，不可恢复）。
     *
     * @param id 待办 ID
     */
    void delete(Long id);

    /**
     * 按分类批量迁移待办（子分类删除时调用）。
     * <p>
     * 把当前用户源分类下的待办 categoryId 改为目标分类。
     *
     * @param fromCategoryId 源分类 ID
     * @param toCategoryId   目标分类 ID
     * @return 受影响待办条数
     */
    int migrateByCategory(Long fromCategoryId, Long toCategoryId);

    /**
     * 判断指定分类下是否有待办（删大分类时校验）。
     * <p>
     * 仅统计直接挂在该分类下的待办。
     *
     * @param categoryId 分类 ID
     * @return true 表示存在待办
     */
    boolean existsByCategory(Long categoryId);

    /**
     * 统计未完成（pending）待办数量。
     * <p>
     * 两种口径，由 categoryId 是否传入决定：
     * <ul>
     *   <li>categoryId 为空：今日待办中的未完成数 = 今日创建的 pending + 历史创建仍 pending（即 createdAt ≤ 今日结束 的全部 pending）。</li>
     *   <li>categoryId 非空：该父分类（含其所有子分类）下全部 pending 数，不限创建时间。</li>
     * </ul>
     *
     * @param categoryId 分类 ID，为 null 时统计今日未完成
     * @return 未完成待办数量
     */
    long countPending(Long categoryId);

    /**
     * 查询指定日期当天的待办（按当天真实状态返回）。
     * <p>
     * 纳入生命周期区间覆盖当天的待办：当天创建、当天完成、当天仍 pending、
     * 当天未完成但未来才完成（这种当天状态为 pending）。
     * <p>
     * 返回的 status/doneAt 为「当天状态快照」，非当前值：
     * <ul>
     *   <li>doneAt ≤ date 当天 -> status=done，doneAt 保留真实值</li>
     *   <li>doneAt 为空 或 doneAt 在 date 之后 -> status=pending，doneAt 返回 null</li>
     * </ul>
     * 未来日期返回空列表（未来无可见待办）。
     *
     * @param date 日期
     * @return 当天可见的待办列表（状态为当天快照）
     */
    List<TodoVO> listByDate(LocalDate date);

    /**
     * 按 id 批量查询待办实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     *
     * @param ids            待办 ID 列表
     * @param checkOwnership 是否校验归属；true 时任一不存在或不属于当前用户均抛 404（越权统一 404）
     * @return 待办实体列表
     */
    List<Todo> listByIds(List<Long> ids, boolean checkOwnership);
}
