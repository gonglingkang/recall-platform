package com.recall.controller.todo;

import com.recall.common.api.PageResp;
import com.recall.common.api.Result;
import com.recall.dto.todo.TodoCreateReq;
import com.recall.dto.todo.TodoListReq;
import com.recall.dto.todo.TodoStatusReq;
import com.recall.dto.todo.TodoUpdateReq;
import com.recall.service.todo.TodoService;
import com.recall.vo.todo.TodoMonthVO;
import com.recall.vo.todo.TodoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 待办 Controller。
 *
 * @author recall
 */
@Tag(name = "待办", description = "待办的增删改查、完成")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@Validated
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "今日待办", description = "今日创建的全部待办（含已完成）+ 历史未完成待办；不分页")
    @GetMapping("/today")
    public Result<List<TodoVO>> today() {
        return Result.ok(todoService.listToday());
    }

    @Operation(summary = "月度待办日历", description = "按天分组返回当月可见待办：含当天创建、当天完成，以及生命周期跨越当天的处理中待办（如跨月未来完成）")
    @GetMapping("/month")
    public Result<TodoMonthVO> month(@Parameter(description = "月份 YYYY-MM") @RequestParam("month") String month) {
        return Result.ok(todoService.monthCalendar(month));
    }

    @Operation(summary = "按日期查待办", description = "返回指定日期当天的待办（状态为当天真实快照）：含当天创建、当天完成、当天仍 pending、当天未完成但未来才完成（当天状态为 pending）；未来日期返回空")
    @GetMapping("/by-date/{date}")
    public Result<List<TodoVO>> byDate(@Parameter(description = "日期 YYYY-MM-DD")
                                       @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.ok(todoService.listByDate(date));
    }

    @Operation(summary = "待办分页查询", description = "支持按分类（联动子分类）、优先级、完成状态、关键词过滤，所有条件 AND 生效；分页返回")
    @GetMapping
    public Result<PageResp<TodoVO>> page(TodoListReq req) {
        return Result.ok(todoService.page(req));
    }

    @Operation(summary = "未完成待办计数",
            description = "不传 categoryId：统计今日待办中的未完成数（今日创建 + 历史创建仍 pending）；" +
                    "传 categoryId：统计该父分类（含所有子分类）下的未完成数，不限创建时间")
    @GetMapping("/pending-count")
    public Result<Long> pendingCount(@Parameter(description = "分类ID，不传则统计今日未完成") @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return Result.ok(todoService.countPending(categoryId));
    }

    @Operation(summary = "待办详情")
    @GetMapping("/{id}")
    public Result<TodoVO> get(@PathVariable Long id) {
        return Result.ok(todoService.getById(id));
    }

    @Operation(summary = "创建待办", description = "快速创建仅需 title")
    @PostMapping
    public Result<TodoVO> create(@Valid @RequestBody TodoCreateReq req) {
        return Result.ok(todoService.create(req));
    }

    @Operation(summary = "编辑待办", description = "按需更新字段")
    @PutMapping("/{id}")
    public Result<TodoVO> update(@PathVariable Long id,
                                 @Valid @RequestBody TodoUpdateReq req) {
        return Result.ok(todoService.update(id, req));
    }

    @Operation(summary = "完成/撤销", description = "pending ⇄ done 状态机")
    @PatchMapping("/{id}/status")
    public Result<TodoVO> changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody TodoStatusReq req) {
        return Result.ok(todoService.changeStatus(id, req));
    }

    @Operation(summary = "删除", description = "物理删除，不可恢复")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return Result.ok();
    }
}
