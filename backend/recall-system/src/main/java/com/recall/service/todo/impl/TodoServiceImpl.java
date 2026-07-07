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
import com.recall.service.todo.TodoService;
import com.recall.vo.todo.TodoMonthVO;
import com.recall.vo.todo.TodoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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

        // 单次查询：当月创建的全部 ∪ 当月完成的（doneAt 落在当月）
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .in(Todo::getStatus, TodoStatus.PENDING.getValue(), TodoStatus.DONE.getValue())
                .and(w -> w
                        .and(c -> c.ge(Todo::getCreatedAt, start.atStartOfDay()).lt(Todo::getCreatedAt, end.atStartOfDay()))
                        .or(d -> d.ge(Todo::getDoneAt, start.atStartOfDay()).lt(Todo::getDoneAt, end.atStartOfDay())));
        List<Todo> todos = todoMapper.selectList(wrapper);

        // 按天分组：created 按 createdAt 日期归组，completed 按 doneAt 日期归组
        Map<LocalDate, List<TodoVO>> createdByDay = new LinkedHashMap<>();
        Map<LocalDate, List<TodoVO>> completedByDay = new LinkedHashMap<>();
        for (Todo todo : todos) {
            if (todo.getCreatedAt() != null) {
                LocalDate createdDate = todo.getCreatedAt().toLocalDate();
                if (!createdDate.isBefore(start) && createdDate.isBefore(end)) {
                    createdByDay.computeIfAbsent(createdDate, k -> new ArrayList<>()).add(toVO(todo));
                }
            }
            if (todo.getDoneAt() != null) {
                LocalDate completedDate = todo.getDoneAt().toLocalDate();
                if (!completedDate.isBefore(start) && completedDate.isBefore(end)) {
                    completedByDay.computeIfAbsent(completedDate, k -> new ArrayList<>()).add(toVO(todo));
                }
            }
        }

        // 补全空日期：遍历月份每一天，无待办的也输出空 DayGroup
        List<TodoMonthVO.DayGroup> days = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            days.add(TodoMonthVO.DayGroup.builder()
                    .date(d)
                    .created(createdByDay.getOrDefault(d, List.of()))
                    .completed(completedByDay.getOrDefault(d, List.of()))
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

    @Override
    public void delete(Long id) {
        // 物理删除，不可恢复。单条写（deleteById），无需事务（见 backend-code-standards 事务规则）
        loadOwned(id);
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
}
