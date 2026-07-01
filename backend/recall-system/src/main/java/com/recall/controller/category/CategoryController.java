package com.recall.controller.category;

import com.recall.common.api.Result;
import com.recall.dto.category.CategoryCreateReq;
import com.recall.dto.category.CategoryUpdateReq;
import com.recall.service.category.CategoryService;
import com.recall.vo.category.CategoryVO;
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
 * 分类 Controller（PRD 11.3）。
 * <p>
 * 两层分类合并单表，大分类与子分类统一经 /api/categories 管理；
 * 创建子分类时 body 传 parentId 指向大分类。
 *
 * @author recall
 */
@Tag(name = "分类", description = "两层分类管理（大分类+子分类）")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树列表", description = "返回当前用户的两层分类树")
    @GetMapping
    public Result<List<CategoryVO>> listTree() {
        return Result.ok(categoryService.listTree());
    }

    @Operation(summary = "新增分类", description = "parentId 为空建大分类，非空建子分类（最多两级）")
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryCreateReq req) {
        return Result.ok(categoryService.createCategory(req));
    }

    @Operation(summary = "编辑分类", description = "不支持变更父分类；color 仅大分类可改")
    @PutMapping("/{id}")
    public Result<CategoryVO> update(@Parameter(description = "分类ID") @PathVariable Long id,
                                     @Valid @RequestBody CategoryUpdateReq req) {
        return Result.ok(categoryService.updateCategory(id, req));
    }

    @Operation(summary = "删除分类", description = "有子分类一律拒绝；根节点须无待办，非根节点待办迁移到父分类后删除（PRD 6.4.1）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok();
    }
}
