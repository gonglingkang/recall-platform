package com.recall.service.todo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recall.common.api.PageResp;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.todo.TodoMapper;
import com.recall.dto.todo.TodoCreateReq;
import com.recall.dto.todo.TodoListReq;
import com.recall.dto.todo.TodoStatusReq;
import com.recall.dto.todo.TodoUpdateReq;
import com.recall.entity.todo.Todo;
import com.recall.enums.Priority;
import com.recall.enums.TodoStatus;
import com.recall.service.category.CategoryService;
import com.recall.service.daily.DailyReportItemTodoService;
import com.recall.service.todo.TodoService;
import com.recall.vo.category.CategoryVO;
import com.recall.vo.todo.TodoMonthVO;
import com.recall.vo.todo.TodoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办 Service 实现。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoMapper todoMapper;
    /**
     * @Lazy 打破与 CategoryServiceImpl 的循环依赖
     */
    @Lazy
    private final CategoryService categoryService;
    private final DailyReportItemTodoService dailyReportItemTodoService;

    // ===================== 查询 =====================

    @Override
    public List<TodoVO> listToday() {
        Long userId = UserContextHolder.requireUserId();
        LocalDate today = LocalDate.now();
        // 今日待办 = 今日创建的全部 ∪ 历史待办中今日仍需处理的（pending 或今日完成）
        //  - 历史未完成：createdAt < 今日 且 status = pending（提醒用户）
        //  - 历史今日完成：createdAt < 今日 且 doneAt 落在今日（否则用户看不到当天完成的历史任务）
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .and(w -> w
                        // 今日创建的全部待办
                        .ge(Todo::getCreatedAt, today.atStartOfDay())
                        .lt(Todo::getCreatedAt, today.plusDays(1).atStartOfDay())
                        // 或：历史待办中今日仍需处理的
                        .or(sub -> sub
                                .lt(Todo::getCreatedAt, today.atStartOfDay())
                                .and(s -> s
                                        .eq(Todo::getStatus, TodoStatus.PENDING.getValue())
                                        .or(d -> d
                                                .ge(Todo::getDoneAt, today.atStartOfDay())
                                                .lt(Todo::getDoneAt, today.plusDays(1).atStartOfDay())))));
        applyDefaultSort(wrapper);
        return todoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public TodoMonthVO monthCalendar(String month) {
        // 月份格式校验 YYYY-MM
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "月份格式应为 YYYY-MM");
        }
        Long userId = UserContextHolder.requireUserId();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);

        // 查询生命周期区间与当月有交集的待办：
        //   createdAt < 当月末（创建于当月结束前）
        //   且 (doneAt 为空 即仍 pending，或 doneAt >= 当月初 即当月或之后才完成)
        // 这样上月创建、下月才完成的跨月待办也会被查到，覆盖当月每一天。
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .in(Todo::getStatus, TodoStatus.PENDING.getValue(), TodoStatus.DONE.getValue())
                .lt(Todo::getCreatedAt, end.atStartOfDay())
                .and(w -> w.isNull(Todo::getDoneAt)
                        .or().ge(Todo::getDoneAt, start.atStartOfDay()));
        List<Todo> todos = todoMapper.selectList(wrapper);

        // 分类展示顺序（大分类 → 其下子分类的扁平序号），供每天列表内排序使用
        Map<Long, Integer> categoryOrder = buildCategoryOrder();
        Comparator<TodoVO> dayOrder = dayDisplayOrder(categoryOrder);

        // 按天分组：对当月每一天 D，纳入生命周期区间覆盖 D 的待办。
        // 区间 = [createdAt日, doneAt 为空 ? 今天 : doneAt日]：
        //   - 已完成待办 doneAt 一定 ≤ 今天，区间为 [createdAt日, doneAt日]
        //   - pending 待办仍在处理，区间为 [createdAt日, 今天]；未来日期尚未到来，待办不可能在那天“被处理”，故不覆盖
        // 因此 D > 今天时该天列表恒为空（未来日无数据）。
        LocalDate today = LocalDate.now();
        List<TodoMonthVO.DayGroup> days = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            List<TodoVO> dayTodos = new ArrayList<>();
            if (!d.isAfter(today)) {
                for (Todo todo : todos) {
                    if (todo.getCreatedAt() == null) {
                        continue;
                    }
                    LocalDate createdDate = todo.getCreatedAt().toLocalDate();
                    if (createdDate.isAfter(d)) {
                        // 当天尚未创建
                        continue;
                    }
                    if (todo.getDoneAt() != null) {
                        LocalDate doneDate = todo.getDoneAt().toLocalDate();
                        if (doneDate.isBefore(d)) {
                            // 当天之前已完成，生命周期区间不再覆盖
                            continue;
                        }
                    }
                    // pending 待办区间上界为今天，已在 d <= today 保证下；done 待办上界为 doneDate，上面已校验
                    dayTodos.add(toVOForDay(todo, d));
                }
            }
            // 排序须在快照之后：同一条待办在完成日之前是 pending、完成当天是 done，排序键取当天快照状态
            dayTodos.sort(dayOrder);
            days.add(TodoMonthVO.DayGroup.builder()
                    .date(d)
                    .todos(dayTodos)
                    .build());
        }

        return TodoMonthVO.builder()
                .month(month)
                .days(days)
                .build();
    }

    @Override
    public PageResp<TodoVO> page(TodoListReq req) {
        Long userId = UserContextHolder.requireUserId();
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId);
        // 完成状态：不传查全部（pending+done），传了按传入值筛
        if (req.getStatus() != null) {
            wrapper.eq(Todo::getStatus, req.getStatus().getValue());
        } else {
            wrapper.in(Todo::getStatus, TodoStatus.PENDING.getValue(), TodoStatus.DONE.getValue());
        }
        // 创建时间范围过滤（基于 createdAt 自然日闭区间）：startDate 含当日 00:00，endDate 含当日 23:59:59.999999999
        if (req.getStartDate() != null) {
            wrapper.ge(Todo::getCreatedAt, req.getStartDate().atStartOfDay());
        }
        if (req.getEndDate() != null) {
            wrapper.lt(Todo::getCreatedAt, req.getEndDate().plusDays(1).atStartOfDay());
        }
        if (req.getCategoryId() != null) {
            // 按分类筛选时联动子分类：传入大分类 → 查自身 + 所有子分类下的待办
            List<Long> categoryIds = categoryService.listCategoryAndSubIds(req.getCategoryId());
            wrapper.in(Todo::getCategoryId, categoryIds);
        }
        if (req.getPriority() != null) {
            wrapper.eq(Todo::getPriority, req.getPriority().getValue());
        }
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kw = req.getKeyword().trim();
            wrapper.and(w -> w.like(Todo::getTitle, kw).or().like(Todo::getNote, kw));
        }
        applyDefaultSort(wrapper);
        return pageAndConvert(wrapper, req.getPageNum(), req.getPageSize());
    }

    @Override
    public TodoVO getById(Long id) {
        return toVO(loadOwned(id));
    }

    // ===================== 写操作 =====================

    @Override
    public TodoVO create(TodoCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        // 当天同名待办重复校验（按 createdAt 自然日）
        checkTitleDuplicateToday(userId, req.getTitle(), null);

        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle(req.getTitle());
        todo.setNote(req.getNote());
        todo.setCategoryId(req.getCategoryId());
        todo.setPriority(req.getPriority().getValue());
        todo.setStatus(TodoStatus.PENDING.getValue());
        todoMapper.insert(todo);
        return toVO(todo);
    }

    @Override
    public TodoVO update(Long id, TodoUpdateReq req) {
        Todo todo = loadOwned(id);
        Long userId = UserContextHolder.requireUserId();
        // 标题变更时做当天同名重复校验（排除自身）
        if (!req.getTitle().equals(todo.getTitle())) {
            checkTitleDuplicateToday(userId, req.getTitle(), id);
        }
        todo.setTitle(req.getTitle());
        todo.setNote(req.getNote());
        todo.setCategoryId(req.getCategoryId());
        // 优先级为空默认中
        todo.setPriority(req.getPriority() != null ? req.getPriority().getValue() : Priority.MEDIUM.getValue());
        todoMapper.updateById(todo);
        return toVO(todo);
    }

    @Override
    public TodoVO changeStatus(Long id, TodoStatusReq req) {
        Todo todo = loadOwned(id);
        // 状态机：pending ⇄ done
        TodoStatus target = req.getStatus();
        LambdaUpdateWrapper<Todo> uw = new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, id)
                .eq(Todo::getUserId, UserContextHolder.requireUserId());
        if (target == TodoStatus.DONE) {
            if (TodoStatus.DONE.getValue().equals(todo.getStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "待办已完成");
            }
            // 完成：status=done，doneAt=当前时间
            LocalDateTime now = LocalDateTime.now();
            todo.setStatus(TodoStatus.DONE.getValue());
            todo.setDoneAt(now);
            uw.set(Todo::getStatus, TodoStatus.DONE.getValue())
              .set(Todo::getDoneAt, now);
        } else if (target == TodoStatus.PENDING) {
            // 撤销完成：status=pending，doneAt 清空
            // 用 LambdaUpdateWrapper.set 显式置 null，绕开 updateById 默认 NOT_NULL 策略，保证 doneAt=null 落库
            todo.setStatus(TodoStatus.PENDING.getValue());
            todo.setDoneAt(null);
            uw.set(Todo::getStatus, TodoStatus.PENDING.getValue())
              .set(Todo::getDoneAt, null);
        } else {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "状态变更仅支持 pending/done");
        }
        todoMapper.update(null, uw);
        return toVO(todo);
    }

    /**
     * 删除待办（物理删除，不可恢复）。
     * <p>连带清理日报对该待办的关联记录，避免悬空关联（多条写 -> 事务）。
     *
     * @param id 待办 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        loadOwned(id);
        // 级联清理日报关联（走 DailyReportItemTodoService 接口，不直接碰其 Mapper）
        dailyReportItemTodoService.deleteByTodoId(id);
        todoMapper.deleteById(id);
    }

    // ===================== 分类联动（供 CategoryService 调用） =====================

    @Override
    public int migrateByCategory(Long fromCategoryId, Long toCategoryId) {
        // 把源分类下的待办迁移到目标分类（如子分类删除时迁到父大分类）
        Long userId = UserContextHolder.requireUserId();
        return todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .eq(Todo::getCategoryId, fromCategoryId)
                .set(Todo::getCategoryId, toCategoryId));
    }

    @Override
    public boolean existsByCategory(Long categoryId) {
        // 判断该分类下是否有待办
        Long userId = UserContextHolder.requireUserId();
        return todoMapper.exists(new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .eq(Todo::getCategoryId, categoryId));
    }

    @Override
    public long countPending(Long categoryId) {
        Long userId = UserContextHolder.requireUserId();
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .eq(Todo::getStatus, TodoStatus.PENDING.getValue());
        if (categoryId == null) {
            // 今日未完成：今日创建的 pending + 历史创建仍 pending（pending 的 doneAt 恒空，
            // 故“历史今日完成”天然不在内）。等价于 createdAt < 明日 0 点 的全部 pending。
            LocalDate today = LocalDate.now();
            wrapper.lt(Todo::getCreatedAt, today.plusDays(1).atStartOfDay());
        } else {
            // 父分类下未完成：含其所有子分类，不限创建时间
            List<Long> categoryIds = categoryService.listCategoryAndSubIds(categoryId);
            wrapper.in(Todo::getCategoryId, categoryIds);
        }
        Long count = todoMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * 查询指定日期当天的待办（按当天真实状态返回）。
     * <p>
     * 纳入生命周期区间覆盖当天的待办：创建于当天及之前（createdAt < 次日0点），
     * 且尚未完成（doneAt 为空）或当天及之后才完成（doneAt >= 当天0点）。
     * 未来日期返回空列表。
     * <p>
     * 返回的 status/doneAt 为「当天状态快照」：doneAt ≤ date 当天 -> done，
     * 否则（doneAt 为空或晚于 date）-> pending 且 doneAt 返回 null。
     */
    @Override
    public List<TodoVO> listByDate(LocalDate date) {
        // 未来日期无可见待办（与 monthCalendar 单日分支一致）
        if (date.isAfter(LocalDate.now())) {
            return Collections.emptyList();
        }
        Long userId = UserContextHolder.requireUserId();
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                // 创建于当天及之前：createdAt < 次日0点
                .lt(Todo::getCreatedAt, date.plusDays(1).atStartOfDay())
                // 仍 pending（doneAt 为空）或当天及之后才完成
                .and(w -> w.isNull(Todo::getDoneAt)
                        .or().ge(Todo::getDoneAt, date.atStartOfDay()));
        applyDefaultSort(wrapper);
        return todoMapper.selectList(wrapper).stream().map(t -> toVOOnDate(t, date)).toList();
    }

    /**
     * 将待办转为「指定日期当天状态快照」VO。
     * <p>
     * doneAt ≤ date 当天 -> status=done，doneAt 保留真实值；
     * 否则（doneAt 为空或晚于 date）-> status=pending，doneAt 返回 null。
     *
     * @param todo 待办实体（当前值）
     * @param date 目标日期
     * @return 当天状态快照 VO
     */
    private TodoVO toVOOnDate(Todo todo, LocalDate date) {
        boolean doneOnDate = todo.getDoneAt() != null
                && !todo.getDoneAt().toLocalDate().isAfter(date);
        return TodoVO.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .note(todo.getNote())
                .categoryId(todo.getCategoryId())
                .priority(todo.getPriority())
                .status(doneOnDate ? TodoStatus.DONE.getValue() : TodoStatus.PENDING.getValue())
                .doneAt(doneOnDate ? todo.getDoneAt() : null)
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .build();
    }

    /**
     * 按 id 批量查询待办实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     */
    @Override
    public List<Todo> listByIds(List<Long> ids, boolean checkOwnership) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Todo> todos = todoMapper.selectList(new LambdaQueryWrapper<Todo>()
                .in(Todo::getId, ids));
        if (!checkOwnership) {
            return todos;
        }
        // 越权统一 404：数量不符直接判为存在不属于当前用户的 id（不暴露具体哪个）
        if (todos.size() != ids.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long userId = UserContextHolder.requireUserId();
        for (Todo todo : todos) {
            if (!userId.equals(todo.getUserId())) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
        }
        return todos;
    }

    // ===================== 辅助 =====================

    /**
     * 按当前用户加载待办，不存在或越权抛 404
     */
    private Todo loadOwned(Long id) {
        Long userId = UserContextHolder.requireUserId();
        Todo todo = todoMapper.selectById(id);
        if (todo == null || !userId.equals(todo.getUserId())) {
            // 禁止仅凭资源 ID 访问；越权统一返回 404，不暴露资源存在性
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return todo;
    }

    /**
     * 当天同名待办重复校验（按 createdAt 自然日）。
     *
     * @param userId    当前用户
     * @param title     待校验标题
     * @param excludeId 排除的待办 ID（更新时传当前待办 ID，创建时传 null）
     */
    private void checkTitleDuplicateToday(Long userId, String title, Long excludeId) {
        LocalDate today = LocalDate.now();
        Long count = todoMapper.selectCount(new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .eq(Todo::getTitle, title)
                .ne(excludeId != null, Todo::getId, excludeId)
                .ge(Todo::getCreatedAt, today.atStartOfDay())
                .lt(Todo::getCreatedAt, today.plusDays(1).atStartOfDay()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.TODO_TITLE_DUPLICATED);
        }
    }

    /**
     * 默认排序下沉 SQL：状态升序(pending"0"在前) > 创建时间降序(新在前) > 优先级降序(high"2">medium"1">low"0")。
     * <p>
     * orderBy 声明顺序即排序优先级，必须与上述语义一致。
     */
    private void applyDefaultSort(LambdaQueryWrapper<Todo> wrapper) {
        wrapper.orderByAsc(Todo::getStatus)
                .orderByDesc(Todo::getCreatedAt)
                .orderByDesc(Todo::getPriority);
    }

    /**
     * 分页查询并转 VO
     */
    private PageResp<TodoVO> pageAndConvert(LambdaQueryWrapper<Todo> wrapper, int pageNum, int pageSize) {
        IPage<Todo> page = todoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<TodoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResp.<TodoVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .pages(page.getPages())
                .build();
    }

    private TodoVO toVO(Todo todo) {
        return TodoVO.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .note(todo.getNote())
                .categoryId(todo.getCategoryId())
                .priority(todo.getPriority())
                .status(todo.getStatus())
                .doneAt(todo.getDoneAt())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .build();
    }

    /**
     * 构造待办在指定日期 D 的历史状态快照（供日历视图按天展示）。
     * <p>
     * 待办当前已 done，但 doneAt 落在 D 之后（D 当天尚未完成）时，当天应展示为 pending、doneAt=null。
     * 否则按当前实体返回（pending 待办天然 pending；done 且 doneAt≤D 时返回 done + 真实 doneAt）。
     *
     * @param todo 待办实体（当前实时状态）
     * @param day  要展示的日期
     * @return 当天历史状态快照 VO
     */
    private TodoVO toVOForDay(Todo todo, LocalDate day) {
        if (todo.getDoneAt() != null && todo.getDoneAt().toLocalDate().isAfter(day)) {
            // 当天尚未完成：返回 pending 快照
            return TodoVO.builder()
                    .id(todo.getId())
                    .title(todo.getTitle())
                    .note(todo.getNote())
                    .categoryId(todo.getCategoryId())
                    .priority(todo.getPriority())
                    .status(TodoStatus.PENDING.getValue())
                    .doneAt(null)
                    .createdAt(todo.getCreatedAt())
                    .updatedAt(todo.getUpdatedAt())
                    .build();
        }
        return toVO(todo);
    }

    /**
     * 构建分类的展示顺序表：categoryId → 扁平序号。
     * <p>
     * 按「大分类 → 该大分类下的子分类」深度优先遍历分类树依次编号，因此大分类自身的序号必然小于其子分类，
     * 与前端分组渲染「先渲染挂在大分类下的待办，再渲染各子分类」的顺序一致。
     * <p>
     * 取树的遍历下标而非 sortOrder 原值：sortOrder 可空且可重复，下标唯一且已包含
     * {@code sortOrder asc, id asc} 的排序结果，能保证与前端分组顺序严格一致。
     *
     * @return categoryId → 序号；未出现在树中的分类（如已删除）不在表内
     */
    private Map<Long, Integer> buildCategoryOrder() {
        Map<Long, Integer> order = new HashMap<>();
        int seq = 0;
        for (CategoryVO root : categoryService.listTree()) {
            order.put(root.getId(), seq++);
            if (root.getSubcategories() == null) {
                continue;
            }
            for (CategoryVO sub : root.getSubcategories()) {
                order.put(sub.getId(), seq++);
            }
        }
        return order;
    }

    /**
     * 日历中每天列表的展示顺序：已完成在前 → 分类顺序 → 优先级高在前 → 创建时间早在前。
     * <p>
     * 末位以 createdAt、id 兜底，保证同键待办的顺序稳定（查询本身不带 ORDER BY，DB 返回顺序不保证）。
     *
     * @param categoryOrder 分类展示顺序表，见 {@link #buildCategoryOrder()}
     */
    private Comparator<TodoVO> dayDisplayOrder(Map<Long, Integer> categoryOrder) {
        return Comparator
                .comparingInt((TodoVO v) -> TodoStatus.DONE.getValue().equals(v.getStatus()) ? 0 : 1)
                .thenComparingInt(v -> categoryRank(v.getCategoryId(), categoryOrder))
                .thenComparing(Comparator.comparingInt((TodoVO v) -> priorityWeight(v.getPriority())).reversed())
                .thenComparing(TodoVO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TodoVO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /** 未分类、以及分类已被删除的待办统一排在最后，与前端把「未分类」分组追加到末尾一致 */
    private int categoryRank(Long categoryId, Map<Long, Integer> categoryOrder) {
        if (categoryId == null) {
            return Integer.MAX_VALUE;
        }
        return categoryOrder.getOrDefault(categoryId, Integer.MAX_VALUE);
    }

    /** 优先级排序权重；priority 为空时按默认的「中」处理（{@link Priority#of} 对 null 返回 null） */
    private int priorityWeight(String priority) {
        Priority p = Priority.of(priority);
        return p == null ? Priority.MEDIUM.getWeight() : p.getWeight();
    }
}
