package com.recall.service.category;

import com.recall.dto.category.CategoryCreateReq;
import com.recall.dto.category.CategoryUpdateReq;
import com.recall.entity.category.Category;
import com.recall.vo.category.CategoryVO;

import java.util.List;

import java.util.List;

/**
 * 分类 Service（PRD 6.4 / 11.3）。
 * <p>
 * 两层分类合并单表，用 parentId 表达层级；应用层控制最多 2 层。
 * 所有操作强制按当前用户隔离。
 * <p>
 * 删除规则：大分类（parentId=null）不可删除（待办必须关联分类）；
 * 子分类删除时其下待办迁移到父大分类。
 *
 * @author recall
 */
public interface CategoryService {

    /**
     * 查询分类树（两层，PRD 11.3 GET /api/categories）。
     *
     * @return 当前用户的分类树
     */
    List<CategoryVO> listTree();

    /**
     * 创建分类（大分类或子分类，由 req.parentId 区分）。
     *
     * @param req 创建请求
     * @return 创建后的分类详情
     */
    CategoryVO createCategory(CategoryCreateReq req);

    /**
     * 编辑分类（parentId 不可变更，color 仅大分类可改）。
     *
     * @param id  分类 ID
     * @param req 编辑请求
     * @return 编辑后的分类详情
     */
    CategoryVO updateCategory(Long id, CategoryUpdateReq req);

    /**
     * 删除分类（层级无关）。
     * <p>
     * 任意节点删除前，其下有子分类一律拒绝；无子分类时：
     * 根节点（无父）须无直接挂载待办才删，非根节点（有父）把待办迁移到父分类后删除。
     *
     * @param id 分类 ID
     */
    void deleteCategory(Long id);

    /**
     * 按 ID 加载分类（供 Service 间内部调用，entity 不透传出 Service 层）。
     *
     * @param id             分类 ID
     * @param checkOwnership 是否校验归属当前用户；为 true 时查不到或不属于当前用户均抛 404
     * @return 分类实体；checkOwnership=false 且查不到时返回 null
     */
    Category getById(Long id, boolean checkOwnership);

    /**
     * 查询指定分类及其所有子分类的 ID 列表（供待办按分类筛选时联动子分类）。
     * <p>
     * 若 id 为大分类（parentId=null），返回自身 + 其下所有子分类 ID；
     * 若 id 为子分类，返回自身（无子分类）。
     *
     * @param id 分类 ID
     * @return 包含自身及子分类的 ID 列表
     */
    List<Long> listCategoryAndSubIds(Long id);
}
