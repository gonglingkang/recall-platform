package com.recall.service.requirement;

import com.recall.dto.requirement.RequirementCategoryCreateReq;
import com.recall.dto.requirement.RequirementCategoryUpdateReq;
import com.recall.entity.requirement.RequirementCategory;
import com.recall.vo.requirement.RequirementCategoryVO;

import java.util.List;

/**
 * 需求分类 Service（两级，单表 + parentId）。
 * <p>
 * 与待办分类(category 域)完全独立。parentId 为 null=主分类，非空=子分类。
 * 应用层控制最多 2 层。所有操作强制按当前用户隔离。
 * <p>
 * 删除规则：有子分类拒绝；有需求占用拒绝（不自动迁移）。
 *
 * @author recall
 */
public interface RequirementCategoryService {

    /**
     * 查询分类树（两级）。
     *
     * @return 当前用户的需求分类树
     */
    List<RequirementCategoryVO> listTree();

    /**
     * 创建分类（主分类或子分类，由 req.parentId 区分）。
     *
     * @param req 创建请求
     * @return 创建后的分类详情
     */
    RequirementCategoryVO createCategory(RequirementCategoryCreateReq req);

    /**
     * 编辑分类（parentId 不可变更，color 仅主分类可改）。
     *
     * @param id  分类 ID
     * @param req 编辑请求
     * @return 编辑后的分类详情
     */
    RequirementCategoryVO updateCategory(Long id, RequirementCategoryUpdateReq req);

    /**
     * 删除分类（有子分类或下有需求均拒绝）。
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
    RequirementCategory getById(Long id, boolean checkOwnership);

    /**
     * 校验主分类 + 子分类的合法性（供需求创建/编辑时调用）。
     * <p>
     * categoryId 必须是当前用户的主分类（parentId=null）；subCategoryId 非空时
     * 必须是 categoryId 的子分类。
     *
     * @param categoryId     主分类 ID（必填）
     * @param subCategoryId  子分类 ID（可空）
     */
    void validateCategoryBinding(Long categoryId, Long subCategoryId);

    /**
     * 查询指定主分类下的所有子分类 ID（供需求按主分类筛选时联动子分类）。
     *
     * @param parentId 主分类 ID
     * @return 子分类 ID 列表；无子分类返回空列表
     */
    List<Long> listSubCategoryIds(Long parentId);
}
