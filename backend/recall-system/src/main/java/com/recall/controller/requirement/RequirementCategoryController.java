package com.recall.controller.requirement;

import com.recall.common.api.Result;
import com.recall.dto.requirement.RequirementCategoryCreateReq;
import com.recall.dto.requirement.RequirementCategoryUpdateReq;
import com.recall.service.requirement.RequirementCategoryService;
import com.recall.vo.requirement.RequirementCategoryVO;
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
 * 需求分类 Controller。
 * <p>
 * 两级分类合并单表，主分类与子分类统一经 /api/requirement-categories 管理；
 * 创建子分类时 body 传 parentId 指向主分类。与待办分类完全独立。
 *
 * @author recall
 */
@Tag(name = "需求分类", description = "需求两级分类管理（主分类+子分类），独立于待办分类")
@RestController
@RequestMapping("/api/requirement-categories")
@RequiredArgsConstructor
public class RequirementCategoryController {

    private final RequirementCategoryService requirementCategoryService;

    @Operation(summary = "分类树列表", description = "返回当前用户的两级需求分类树")
    @GetMapping
    public Result<List<RequirementCategoryVO>> listTree() {
        return Result.ok(requirementCategoryService.listTree());
    }

    @Operation(summary = "新增分类", description = "parentId 为空建主分类，非空建子分类（最多两级）")
    @PostMapping
    public Result<RequirementCategoryVO> create(@Valid @RequestBody RequirementCategoryCreateReq req) {
        return Result.ok(requirementCategoryService.createCategory(req));
    }

    @Operation(summary = "编辑分类", description = "不支持变更父分类；color 仅主分类可改")
    @PutMapping("/{id}")
    public Result<RequirementCategoryVO> update(@Parameter(description = "分类ID") @PathVariable Long id,
                                                @Valid @RequestBody RequirementCategoryUpdateReq req) {
        return Result.ok(requirementCategoryService.updateCategory(id, req));
    }

    @Operation(summary = "删除分类", description = "有子分类或有需求占用均拒绝删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        requirementCategoryService.deleteCategory(id);
        return Result.ok();
    }
}
