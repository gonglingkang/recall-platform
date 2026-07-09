package com.recall.service.todo;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.recall.BaseTest;
import com.recall.common.api.PageResp;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.dao.category.CategoryMapper;
import com.recall.dao.todo.TodoMapper;
import com.recall.dto.todo.TodoCreateReq;
import com.recall.dto.todo.TodoListReq;
import com.recall.dto.todo.TodoStatusReq;
import com.recall.dto.todo.TodoUpdateReq;
import com.recall.entity.category.Category;
import com.recall.entity.todo.Todo;
import com.recall.enums.Priority;
import com.recall.enums.TodoStatus;
import com.recall.vo.todo.TodoMonthVO;
import com.recall.vo.todo.TodoVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 待办核心业务逻辑测试。
 *
 * @author recall
 */
@Transactional
class TodoServiceTest extends BaseTest {

    @Autowired
    private TodoService todoService;
    @Autowired
    private TodoMapper todoMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void create_shouldDefaultPendingAndMediumPriority() {
        Long userId = loginAsNewUser();
        TodoCreateReq req = newCreateReq("买牛奶");
        TodoVO vo = todoService.create(req);

        assertNotNull(vo.getId());
        assertEquals("0", vo.getStatus(), "新建默认 pending");
        assertEquals("1", vo.getPriority(), "新建默认 medium");
        assertEquals(userId, todoMapper.selectById(vo.getId()).getUserId());
    }

    @Test
    void changeStatus_doneThenUndo_shouldManageDoneAt() {
        loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("写报告"));

        // 标记完成
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        TodoVO doneVo = todoService.changeStatus(todo.getId(), done);
        assertEquals("1", doneVo.getStatus());
        assertNotNull(doneVo.getDoneAt());
        // DB 里 doneAt 应已落库
        assertNotNull(todoMapper.selectById(todo.getId()).getDoneAt(), "完成时 doneAt 应写入 DB");

