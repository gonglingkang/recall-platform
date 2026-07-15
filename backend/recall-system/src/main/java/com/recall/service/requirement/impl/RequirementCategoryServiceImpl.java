package com.recall.service.requirement.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.requirement.RequirementCategoryMapper;
import com.recall.dto.requirement.RequirementCategoryCreateReq;
import com.recall.dto.requirement.RequirementCategoryUpdateReq;
import com.recall.entity.requirement.RequirementCategory;
import com.recall.service.requirement.RequirementCategoryService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.RequirementCategoryVO;
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
 * 需求分类 Service 实现（两级，单表 + parentId）。
 * <p>
 * 持有 RequirementCategoryMapper，负责需求分类的增删改查。
 * parentId 为 null=主分类，非空=子分类；应用层控制最多 2 层。
 * 名称唯一性：同用户同父下名称唯一，由 DB 唯一键 uk_user_parent_name 兜底。
 * color 仅主分类可用，子分类强制 null。
 * <p>
 * 删除规则：有子分类拒绝；有需求占用拒绝（主分类查 category_id，子分类查 sub_category_id）。
 * 需求占用查询通过 RequirementService 接口（不直接注入 RequirementMapper）。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementCategoryServiceImpl implements RequirementCategoryService {

    private final RequirementCategoryMapper requirementCategoryMapper;
    /** @Lazy 打破与 RequirementServiceImpl 的循环依赖 */
    @Lazy
    private final RequirementService requirementService;

    // ===================== 查询 =====================

    @Override
    public List<RequirementCategoryVO> listTree() {
        Long userId = UserContextHolder.requireUserId();
        List<RequirementCategory> roots = requirementCategoryMapper.selectList(
                new LambdaQueryWrapper<RequirementCategory>()
                        .eq(RequirementCategory::getUserId, userId)
                        .isNull(RequirementCategory::getParentId)
                        .orderByAsc(RequirementCategory::getSortOrder)
                        .orderByAsc(RequirementCategory::getId));
        if (roots.isEmpty()) {
            return Collections.emptyList();
        }
        // 子分类一次性查出，按 parentId 分组
        List<Long> rootIds = roots.stream().map(RequirementCategory::getId).toList();
        List<RequirementCategory> subs = requirementCategoryMapper.selectList(
                new LambdaQueryWrapper<RequirementCategory>()
                        .eq(RequirementCategory::getUserId, userId)
                        .in(RequirementCategory::getParentId, rootIds)
                        .orderByAsc(RequirementCategory::getSortOrder)
                        .orderByAsc(RequirementCategory::getId));
        Map<Long, List<RequirementCategoryVO>> subMap = subs.stream()
                .collect(Collectors.groupingBy(RequirementCategory::getParentId,
                        Collectors.mapping(this::toVO, Collectors.toList())));
        return roots.stream()
                .map(r -> toVO(r, subMap.getOrDefault(r.getId(), Collections.emptyList())))
                .toList();
    }

    // ===================== 写操作 =====================

    @Override
    public RequirementCategoryVO createCategory(RequirementCategoryCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        Long parentId = req.getParentId();
        String color = null;
        if (parentId == null) {
            // 创建主分类：允许设置 color
            color = req.getColor();
        } else {
            // 创建子分类：校验 2 层约束--parent 必须存在、归属当前用户、且 parent 本身是主分类
            RequirementCategory parent = loadOwned(parentId);
            if (parent.getParentId() != null) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "最多两级分类，子分类下不可再建子分类");
            }
            // 子分类强制 color 为 null（忽略前端传入）
        }
        // 同用户同父下名称唯一（DB 唯一键兜底，这里提前校验给友好提示）
        if (existsByName(userId, parentId, req.getName(), null)) {
            throw new BusinessException(ResultCode.REQUIREMENT_CATEGORY_NAME_DUPLICATED, req.getName());
        }
        RequirementCategory c = new RequirementCategory();
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setName(req.getName());
        c.setColor(color);
        c.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        requirementCategoryMapper.insert(c);
        return toVO(c, Collections.emptyList());
    }

    @Override
    public RequirementCategoryVO updateCategory(Long id, RequirementCategoryUpdateReq req) {
        RequirementCategory c = loadOwned(id);
        boolean isRoot = c.getParentId() == null;
        // color 仅主分类可改
        if (req.getColor() != null && !isRoot) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "子分类不支持设置颜色");
        }
        // 名称变更需校验同父唯一
        if (!req.getName().equals(c.getName())
                && existsByName(c.getUserId(), c.getParentId(), req.getName(), id)) {
            throw new BusinessException(ResultCode.REQUIREMENT_CATEGORY_NAME_DUPLICATED, req.getName());
        }
        c.setName(req.getName());
        if (req.getColor() != null) c.setColor(req.getColor());
        if (req.getSortOrder() != null) c.setSortOrder(req.getSortOrder());
        requirementCategoryMapper.updateById(c);
        return toVO(c, null);
    }

    /**
     * 删除分类（有子分类或下有需求均拒绝）。
     * <p>单条写（deleteById）-> 不加事务。
     */
    @Override
    public void deleteCategory(Long id) {
        RequirementCategory c = loadOwned(id);
        // 有子分类一律拒绝
        boolean hasSub = requirementCategoryMapper.exists(new LambdaQueryWrapper<RequirementCategory>()
                .eq(RequirementCategory::getParentId, id));
        if (hasSub) {
            throw new BusinessException(ResultCode.REQUIREMENT_CATEGORY_HAS_SUB);
        }
        // 有需求占用拒绝（主分类查 category_id，子分类查 sub_category_id）
        if (c.getParentId() == null) {
            if (requirementService.existsByCategory(id)) {
                throw new BusinessException(ResultCode.REQUIREMENT_CATEGORY_HAS_REQUIREMENT);
            }
        } else {
            if (requirementService.existsBySubCategory(id)) {
                throw new BusinessException(ResultCode.REQUIREMENT_CATEGORY_HAS_REQUIREMENT);
            }
        }
        requirementCategoryMapper.deleteById(id);
        log.info("删除需求分类 id={}", id);
    }

    // ===================== 辅助 =====================

    @Override
    public RequirementCategory getById(Long id, boolean checkOwnership) {
        if (checkOwnership) {
            return loadOwned(id);
        }
        return requirementCategoryMapper.selectById(id);
    }

    /**
     * 校验主分类 + 子分类的合法性（供需求创建/编辑时调用）。
     * <p>categoryId 必须是当前用户的主分类；subCategoryId 非空时必须是 categoryId 的子分类。
     */
    @Override
    public void validateCategoryBinding(Long categoryId, Long subCategoryId) {
        if (categoryId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "需求主分类不能为空");
        }
        RequirementCategory main = loadOwned(categoryId);
        if (main.getParentId() != null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "需求主分类必须是主分类，不能是子分类");
        }
        if (subCategoryId != null) {
            RequirementCategory sub = loadOwned(subCategoryId);
            if (sub.getParentId() == null) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "需求子分类不能是主分类");
            }
            if (!categoryId.equals(sub.getParentId())) {
                throw new BusinessException(ResultCode.REQUIREMENT_SUB_CATEGORY_PARENT_MISMATCH);
            }
        }
    }

    /** 同用户同父下名称是否已存在（编辑时排除自身 id） */
    private boolean existsByName(Long userId, Long parentId, String name, Long excludeId) {
        return requirementCategoryMapper.exists(new LambdaQueryWrapper<RequirementCategory>()
                .eq(RequirementCategory::getUserId, userId)
                .eq(parentId != null, RequirementCategory::getParentId, parentId)
                .isNull(parentId == null, RequirementCategory::getParentId)
                .eq(RequirementCategory::getName, name)
                .ne(excludeId != null, RequirementCategory::getId, excludeId));
    }

    @Override
    public List<Long> listSubCategoryIds(Long parentId) {
        return requirementCategoryMapper.selectList(new LambdaQueryWrapper<RequirementCategory>()
                        .eq(RequirementCategory::getParentId, parentId))
                .stream().map(RequirementCategory::getId).toList();
    }

    /** 加载当前用户的分类，越权/不存在抛 404 */
    private RequirementCategory loadOwned(Long id) {
        RequirementCategory c = requirementCategoryMapper.selectById(id);
        if (c == null || !UserContextHolder.requireUserId().equals(c.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求分类不存在或无权访问");
        }
        return c;
    }

    private RequirementCategoryVO toVO(RequirementCategory c, List<RequirementCategoryVO> subs) {
        RequirementCategoryVO vo = RequirementCategoryVO.builder()
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

    private RequirementCategoryVO toVO(RequirementCategory c) {
        return toVO(c, Collections.emptyList());
    }
}
