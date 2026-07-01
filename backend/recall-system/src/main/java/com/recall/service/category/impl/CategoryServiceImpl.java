package com.recall.service.category.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.category.CategoryMapper;
import com.recall.dto.category.CategoryCreateReq;
import com.recall.dto.category.CategoryUpdateReq;
import com.recall.entity.category.Category;
import com.recall.service.category.CategoryService;
import com.recall.service.todo.TodoService;
import com.recall.vo.category.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类 Service 实现（PRD 6.4）。
 * <p>
 * 两层分类合并单表（categories），用 parentId 表达层级：
 * parentId 为 null = 大分类（第一层）；非空 = 子分类（第二层）。
 * 应用层控制最多 2 层：子分类的 parent 必须是大分类。
 * 名称唯一性：同用户同父下名称唯一（PRD 4.1），由 DB 唯一键 uk_user_parent_name 兜底。
 * color 仅大分类可用，子分类强制 null。
 * 删除规则（层级无关）：任意节点有子分类一律拒绝；根节点须无待办，非根节点待办迁移到父分类后删除。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    /** @Lazy 打破潜在循环依赖：TodoServiceImpl 若反向注入 CategoryService 时 */
    @Lazy
    private final TodoService todoService;

    // ===================== 查询 =====================

    @Override
    public List<CategoryVO> listTree() {
        Long userId = UserContextHolder.requireUserId();
        // 大分类：parent_id IS NULL，按 sortOrder 升序
        List<Category> roots = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .isNull(Category::getParentId)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        if (roots.isEmpty()) {
            return Collections.emptyList();
        }
        // 子分类：一次性查出，按 parentId 分组
        List<Long> rootIds = roots.stream().map(Category::getId).toList();
        List<Category> subs = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .in(Category::getParentId, rootIds)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        Map<Long, List<CategoryVO>> subMap = subs.stream()
                .collect(Collectors.groupingBy(Category::getParentId,
                        Collectors.mapping(this::toVO, Collectors.toList())));

        return roots.stream()
                .map(r -> toVO(r, subMap.getOrDefault(r.getId(), Collections.emptyList())))
                .toList();
    }

    // ===================== 写操作 =====================

    @Override
    public CategoryVO createCategory(CategoryCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        Long parentId = req.getParentId();
        String color = null;
        if (parentId == null) {
            // 创建大分类：允许设置 color
            color = req.getColor();
        } else {
            // 创建子分类：校验 2 层约束——parent 必须存在、归属当前用户、且 parent 本身是大分类
            Category parent = loadOwned(parentId);
            if (parent.getParentId() != null) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "最多两级分类，子分类下不可再建子分类");
            }
            // 子分类强制 color 为 null（忽略前端传入）
        }
        // 同用户同父下名称唯一（DB 唯一键兜底，这里提前校验给友好提示）
        if (categoryMapper.exists(new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .eq(parentId != null, Category::getParentId, parentId)
                .isNull(parentId == null, Category::getParentId)
                .eq(Category::getName, req.getName()))) {
            throw new BusinessException(ResultCode.CONFLICT, "分类名已存在: " + req.getName());
        }
        Category c = new Category();
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setName(req.getName());
        c.setColor(color);
        c.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        categoryMapper.insert(c);
        return toVO(c, Collections.emptyList());
    }

    @Override
    public CategoryVO updateCategory(Long id, CategoryUpdateReq req) {
        Category c = loadOwned(id);
        // color 仅大分类可改
        boolean isRoot = c.getParentId() == null;
        if (req.getColor() != null && !isRoot) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "子分类不支持设置颜色");
        }
        // 名称变更需校验同父唯一
        if (!req.getName().equals(c.getName())) {
            if (categoryMapper.exists(new LambdaQueryWrapper<Category>()
                    .eq(Category::getUserId, c.getUserId())
                    .eq(c.getParentId() != null, Category::getParentId, c.getParentId())
                    .isNull(c.getParentId() == null, Category::getParentId)
                    .eq(Category::getName, req.getName())
                    .ne(Category::getId, id))) {
                throw new BusinessException(ResultCode.CONFLICT, "分类名已存在: " + req.getName());
            }
            c.setName(req.getName());
        }
        if (req.getColor() != null) c.setColor(req.getColor());
        if (req.getSortOrder() != null) c.setSortOrder(req.getSortOrder());
        categoryMapper.updateById(c);
        return toVO(c, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category c = loadOwned(id);
        // 层级无关：任意节点删除前，其下有子分类一律拒绝（避免悬空子分类）
        boolean hasSub = categoryMapper.exists(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (hasSub) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "请先删除该分类下的子分类");
        }
        if (c.getParentId() == null) {
            // 根节点（无父）：待办无处迁移，须无直接待办才允许删
            if (todoService.existsByCategory(id)) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "该分类下有待办，无法删除");
            }
            categoryMapper.deleteById(id);
            log.info("删除根分类 id={}", id);
        } else {
            // 非根节点（有父）：待办迁移到父分类后删除
            int todoCnt = todoService.migrateByCategory(id, c.getParentId());
            categoryMapper.deleteById(id);
            log.info("删除非根分类 id={}, 迁移待办{}条到父分类{}", id, todoCnt, c.getParentId());
        }
    }

    // ===================== 辅助 =====================

    @Override
    public Category getById(Long id, boolean checkOwnership) {
        if (checkOwnership) {
            return loadOwned(id);
        }
        return categoryMapper.selectById(id);
    }

    @Override
    public List<Long> listCategoryAndSubIds(Long id) {
        // 校验归属：查不到或不属于当前用户抛 404
        Category category = loadOwned(id);
        Long userId = UserContextHolder.requireUserId();
        // 大分类：查自身 + 所有子分类；子分类：仅自身
        List<Long> subIds = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .eq(Category::getParentId, id))
                .stream().map(Category::getId).toList();
        List<Long> result = new java.util.ArrayList<>();
        result.add(category.getId());
        result.addAll(subIds);
        return result;
    }

    /** 加载当前用户的分类，越权/不存在抛 404 */
    private Category loadOwned(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null || !UserContextHolder.requireUserId().equals(c.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在或无权访问");
        }
        return c;
    }

    private CategoryVO toVO(Category c, List<CategoryVO> subs) {
        CategoryVO vo = CategoryVO.builder()
                .id(c.getId())
                .parentId(c.getParentId())
                .name(c.getName())
                .color(c.getColor())
                .sortOrder(c.getSortOrder())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
        vo.setSubcategories(subs == null ? new ArrayList<>() : subs);
        return vo;
    }

    private CategoryVO toVO(Category c) {
        return toVO(c, Collections.emptyList());
    }
}
