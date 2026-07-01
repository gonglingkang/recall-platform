package com.recall.controller.sprint;

import com.recall.common.api.Result;
import com.recall.dto.sprint.SprintCreateReq;
import com.recall.dto.sprint.SprintInvolvedReq;
import com.recall.dto.sprint.SprintLinkReq;
import com.recall.dto.sprint.SprintStatusReq;
import com.recall.dto.sprint.SprintUpdateReq;
import com.recall.service.sprint.SprintService;
import com.recall.vo.sprint.SprintItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * 团队冲刺 Controller。
 *
 * @author recall
 */
@Tag(name = "团队冲刺", description = "团队冲刺任务管理、需我介入、关联关键成果")
@RestController
@RequestMapping("/api/sprint/items")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @Operation(summary = "冲刺列表", description = "可按「需我介入」过滤")
    @GetMapping
    public Result<List<SprintItemVO>> list(@Parameter(description = "月份 YYYY-MM") @RequestParam String month,
                                           @Parameter(description = "是否仅看需我介入") @RequestParam(required = false) Boolean needInvolved) {
        return Result.ok(sprintService.list(month, needInvolved));
    }

    @Operation(summary = "新增冲刺任务", description = "只需 month + title + note")
    @PostMapping
    public Result<SprintItemVO> create(@Valid @RequestBody SprintCreateReq req) {
        return Result.ok(sprintService.create(req));
    }

    @Operation(summary = "编辑冲刺任务", description = "可改 title/note")
    @PutMapping("/{id}")
    public Result<SprintItemVO> update(@Parameter(description = "任务ID") @PathVariable Long id,
                                       @Valid @RequestBody SprintUpdateReq req) {
        return Result.ok(sprintService.update(id, req));
    }

    @Operation(summary = "状态变更", description = "用户手动切换：未开始/进行中/已完成")
    @PatchMapping("/{id}/status")
    public Result<SprintItemVO> changeStatus(@Parameter(description = "任务ID") @PathVariable Long id,
                                             @Valid @RequestBody SprintStatusReq req) {
        return Result.ok(sprintService.changeStatus(id, req));
    }

    @Operation(summary = "切换需我介入", description = "true→false 时清空关联的关键成果")
    @PatchMapping("/{id}/involved")
    public Result<SprintItemVO> toggleInvolved(@Parameter(description = "任务ID") @PathVariable Long id,
                                               @Valid @RequestBody SprintInvolvedReq req) {
        return Result.ok(sprintService.toggleInvolved(id, req));
    }

    @Operation(summary = "关联关键成果", description = "全量覆盖关联；仅需介入时允许；空列表=取消全部关联")
    @PutMapping("/{id}/key-results")
    public Result<SprintItemVO> linkKeyResults(@Parameter(description = "任务ID") @PathVariable Long id,
                                               @Valid @RequestBody SprintLinkReq req) {
        return Result.ok(sprintService.linkKeyResults(id, req));
    }

    @Operation(summary = "删除冲刺任务", description = "连带删除关联关系")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "任务ID") @PathVariable Long id) {
        sprintService.delete(id);
        return Result.ok();
    }
}
