package com.recall.service.todo;

import com.recall.common.api.PageResp;
import com.recall.dto.todo.TodoCreateReq;
import com.recall.dto.todo.TodoListReq;
import com.recall.dto.todo.TodoStatusReq;
import com.recall.dto.todo.TodoUpdateReq;
import com.recall.vo.todo.TodoMonthVO;
import com.recall.vo.todo.TodoVO;

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
     * 查询指定月份的待办日历：按天分组，每天含当天创建的全部 + 当天完成的历史待办。
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
}
