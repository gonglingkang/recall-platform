package com.recall.controller.todo;

import com.recall.common.api.Result;
import com.recall.dto.todo.TodoTemplateSaveReq;
import com.recall.service.todo.TodoTemplateService;
import com.recall.vo.todo.TodoTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 周期待办模板 Controller。
 *
 * @author recall
 */
@Tag(name = "周期待办", description = "周期待办模板管理：定时按周期自动生成待办，完成门控（未完成不生成下一条）")
@RestController
@RequestMapping("/api/todo-templates")
@RequiredArgsConstructor
public class TodoTemplateController {

    private final TodoTemplateService todoTemplateService;

    @Operation(summary = "模板列表", description = "当前用户的模板，含最近生成待办的状态（门控进展）")
    @GetMapping
    public Result<List<TodoTemplateVO>> list() {
        return Result.ok(todoTemplateService.list());
    }

    @Operation(summary = "新增模板", description = "next_date 自动计算为今天之后的第一个周期日")
    @PostMapping
    public Result<TodoTemplateVO> create(@Valid @RequestBody TodoTemplateSaveReq req) {
        return Result.ok(todoTemplateService.create(req));
    }

    @Operation(summary = "编辑模板", description = "周期变更后 next_date 重算；未完成的在途待办不受影响")
    @PutMapping("/{id}")
    public Result<TodoTemplateVO> update(@Parameter(description = "模板ID") @PathVariable Long id,
                                         @Valid @RequestBody TodoTemplateSaveReq req) {
        return Result.ok(todoTemplateService.update(id, req));
    }

    @Operation(summary = "删除模板", description = "仅删模板，已生成的待办不受影响")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "模板ID") @PathVariable Long id) {
        todoTemplateService.delete(id);
        return Result.ok();
    }
}