        // 撤销完成 → doneAt 应清空
        TodoStatusReq undo = new TodoStatusReq();
        undo.setStatus(TodoStatus.PENDING);
        TodoVO undoVo = todoService.changeStatus(todo.getId(), undo);
        assertEquals("0", undoVo.getStatus());
        assertNull(undoVo.getDoneAt(), "撤销完成应清空 doneAt");
        // 关键：从 DB 重新查，确认 null 真正落库（updateById 默认 NOT_NULL 策略会跳过 null，曾导致此 bug）
        assertNull(todoMapper.selectById(todo.getId()).getDoneAt(), "撤销完成后 DB 中 doneAt 必须为 null");
    }

    @Test
    void changeStatus_alreadyDone_shouldThrowConflict() {
        loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("重复完成"));
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        todoService.changeStatus(todo.getId(), done);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> todoService.changeStatus(todo.getId(), done));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void list_withDateRangeAndCategory_shouldFilterBoth() {
        Long userId = loginAsNewUser();
        // 建一个分类
        Category cat = newCategory(userId, null, "工作");
        categoryMapper.insert(cat);
        // 两条今日待办：一条带分类，一条不带
        TodoCreateReq r1 = newCreateReq("带分类");
        r1.setCategoryId(cat.getId());
        todoService.create(r1);

        TodoCreateReq r2 = newCreateReq("不带分类");
        todoService.create(r2);

        // startDate=today + endDate=today + categoryId 同时生效，应只命中 r1
        TodoListReq req = new TodoListReq();
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now());
        req.setCategoryId(cat.getId());
        req.setPageSize(100); // 一次取全
        PageResp<TodoVO> page = todoService.page(req);

        assertEquals(1, page.getTotal());
        assertEquals("带分类", page.getRecords().get(0).getTitle());
    }

    @Test
    void list_withDateRange_shouldFilterByCreatedAt() {
        Long userId = loginAsNewUser();
        // 造三条待办，createdAt 分别改到前天/今天/后天
        TodoVO d1 = todoService.create(newCreateReq("前天"));
        TodoVO d2 = todoService.create(newCreateReq("今天"));
        TodoVO d3 = todoService.create(newCreateReq("后天"));
        LocalDateTime base = LocalDateTime.now();
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, d1.getId()).set(Todo::getCreatedAt, base.minusDays(1)));
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, d3.getId()).set(Todo::getCreatedAt, base.plusDays(1)));

        // 范围 [今天, 后天]：含今天、含后天，排除前天
        TodoListReq req = new TodoListReq();
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(1));
        req.setPageSize(100);
        List<Long> ids = todoService.page(req).getRecords().stream()
                .map(TodoVO::getId).toList();

        assertTrue(ids.contains(d2.getId()), "起始日应含");
        assertTrue(ids.contains(d3.getId()), "结束日应含");
        assertFalse(ids.contains(d1.getId()), "区间外应排除");
    }

    @Test
    void list_pagination_shouldReturnCorrectPage() {
        loginAsNewUser();
        // 造 25 条数据
        for (int i = 0; i < 25; i++) {
            todoService.create(newCreateReq("任务" + i));
        }

        // pageSize=10，验证三页
        TodoListReq req = new TodoListReq();
        req.setPageSize(10);

        req.setPageNum(1);
        PageResp<TodoVO> p1 = todoService.page(req);
        assertEquals(25, p1.getTotal());
        assertEquals(10, p1.getRecords().size());
        assertEquals(3, p1.getPages());

        req.setPageNum(2);
        PageResp<TodoVO> p2 = todoService.page(req);
        assertEquals(10, p2.getRecords().size());

        req.setPageNum(3);
        PageResp<TodoVO> p3 = todoService.page(req);
        assertEquals(5, p3.getRecords().size(), "第三页应剩 5 条");
    }

    @Test
    void list_sortOrderShouldFollowPriorityStatusCreated() {
        loginAsNewUser();
        // 高优、中优、低优各一条，统一 createdAt 让前两排序键(status/createdAt)无法区分，priority 才起作用
        TodoVO low = todoService.create(newCreateReq("低优"));
        TodoVO high = todoService.create(newCreateReq("高优"));
        TodoVO medium = todoService.create(newCreateReq("中优"));
        setPriority(low.getId(), Priority.LOW);
        setPriority(high.getId(), Priority.HIGH);
        setPriority(medium.getId(), Priority.MEDIUM);
        LocalDateTime fixedCreatedAt = LocalDateTime.now().withNano(0);
        setCreatedAt(low.getId(), fixedCreatedAt);
        setCreatedAt(high.getId(), fixedCreatedAt);
        setCreatedAt(medium.getId(), fixedCreatedAt);

        TodoListReq req = new TodoListReq();
        req.setPageSize(100);
        List<TodoVO> records = todoService.page(req).getRecords();
        // 排序：status ASC(同为pending) > createdAt DESC(相同) > priority DESC → 高优 > 中优 > 低优
        assertEquals("高优", records.get(0).getTitle(), "高优先级应排第一");
        assertEquals("中优", records.get(1).getTitle(), "其次中优先级");
        assertEquals("低优", records.get(2).getTitle());
    }

    @Test
    void list_doneShouldComeAfterPending() {
        loginAsNewUser();
        TodoVO a = todoService.create(newCreateReq("待处理A"));
        TodoVO b = todoService.create(newCreateReq("已完成B"));
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        todoService.changeStatus(b.getId(), done);

        // a 是 pending、b 是 done，优先级相同 → pending 应在前
        TodoListReq req = new TodoListReq();
        req.setPageSize(100);
        List<TodoVO> records = todoService.page(req).getRecords();
        int idxA = indexOf(records, a.getId());
        int idxB = indexOf(records, b.getId());
        assertTrue(idxA < idxB, "pending 应排在 done 之前");
    }

    @Test
    void list_sameStatus_shouldSortByCreatedAtDescThenPriority() {
        loginAsNewUser();
        // 三条均 pending；createdAt 各差 1 小时，priority 故意"反着"设，验证 createdAt DESC 优先于 priority
        TodoVO earlyHigh = todoService.create(newCreateReq("最早高优"));
        TodoVO midMedium = todoService.create(newCreateReq("居中中优"));
        TodoVO lateLow = todoService.create(newCreateReq("最新低优"));
        setPriority(earlyHigh.getId(), Priority.HIGH);
        setPriority(midMedium.getId(), Priority.MEDIUM);
        setPriority(lateLow.getId(), Priority.LOW);
        LocalDateTime base = LocalDateTime.now().withNano(0);
        setCreatedAt(earlyHigh.getId(), base.minusHours(2));
        setCreatedAt(midMedium.getId(), base.minusHours(1));
        setCreatedAt(lateLow.getId(), base);

        TodoListReq req = new TodoListReq();
        req.setPageSize(100);
        List<TodoVO> records = todoService.page(req).getRecords();
        // 排序：status ASC(同为pending) > createdAt DESC → 最新 > 居中 > 最早（priority 此时不起决定作用）
        assertEquals("最新低优", records.get(0).getTitle(), "createdAt DESC 应让最新创建排第一");
        assertEquals("居中中优", records.get(1).getTitle());
        assertEquals("最早高优", records.get(2).getTitle());
    }

    @Test
    void delete_shouldPhysicallyRemove() {
        loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("物理删除测试"));

        todoService.delete(todo.getId());

        // 物理删除：DB 中应彻底消失
        assertNull(todoMapper.selectById(todo.getId()), "物理删除后应彻底消失");
        // 主列表查不到
        TodoListReq req = new TodoListReq();
        req.setPageSize(100);
        assertTrue(todoService.page(req).getRecords().stream().noneMatch(t -> t.getId().equals(todo.getId())));
    }

    @Test
    void listToday_shouldIncludeTodayAllAndHistoryPendingAndHistoryDoneToday() {
        loginAsNewUser();
        // 今日创建的 pending
        TodoVO todayPending = todoService.create(newCreateReq("今日待办"));
        // 今日创建的 done
        TodoVO todayDone = todoService.create(newCreateReq("今日已完成"));
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        todoService.changeStatus(todayDone.getId(), done);

        // 历史未完成（pending）：createdAt 改成昨天
        TodoVO historyPending = todoService.create(newCreateReq("历史未完成"));
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, historyPending.getId())
                .set(Todo::getCreatedAt, yesterday));

        // 历史今天完成：createdAt 昨天，今天完成（doneAt=今天）→ 应返回
        TodoVO historyDoneToday = todoService.create(newCreateReq("历史今天完成"));
        todoService.changeStatus(historyDoneToday.getId(), done);
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, historyDoneToday.getId())
                .set(Todo::getCreatedAt, yesterday));

        // 历史已完成（非今天完成）：createdAt 前天、doneAt 前天 → 不应返回
        TodoVO historyDone = todoService.create(newCreateReq("历史已完成"));
        todoService.changeStatus(historyDone.getId(), done);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, historyDone.getId())
                .set(Todo::getCreatedAt, twoDaysAgo)
                .set(Todo::getDoneAt, twoDaysAgo));

        List<TodoVO> result = todoService.listToday();
        // 应含：今日 pending、今日 done、历史 pending、历史今天完成；不含历史（非今天）完成
        assertEquals(4, result.size());
        List<Long> ids = result.stream().map(TodoVO::getId).toList();
        assertTrue(ids.contains(todayPending.getId()), "应含今日 pending");
        assertTrue(ids.contains(todayDone.getId()), "应含今日 done");
        assertTrue(ids.contains(historyPending.getId()), "应含历史 pending");
        assertTrue(ids.contains(historyDoneToday.getId()), "应含历史今天完成");
        assertFalse(ids.contains(historyDone.getId()), "不应含历史（非今天）完成");
    }

    @Test
    void listToday_sortOrderShouldFollowPriorityStatusCreated() {
        loginAsNewUser();
        // 今日创建三条不同优先级，统一 createdAt 让前两排序键(status/createdAt)无法区分，priority 才起作用
        TodoVO low = todoService.create(newCreateReq("低优"));
        TodoVO high = todoService.create(newCreateReq("高优"));
        TodoVO medium = todoService.create(newCreateReq("中优"));
        setPriority(low.getId(), Priority.LOW);
        setPriority(high.getId(), Priority.HIGH);
        setPriority(medium.getId(), Priority.MEDIUM);
        LocalDateTime fixedCreatedAt = LocalDateTime.now().withNano(0);
        setCreatedAt(low.getId(), fixedCreatedAt);
        setCreatedAt(high.getId(), fixedCreatedAt);
        setCreatedAt(medium.getId(), fixedCreatedAt);

        List<TodoVO> result = todoService.listToday();
        // 排序：status ASC(同为pending) > createdAt DESC(相同) > priority DESC → 高优 > 中优 > 低优
        assertEquals("高优", result.get(0).getTitle(), "高优先级应排第一");
        assertEquals("中优", result.get(1).getTitle());
        assertEquals("低优", result.get(2).getTitle());
    }

    @Test
    void monthCalendar_shouldGroupByDay() {
        loginAsNewUser();
        YearMonth ym = YearMonth.now();
        String month = ym.toString();
        LocalDate day3 = ym.atDay(3);
        LocalDate day5 = ym.atDay(5);

        // day3 创建一条 pending：从 day3 起覆盖到月末每一天
        TodoVO t1 = todoService.create(newCreateReq("3号创建"));
        setCreatedAt(t1.getId(), day3.atTime(10, 0));

        // day5 创建并完成（同一天创建+完成）：仅 day5 出现一次
        TodoVO t2 = todoService.create(newCreateReq("5号创建并完成"));
        setCreatedAt(t2.getId(), day5.atTime(9, 0));
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        todoService.changeStatus(t2.getId(), done);
        setDoneAt(t2.getId(), day5.atTime(18, 0));

        // 历史待办（上月创建），本月 day5 完成：覆盖本月 day1~day5
        TodoVO t3 = todoService.create(newCreateReq("历史待办本月5号完成"));
        setCreatedAt(t3.getId(), ym.minusMonths(1).atDay(20).atTime(8, 0));
        todoService.changeStatus(t3.getId(), done);
        setDoneAt(t3.getId(), day5.atTime(15, 0));

        TodoMonthVO result = todoService.monthCalendar(month);

        assertEquals(month, result.getMonth());
        assertEquals(ym.lengthOfMonth(), result.getDays().size(), "应返回月份全部天数");

        // day1：t3 覆盖（上月20~本月5），t1/t2 尚未创建 -> 仅 t3
        TodoMonthVO.DayGroup g1 = result.getDays().get(0);
        List<String> day1Titles = g1.getTodos().stream().map(TodoVO::getTitle).toList();
        assertTrue(day1Titles.contains("历史待办本月5号完成"), "跨月待办应覆盖 day1");
        assertFalse(day1Titles.contains("3号创建"), "day1 尚未创建 t1");

        // day3：t1 当天创建 + t3 跨越覆盖 -> 2 条
        TodoMonthVO.DayGroup g3 = result.getDays().get(2);
        List<String> day3Titles = g3.getTodos().stream().map(TodoVO::getTitle).toList();
        assertEquals(2, g3.getTodos().size(), "day3 应含 t1(当天创建) + t3(处理中)");
        assertTrue(day3Titles.contains("3号创建"));
        assertTrue(day3Titles.contains("历史待办本月5号完成"));

        // day5：t1(处理中) + t2(当天创建并完成) + t3(当天完成) -> 3 条
        TodoMonthVO.DayGroup g5 = result.getDays().get(4);
        List<String> day5Titles = g5.getTodos().stream().map(TodoVO::getTitle).toList();
        assertEquals(3, g5.getTodos().size(), "day5 应含 t1+t2+t3");
        assertTrue(day5Titles.contains("3号创建"), "t1 覆盖 day5");
        assertTrue(day5Titles.contains("5号创建并完成"));
        assertTrue(day5Titles.contains("历史待办本月5号完成"));

        // day6：t1(处理中) + t2(已完成，doneDate=day5 < day6 不覆盖) + t3(已完成，doneDate=day5 不覆盖) -> 仅 t1
        TodoMonthVO.DayGroup g6 = result.getDays().get(5);
        List<String> day6Titles = g6.getTodos().stream().map(TodoVO::getTitle).toList();
        assertEquals(1, g6.getTodos().size(), "day6 仅 t1 处理中");
        assertTrue(day6Titles.contains("3号创建"));
    }

    @Test
    void monthCalendar_crossMonthPending_shouldAppearUntilToday() {
        loginAsNewUser();
        YearMonth ym = YearMonth.now();
        String month = ym.toString();
        LocalDate today = LocalDate.now();

        // 跨月仍 pending：上月 20 号创建，至今未完成 -> 当月 day1 到今天都应出现，未来日不出现
        TodoVO t = todoService.create(newCreateReq("跨月未完成"));
        setCreatedAt(t.getId(), ym.minusMonths(1).atDay(20).atTime(8, 0));

        TodoMonthVO result = todoService.monthCalendar(month);

        for (TodoMonthVO.DayGroup g : result.getDays()) {
            List<String> titles = g.getTodos().stream().map(TodoVO::getTitle).toList();
            if (!g.getDate().isAfter(today)) {
                assertTrue(titles.contains("跨月未完成"),
                        "跨月未完成待办应覆盖当月至今天，day=" + g.getDate() + " 缺失");
            } else {
                assertFalse(titles.contains("跨月未完成"),
                        "未来日期不应显示待办，day=" + g.getDate() + " 不该含");
            }
        }
    }

    @Test
    void monthCalendar_shouldReturnHistoricalStatusPerDay() {
        loginAsNewUser();
        YearMonth ym = YearMonth.now();
        String month = ym.toString();
        LocalDate day3 = ym.atDay(3);
        LocalDate day5 = ym.atDay(5);
        LocalDate day6 = ym.atDay(6);

        // day3 创建，day6 完成 -> day3/day5 应展示 pending，day6 应展示 done
        TodoVO t = todoService.create(newCreateReq("历史状态快照"));
        setCreatedAt(t.getId(), day3.atTime(9, 0));
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        todoService.changeStatus(t.getId(), done);
        setDoneAt(t.getId(), day6.atTime(18, 0));

        TodoMonthVO result = todoService.monthCalendar(month);

        // day5（完成前）：应返回 pending、doneAt=null
        TodoMonthVO.DayGroup g5 = result.getDays().get(day5.getDayOfMonth() - 1);
        TodoVO day5Todo = g5.getTodos().stream().filter(x -> x.getId().equals(t.getId())).findFirst().orElseThrow();
        assertEquals("0", day5Todo.getStatus(), "完成日之前应展示为 pending");
        assertNull(day5Todo.getDoneAt(), "完成日之前 doneAt 应为 null");

        // day6（完成当天）：应返回 done、doneAt=day6
        TodoMonthVO.DayGroup g6 = result.getDays().get(day6.getDayOfMonth() - 1);
        TodoVO day6Todo = g6.getTodos().stream().filter(x -> x.getId().equals(t.getId())).findFirst().orElseThrow();
        assertEquals("1", day6Todo.getStatus(), "完成当天应展示为 done");
        assertNotNull(day6Todo.getDoneAt(), "完成当天 doneAt 应有值");

        // day3（创建当天，尚未完成）：应返回 pending
        TodoMonthVO.DayGroup g3 = result.getDays().get(day3.getDayOfMonth() - 1);
        TodoVO day3Todo = g3.getTodos().stream().filter(x -> x.getId().equals(t.getId())).findFirst().orElseThrow();
        assertEquals("0", day3Todo.getStatus(), "创建当天尚未完成应展示为 pending");
        assertNull(day3Todo.getDoneAt());
    }

    @Test
    void monthCalendar_emptyMonth_shouldReturnFullDaysWithEmptyGroups() {
        loginAsNewUser();
        String month = YearMonth.now().toString();
        // 不造任何数据

        TodoMonthVO result = todoService.monthCalendar(month);

        assertEquals(month, result.getMonth());
        assertEquals(YearMonth.now().lengthOfMonth(), result.getDays().size());
        // 每天的 todos 都应为空
        assertTrue(result.getDays().stream().allMatch(g -> g.getTodos().isEmpty()));
    }

    @Test
    void monthCalendar_invalidMonth_shouldThrow() {
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> todoService.monthCalendar("2026/07"));
        assertEquals(ResultCode.PARAM_VALIDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void dataIsolation_userCannotAccessOthersTodo() {
        Long owner = loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("我的私有任务"));

        // 切换到另一个用户
        Long other = loginAsNewUser();
        // other 用户查不到 owner 的待办详情
        BusinessException ex = assertThrows(BusinessException.class,
                () -> todoService.getById(todo.getId()));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void list_withKeyword_shouldFilterByTitleAndNote() {
        loginAsNewUser();
        todoService.create(newCreateReq("学习Vue"));
        TodoCreateReq t2 = newCreateReq("跑步");
        t2.setNote("学习后跑步");
        todoService.create(t2);
        todoService.create(newCreateReq("做饭"));

        TodoListReq req = new TodoListReq();
        req.setKeyword("学习");
        req.setPageSize(100);
        PageResp<TodoVO> result = todoService.page(req);
        assertEquals(2, result.getTotal(), "标题和备注含关键字都应命中");
    }

    @Test
    void list_withKeyword_pagination_shouldReturnPage() {
        loginAsNewUser();
        for (int i = 0; i < 15; i++) {
            todoService.create(newCreateReq("学习" + i));
        }
        todoService.create(newCreateReq("跑步"));

        TodoListReq req = new TodoListReq();
        req.setKeyword("学习");
        req.setPageSize(10);
        PageResp<TodoVO> p1 = todoService.page(req);
        assertEquals(15, p1.getTotal());
        assertEquals(10, p1.getRecords().size());
        assertEquals(2, p1.getPages());

        req.setPageNum(2);
        PageResp<TodoVO> p2 = todoService.page(req);
        assertEquals(5, p2.getRecords().size(), "第二页应 5 条");
    }

    @Test
    void list_withPriority_shouldFilter() {
        loginAsNewUser();
        TodoCreateReq high = newCreateReq("高优");
        high.setPriority(Priority.HIGH);
        todoService.create(high);

        TodoCreateReq low = newCreateReq("低优");
        low.setPriority(Priority.LOW);
        todoService.create(low);

        TodoListReq req = new TodoListReq();
        req.setPriority(Priority.HIGH);
        req.setPageSize(100);
        PageResp<TodoVO> result = todoService.page(req);
        assertEquals(1, result.getTotal());
        assertEquals("高优", result.getRecords().get(0).getTitle());
    }

    @Test
    void list_withStatus_shouldFilter() {
        loginAsNewUser();
        TodoVO pending = todoService.create(newCreateReq("待处理"));
        TodoVO done = todoService.create(newCreateReq("已完成"));
        TodoStatusReq statusReq = new TodoStatusReq();
        statusReq.setStatus(TodoStatus.DONE);
        todoService.changeStatus(done.getId(), statusReq);

        // 只查 pending
        TodoListReq req = new TodoListReq();
        req.setStatus(TodoStatus.PENDING);
        req.setPageSize(100);
        PageResp<TodoVO> pendingPage = todoService.page(req);
        assertEquals(1, pendingPage.getTotal());
        assertEquals(pending.getId(), pendingPage.getRecords().get(0).getId());

        // 只查 done
        req.setStatus(TodoStatus.DONE);
        PageResp<TodoVO> donePage = todoService.page(req);
        assertEquals(1, donePage.getTotal());
        assertEquals(done.getId(), donePage.getRecords().get(0).getId());
    }

    @Test
    void list_withMultipleConditions_shouldAndFilter() {
        Long userId = loginAsNewUser();
        Category work = newCategory(userId, null, "工作");
        categoryMapper.insert(work);
        Category life = newCategory(userId, null, "生活");
        categoryMapper.insert(life);

        // 命中：高优 + 工作分类 + pending + 含"学习"
        TodoCreateReq hit = newCreateReq("学习Vue");
        hit.setPriority(Priority.HIGH);
        hit.setCategoryId(work.getId());
        todoService.create(hit);

        // 不命中：低优
        TodoCreateReq low = newCreateReq("学习Java");
        low.setPriority(Priority.LOW);
        low.setCategoryId(work.getId());
        todoService.create(low);

        // 不命中：分类不同
        TodoCreateReq otherCat = newCreateReq("学习React");
        otherCat.setPriority(Priority.HIGH);
        otherCat.setCategoryId(life.getId());
        todoService.create(otherCat);

        TodoListReq req = new TodoListReq();
        req.setKeyword("学习");
        req.setPriority(Priority.HIGH);
        req.setCategoryId(work.getId());
        req.setStatus(TodoStatus.PENDING);
        req.setPageSize(100);
        PageResp<TodoVO> result = todoService.page(req);
        assertEquals(1, result.getTotal(), "多条件 AND 应只命中 1 条");
        assertEquals("学习Vue", result.getRecords().get(0).getTitle());
    }

    @Test
    void page_withRootCategory_shouldIncludeSubCategories() {
        Long userId = loginAsNewUser();
        // 建大分类 + 两个子分类
        Category root = newCategory(userId, null, "大分类");
        categoryMapper.insert(root);
        Category sub1 = newCategory(userId, root.getId(), "子分类1");
        categoryMapper.insert(sub1);
        Category sub2 = newCategory(userId, root.getId(), "子分类2");
        categoryMapper.insert(sub2);

        // 三个待办分别挂在大分类、子分类1、子分类2 下
        TodoCreateReq r = newCreateReq("挂大分类");
        r.setCategoryId(root.getId());
        todoService.create(r);
        TodoCreateReq s1 = newCreateReq("挂子分类1");
        s1.setCategoryId(sub1.getId());
        todoService.create(s1);
        TodoCreateReq s2 = newCreateReq("挂子分类2");
        s2.setCategoryId(sub2.getId());
        todoService.create(s2);

        // 传入大分类 ID → 应查出全部 3 条（自身 + 两个子分类）
        TodoListReq req = new TodoListReq();
        req.setCategoryId(root.getId());
        req.setPageSize(100);
        PageResp<TodoVO> result = todoService.page(req);
        assertEquals(3, result.getTotal(), "大分类筛选应联动子分类");
    }

    @Test
    void page_withSubCategory_shouldOnlyQueryItself() {
        Long userId = loginAsNewUser();
        Category root = newCategory(userId, null, "大分类");
        categoryMapper.insert(root);
        Category sub1 = newCategory(userId, root.getId(), "子分类1");
        categoryMapper.insert(sub1);
        Category sub2 = newCategory(userId, root.getId(), "子分类2");
        categoryMapper.insert(sub2);

        todoService.create(makeReqWithCategory("挂子分类1", sub1.getId()));
        todoService.create(makeReqWithCategory("挂子分类2", sub2.getId()));

        // 传入子分类1 → 只查自身，不查兄弟子分类2
        TodoListReq req = new TodoListReq();
        req.setCategoryId(sub1.getId());
        req.setPageSize(100);
        PageResp<TodoVO> result = todoService.page(req);
        assertEquals(1, result.getTotal(), "子分类筛选只查自身");
        assertEquals("挂子分类1", result.getRecords().get(0).getTitle());
    }

    @Test
    void countPending_today_shouldCountTodayAndHistoryPendingExcludeDone() {
        loginAsNewUser();
        // 今日创建 pending
        todoService.create(newCreateReq("今日pending"));
        // 今日创建并完成
        TodoVO todayDone = todoService.create(newCreateReq("今日done"));
        TodoStatusReq done = new TodoStatusReq();
        done.setStatus(TodoStatus.DONE);
        todoService.changeStatus(todayDone.getId(), done);
        // 历史创建仍 pending：createdAt 改成昨天
        TodoVO historyPending = todoService.create(newCreateReq("历史pending"));
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, historyPending.getId())
                .set(Todo::getCreatedAt, LocalDateTime.now().minusDays(1)));
        // 历史已完成（非今天完成）：createdAt 前天、doneAt 前天 → 不计入
        TodoVO historyDone = todoService.create(newCreateReq("历史done"));
        todoService.changeStatus(historyDone.getId(), done);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, historyDone.getId())
                .set(Todo::getCreatedAt, twoDaysAgo)
                .set(Todo::getDoneAt, twoDaysAgo));

        // 今日待办中的未完成 = 今日pending + 历史pending = 2
        long count = todoService.countPending(null);
        assertEquals(2, count, "今日未完成应含今日pending与历史pending，排除done");
    }

    @Test
    void countPending_withCategory_shouldIncludeSubCategoriesAndExcludeDone() {
        Long userId = loginAsNewUser();
        Category root = newCategory(userId, null, "大分类");
        categoryMapper.insert(root);
        Category sub = newCategory(userId, root.getId(), "子分类");
        categoryMapper.insert(sub);
        // 大分类下 pending
        todoService.create(makeReqWithCategory("大分类pending", root.getId()));
        // 子分类下 pending
        todoService.create(makeReqWithCategory("子分类pending", sub.getId()));
        // 子分类下 done → 不计
        TodoVO done = todoService.create(makeReqWithCategory("子分类done", sub.getId()));
        TodoStatusReq doneReq = new TodoStatusReq();
        doneReq.setStatus(TodoStatus.DONE);
        todoService.changeStatus(done.getId(), doneReq);
        // 不属于该父分类的 pending → 不计
        todoService.create(newCreateReq("无分类pending"));

        // 传父分类 → 大分类 + 子分类 的 pending = 2
        long count = todoService.countPending(root.getId());
        assertEquals(2, count, "父分类未完成应含子分类且排除done");
    }

    @Test
    void countPending_shouldIsolateByUser() {
        Long userA = loginAsNewUser();
        Category rootA = newCategory(userA, null, "A分类");
        categoryMapper.insert(rootA);
        todoService.create(makeReqWithCategory("A的pending", rootA.getId()));

        // 切换到用户 B：用 A 的分类 ID 查 → 越权应统一 404（loadOwned 模式）
        loginAsNewUser();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> todoService.countPending(rootA.getId()));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode(), "跨用户访问分类应返回 404");
    }

    @Test
    void update_shouldUpdateRequiredFields() {
        loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("原标题"));
        TodoUpdateReq upd = new TodoUpdateReq();
        upd.setTitle("新标题");
        upd.setCategoryId(100L);
        upd.setPriority(Priority.HIGH);
        TodoVO vo = todoService.update(todo.getId(), upd);
        assertEquals("新标题", vo.getTitle());
        assertEquals(100L, vo.getCategoryId());
        assertEquals("2", vo.getPriority());
    }

    @Test
    void update_priorityNull_shouldDefaultMedium() {
        loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("原标题"));
        TodoUpdateReq upd = new TodoUpdateReq();
        upd.setTitle("新标题");
        upd.setCategoryId(100L);
        upd.setPriority(null); // 为空应默认中
        TodoVO vo = todoService.update(todo.getId(), upd);
        assertEquals("1", vo.getPriority(), "优先级为空应默认中");
    }

    @Test
    void create_duplicateTitleToday_shouldThrowConflict() {
        loginAsNewUser();
        todoService.create(newCreateReq("买牛奶"));
        // 当天再创建同名 → 应抛冲突
        BusinessException ex = assertThrows(BusinessException.class,
                () -> todoService.create(newCreateReq("买牛奶")));
        assertEquals(ResultCode.TODO_TITLE_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void update_duplicateTitleToday_shouldThrowConflict() {
        loginAsNewUser();
        todoService.create(newCreateReq("买牛奶"));
        TodoVO target = todoService.create(newCreateReq("买面包"));
        // 把 target 标题改成已存在的"买牛奶" → 应抛冲突
        TodoUpdateReq upd = new TodoUpdateReq();
        upd.setTitle("买牛奶");
        upd.setCategoryId(100L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> todoService.update(target.getId(), upd));
        assertEquals(ResultCode.TODO_TITLE_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void update_sameTitleAsSelf_shouldNotThrowConflict() {
        loginAsNewUser();
        TodoVO todo = todoService.create(newCreateReq("买牛奶"));
        // 改成自己的同名标题（其他字段变更）→ 不应判重
        TodoUpdateReq upd = new TodoUpdateReq();
        upd.setTitle("买牛奶");
        upd.setCategoryId(200L);
        upd.setPriority(Priority.HIGH);
        TodoVO vo = todoService.update(todo.getId(), upd);
        assertEquals("买牛奶", vo.getTitle());
        assertEquals(200L, vo.getCategoryId());
    }

    private void setCreatedAt(Long id, LocalDateTime createdAt) {
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, id)
                .set(Todo::getCreatedAt, createdAt));
    }

    private void setDoneAt(Long id, LocalDateTime doneAt) {
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, id)
                .set(Todo::getDoneAt, doneAt));
    }

    private void setPriority(Long id, Priority priority) {
        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getId, id)
                .set(Todo::getPriority, priority.getValue()));
    }

    private TodoCreateReq newCreateReq(String title) {
        TodoCreateReq req = new TodoCreateReq();
        req.setTitle(title);
        return req;
    }

    private TodoCreateReq makeReqWithCategory(String title, Long categoryId) {
        TodoCreateReq req = newCreateReq(title);
        req.setCategoryId(categoryId);
        return req;
    }

    private Category newCategory(Long userId, Long parentId, String name) {
        Category c = new Category();
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setName(name);
        c.setSortOrder(0);
        return c;
    }

    private int indexOf(List<TodoVO> list, Long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) return i;
        }
        return -1;
    }
}
