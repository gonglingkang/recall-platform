package com.recall.controller.requirement;

import com.recall.common.api.PageResp;
import com.recall.common.api.Result;
import com.recall.dto.requirement.RequirementCreateReq;
import com.recall.dto.requirement.RequirementPageReq;
import com.recall.dto.requirement.RequirementStatusReq;
import com.recall.dto.requirement.RequirementUpdateReq;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.RequirementVO;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 需求 Controller。
 *
 * @author recall
 */
@Tag(name = "需求管理", description = "需求的增删改查、状态流转、绑定关键成果K")
@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @Operation(summary = "需求分页列表", description = "支持关键字、创建时间范围、状态过滤")
    @GetMapping("/page")
    public Result<PageResp<RequirementVO>> page(@Valid RequirementPageReq req) {
        return Result.ok(requirementService.page(req));
    }

    @Operation(summary = "需求详情", description = "含文档列表与绑定的关键成果K摘要")
    @GetMapping("/{id}")
    public Result<RequirementVO> getById(@Parameter(description = "需求ID") @PathVariable Long id) {
        return Result.ok(requirementService.getById(id));
    }

    @Operation(summary = "创建需求", description = "初始状态为讨论中；可选传 keyResultId 绑定K")
    @PostMapping
    public Result<RequirementVO> create(@Valid @RequestBody RequirementCreateReq req) {
        return Result.ok(requirementService.create(req));
    }

    @Operation(summary = "编辑需求", description = "可改 title/description；keyResultId 传值绑定K，不传不变")
    @PutMapping("/{id}")
    public Result<RequirementVO> update(@Parameter(description = "需求ID") @PathVariable Long id,
                                        @Valid @RequestBody RequirementUpdateReq req) {
        return Result.ok(requirementService.update(id, req));
    }

    @Operation(summary = "状态变更", description = "手动切换状态；绑K时讨论中/进行中/开发完成由K联动不可手动改，仅可进入不涉及/验收完成/发布完成")
    @PatchMapping("/{id}/status")
    public Result<RequirementVO> changeStatus(@Parameter(description = "需求ID") @PathVariable Long id,
                                              @Valid @RequestBody RequirementStatusReq req) {
        return Result.ok(requirementService.changeStatus(id, req));
    }

    @Operation(summary = "删除需求", description = "连带删除其下文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "需求ID") @PathVariable Long id) {
        requirementService.delete(id);
        return Result.ok();
    }
}
