package com.recall.controller.objectives;

import com.recall.common.api.Result;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.objectives.KeyResultUpdateReq;
import com.recall.service.objectives.KeyResultService;
import com.recall.vo.objectives.KeyResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关键成果 K Controller（v2.0）。
 * <p>
 * K 的全部接口统一在 /api/key-results 下：
 * 新增（objectiveId 放请求体）、编辑、状态切换、删除。
 *
 * @author recall
 */
@Tag(name = "关键成果", description = "目标O下的关键成果K管理，状态切换改变进度")
@RestController
@RequestMapping("/api/key-results")
@RequiredArgsConstructor
public class KeyResultController {

    private final KeyResultService keyResultService;

    @Operation(summary = "新增关键成果", description = "objectiveId 放请求体指定归属目标，completeDate后端管理")
    @PostMapping
    public Result<KeyResultVO> create(@Valid @RequestBody KeyResultCreateReq req) {
        return Result.ok(keyResultService.create(req));
    }

    @Operation(summary = "编辑关键成果", description = "name/description/planCompleteDate，不改状态")
    @PutMapping("/{id}")
    public Result<KeyResultVO> update(@Parameter(description = "关键成果ID") @PathVariable Long id,
                                      @Valid @RequestBody KeyResultUpdateReq req) {
        return Result.ok(keyResultService.update(id, req));
    }

    @Operation(summary = "切换关键成果状态", description = "改变进度的唯一入口，completeDate后端自动维护")
    @PatchMapping("/{id}/status")
    public Result<KeyResultVO> changeStatus(@Parameter(description = "关键成果ID") @PathVariable Long id,
                                            @Valid @RequestBody KeyResultStatusReq req) {
        return Result.ok(keyResultService.changeStatus(id, req));
    }

    @Operation(summary = "删除关键成果", description = "删除后归属目标的派生进度自动重算")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "关键成果ID") @PathVariable Long id) {
        keyResultService.delete(id);
        return Result.ok();
    }
}
