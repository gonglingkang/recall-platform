package com.recall.service.todo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.todo.TodoMapper;
import com.recall.dao.todo.TodoTemplateMapper;
import com.recall.dto.todo.TodoTemplateSaveReq;
import com.recall.entity.todo.Todo;
import com.recall.entity.todo.TodoTemplate;
import com.recall.enums.TodoStatus;
import com.recall.service.todo.TodoTemplateService;
import com.recall.vo.todo.TodoTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 周期待办模板 Service 实现。
 * <p>
 * 持有 TodoTemplateMapper 与 TodoMapper（生成待办/门控查询）；
 * 周期命中采用逐日扫描（上限 366 天），一个月 30/31 号在小月自然跳过。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoTemplateServiceImpl implements TodoTemplateService {

    /** 周期日扫描上限：一年内必命中（WEEKLY/MONTHLY 合法值），否则视为配置无法命中 */
    private static final int SCAN_LIMIT_DAYS = 366;

    /** 未配置时间点时的默认生成时刻 */
    private static final String DEFAULT_TIME = "09:00";

    private static final Set<String> RECURRENCE_TYPES = Set.of("DAILY", "WEEKLY", "MONTHLY");

    private final TodoTemplateMapper todoTemplateMapper;
    private final TodoMapper todoMapper;

    @Override
    public List<TodoTemplateVO> list() {
        List<TodoTemplate> templates = todoTemplateMapper.selectList(new LambdaQueryWrapper<TodoTemplate>()
                .eq(TodoTemplate::getUserId, UserContextHolder.requireUserId())
                .orderByAsc(TodoTemplate::getId));
        if (templates.isEmpty()) {
            return List.of();
        }
        // 批量取最近生成待办的状态（管理页展示门控进展）
        List<Long> lastTodoIds = templates.stream()
                .map(TodoTemplate::getLastTodoId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Todo> todoById = lastTodoIds.isEmpty() ? Map.of()
                : todoMapper.selectList(new LambdaQueryWrapper<Todo>()
                        .select(Todo::getId, Todo::getTitle, Todo::getStatus)
                        .in(Todo::getId, lastTodoIds))
                .stream().collect(Collectors.toMap(Todo::getId, Function.identity()));
        return templates.stream()
                .map(t -> toVO(t, t.getLastTodoId() == null ? null : todoById.get(t.getLastTodoId())))
                .toList();
    }

    @Override
    public TodoTemplateVO create(TodoTemplateSaveReq req) {
        validateRecurrence(req);
        TodoTemplate template = new TodoTemplate();
        apply(template, req);
        template.setUserId(UserContextHolder.requireUserId());
        template.setNextDate(firstOccurrenceAfter(LocalDate.now(), req));
        if (template.getNextDate() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "该周期配置在一年内无法命中，请检查");
        }
        template.setLastTodoId(null);
        todoTemplateMapper.insert(template);
        return toVO(template, null);
    }

    @Override
    public TodoTemplateVO update(Long id, TodoTemplateSaveReq req) {
        TodoTemplate template = loadOwned(id);
        validateRecurrence(req);
        apply(template, req);
        // 周期可能被改：next_date 重算为今天之后的第一个周期日（门控照常生效，不影响未完成的在途待办）
        template.setNextDate(firstOccurrenceAfter(LocalDate.now(), req));
        if (template.getNextDate() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "该周期配置在一年内无法命中，请检查");
        }
        todoTemplateMapper.updateById(template);
        return toVO(template, currentLastTodo(template));
    }

    @Override
    public void delete(Long id) {
        loadOwned(id);
        todoTemplateMapper.deleteById(id);
    }

    // ===================== 定时生成（供 Job 调用） =====================

    @Override
    public int generateDueTodos() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<TodoTemplate> templates = todoTemplateMapper.selectList(new LambdaQueryWrapper<TodoTemplate>()
                .eq(TodoTemplate::getEnabled, 1));
        int generated = 0;
        for (TodoTemplate template : templates) {
            // 完成门控：最近生成的待办仍待处理 → 不生成（待办已删视为已处理，门控放开）
            if (template.getLastTodoId() != null) {
                Todo last = todoMapper.selectById(template.getLastTodoId());
                if (last != null && TodoStatus.PENDING.getValue().equals(last.getStatus())) {
                    continue;
                }
            }
            // 到期判定 = next_date + 配置时间点 已到（错过几天后补跑同样命中）
            if (template.getNextDate() == null) {
                continue;
            }
            LocalDateTime dueAt = LocalDateTime.of(template.getNextDate(), parseTime(template.getRecurrenceTime()));
            if (now.isBefore(dueAt)) {
                continue;
            }
            Todo todo = new Todo();
            todo.setUserId(template.getUserId());
            todo.setTitle(template.getTitle());
            todo.setNote(template.getNote());
            todo.setCategoryId(template.getCategoryId());
            todo.setPriority(template.getPriority());
            todo.setStatus(TodoStatus.PENDING.getValue());
            todoMapper.insert(todo);
            generated++;
            // 推进到今天之后的下一个周期日（错过的日期折叠为今天生成，不逐日补）
            template.setLastTodoId(todo.getId());
            template.setNextDate(nextOccurrenceAfter(template, today));
            todoTemplateMapper.updateById(template);
            log.info("周期待办生成: templateId={}, todoId={}, title={}", template.getId(), todo.getId(), todo.getTitle());
        }
        return generated;
    }

    // ===================== 辅助 =====================

    /** 校验周期类型与取值；DAILY 归一为 * */
    private void validateRecurrence(TodoTemplateSaveReq req) {
        if (!RECURRENCE_TYPES.contains(req.getRecurrenceType())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "周期类型取值非法");
        }
        if ("DAILY".equals(req.getRecurrenceType())) {
            req.setRecurrenceValue("*");
            return;
        }
        String value = req.getRecurrenceValue() == null ? "" : req.getRecurrenceValue().trim();
        if (value.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "周期值不能为空");
        }
        int max = "WEEKLY".equals(req.getRecurrenceType()) ? 7 : 31;
        try {
            Set<Integer> days = Arrays.stream(value.split("[,，]"))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Integer::parseInt).collect(Collectors.toSet());
            boolean allValid = !days.isEmpty() && days.stream().allMatch(d -> d >= 1 && d <= max);
            if (!allValid) {
                throw new IllegalArgumentException();
            }
            // 归一存储（去重后按数字升序）
            req.setRecurrenceValue(days.stream().sorted()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "周期值格式非法");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED,
                    ("WEEKLY".equals(req.getRecurrenceType()) ? "周几取值须为1-7" : "日期取值须为1-31"));
        }
    }

    private void apply(TodoTemplate template, TodoTemplateSaveReq req) {
        template.setTitle(req.getTitle().trim());
        template.setNote(req.getNote() == null || req.getNote().isBlank() ? null : req.getNote().trim());
        template.setCategoryId(req.getCategoryId());
        template.setPriority(req.getPriority());
        template.setRecurrenceType(req.getRecurrenceType());
        template.setRecurrenceValue(req.getRecurrenceValue());
        template.setRecurrenceTime(req.getRecurrenceTime() == null || req.getRecurrenceTime().isBlank()
                ? DEFAULT_TIME : req.getRecurrenceTime().trim());
        template.setEnabled(req.getEnabled() == null || req.getEnabled() ? 1 : 0);
    }

    /** 今天之后的第一个周期日；一年内无法命中返回 null */
    private LocalDate firstOccurrenceAfter(LocalDate today, TodoTemplateSaveReq req) {
        return nextOccurrence(today, req.getRecurrenceType(), parseDays(req.getRecurrenceType(), req.getRecurrenceValue()));
    }

    /** today 之后的下一个周期日；一年内无法命中返回 null（调用方保持原 next_date，下月再试） */
    private LocalDate nextOccurrenceAfter(TodoTemplate template, LocalDate today) {
        LocalDate next = nextOccurrence(today, template.getRecurrenceType(), parseDays(template.getRecurrenceType(), template.getRecurrenceValue()));
        if (next == null) {
            log.warn("周期配置一年内无法命中，保持原 next_date: templateId={}, value={}", template.getId(), template.getRecurrenceValue());
        }
        return next;
    }

    private LocalDate nextOccurrence(LocalDate today, String type, Set<Integer> days) {
        // DAILY 先判定：其周期值为 *（days 为空集合），不能被空集合守卫拦截
        if ("DAILY".equals(type)) {
            return today.plusDays(1);
        }
        if (days.isEmpty()) {
            return null;
        }
        LocalDate d = today.plusDays(1);
        for (int i = 0; i < SCAN_LIMIT_DAYS; i++) {
            if ("WEEKLY".equals(type)) {
                // ISO：周一=1 … 周日=7
                if (days.contains(d.getDayOfWeek().getValue())) {
                    return d;
                }
            } else if (days.contains(d.getDayOfMonth())) {
                return d;
            }
            d = d.plusDays(1);
        }
        return null;
    }

    private Set<Integer> parseDays(String type, String value) {
        if (!"WEEKLY".equals(type) && !"MONTHLY".equals(type)) {
            return Set.of();
        }
        return Arrays.stream((value == null ? "" : value).split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).collect(Collectors.toSet());
    }

    /** 解析配置的生成时间点；非法回退默认值 */
    private java.time.LocalTime parseTime(String time) {
        try {
            return java.time.LocalTime.parse(time == null || time.isBlank() ? DEFAULT_TIME : time.trim());
        } catch (Exception e) {
            return java.time.LocalTime.parse(DEFAULT_TIME);
        }
    }

    private TodoTemplate loadOwned(Long id) {
        TodoTemplate template = todoTemplateMapper.selectById(id);
        if (template == null || !UserContextHolder.requireUserId().equals(template.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "周期待办模板不存在或无权访问");
        }
        return template;
    }

    private Todo currentLastTodo(TodoTemplate template) {
        if (template.getLastTodoId() == null) {
            return null;
        }
        return todoMapper.selectById(template.getLastTodoId());
    }

    private TodoTemplateVO toVO(TodoTemplate template, Todo lastTodo) {
        return TodoTemplateVO.builder()
                .id(template.getId())
                .title(template.getTitle())
                .note(template.getNote())
                .categoryId(template.getCategoryId())
                .priority(template.getPriority())
                .recurrenceType(template.getRecurrenceType())
                .recurrenceValue(template.getRecurrenceValue())
                .recurrenceTime(template.getRecurrenceTime())
                .enabled(template.getEnabled() != null && template.getEnabled() == 1)
                .nextDate(template.getNextDate())
                .lastTodoId(template.getLastTodoId())
                .lastTodoStatus(lastTodo == null ? null : lastTodo.getStatus())
                .build();
    }

    private TodoTemplateVO toVO(TodoTemplate template) {
        return toVO(template, null);
    }
}
