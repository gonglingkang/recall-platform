package com.recall.service.requirement;

import com.recall.common.api.PageResp;
import com.recall.dto.requirement.RequirementCreateReq;
import com.recall.dto.requirement.RequirementPageReq;
import com.recall.dto.requirement.RequirementStatusReq;
import com.recall.dto.requirement.RequirementUpdateReq;
import com.recall.enums.KeyResultStatus;
import com.recall.vo.requirement.RequirementVO;

import java.util.List;

/**
 * 需求 Service。
 * <p>
 * 持有 RequirementMapper，负责需求的增删改查、状态流转与关键成果 K 绑定。
 * 绑 K 时，讨论中/进行中/开发完成三态由 K 状态映射驱动；未绑 K 时手动维护。
 * 进入不涉及/验收完成/发布完成时自动解绑 K，之后 K 不再驱动。
 *
 * @author recall
 */
public interface RequirementService {

    /**
     * 需求分页查询，支持关键字、创建时间范围、状态过滤。
     *
     * @param req 分页查询请求
     * @return 分页结果（列表项仅基础字段，不含文档与 K 详情）
     */
    PageResp<RequirementVO> page(RequirementPageReq req);

    /**
     * 需求详情（含文档列表与绑定的 K 摘要）。
     *
     * @param id 需求 ID
     * @return 需求详情
     */
    RequirementVO getById(Long id);

    /**
     * 新建需求（初始状态为讨论中）。
     * <p>同用户标题唯一，冲突抛 4701。
     * 可选传入 keyResultId 绑定 K（绑定后状态跟 K 走）。
     *
     * @param req 创建请求
     * @return 新建的需求
     */
    RequirementVO create(RequirementCreateReq req);

    /**
     * 编辑需求（title/description/keyResultId）。
     * <p>发布完成（终态）的需求禁止编辑。
     * keyResultId 传值则绑定该 K（与当前不同先解绑再绑），不传/null 则保持不变。
     *
     * @param id  需求 ID
     * @param req 编辑请求
     * @return 更新后的需求
     */
    RequirementVO update(Long id, RequirementUpdateReq req);

    /**
     * 手动切换需求状态。
     * <p>绑 K 时，讨论中/进行中/开发完成三态由 K 驱动，禁止手动改这三态；
     * 手动只能进入不涉及/验收完成/发布完成，进入时自动解绑 K。
     * 发布完成为终态，不可再变更。
     *
     * @param id  需求 ID
     * @param req 状态请求
     * @return 更新后的需求
     */
    RequirementVO changeStatus(Long id, RequirementStatusReq req);

    /**
     * 删除需求（连带删除其下文档）。
     *
     * @param id 需求 ID
     */
    void delete(Long id);

    /**
     * 校验需求存在且属于当前用户（越权统一 404，不暴露存在性）。
     * <p>供 Service 间内部调用（如文档 Service 校验需求归属）。
     *
     * @param requirementId 需求 ID
     */
    void checkOwned(Long requirementId);

    /**
     * 关键成果 K 状态变更后同步所有绑定该 K 的需求状态（供 KeyResultService 调用）。
     * <p>一个 K 可被多个需求绑定，逐个同步。
     * K 状态映射：未开始->讨论中，进行中->进行中，已完成->开发完成，已取消->解绑回讨论中。
     * 仅需求处于 K 活跃态时生效；脱钩态（不涉及/验收完成/发布完成）不受影响。
     *
     * @param krId     发生状态变更的关键成果 ID
     * @param krStatus 关键成果新状态
     */
    void syncStatusByKeyResult(Long krId, KeyResultStatus krStatus);

    /**
     * 关键成果 K 被删除时，解绑所有绑定该 K 的需求；K 活跃态的需求回讨论中（供 KeyResultService 调用）。
     *
     * @param krId 被删除的关键成果 ID
     */
    void handleKeyResultDeleted(Long krId);

    /**
     * 批量关键成果 K 被删除时（级联删 O 时），解绑这些 K 的需求并回讨论中（供 KeyResultService 调用）。
     *
     * @param krIds 被删除的关键成果 ID 列表；为空不执行
     */
    void handleKeyResultsDeleted(List<Long> krIds);

    /**
     * 指定主分类下是否有需求（供 RequirementCategoryService 删除校验调用）。
     *
     * @param categoryId 主分类 ID
     * @return true 表示有需求占用该主分类
     */
    boolean existsByCategory(Long categoryId);

    /**
     * 指定子分类下是否有需求（供 RequirementCategoryService 删除校验调用）。
     *
     * @param subCategoryId 子分类 ID
     * @return true 表示有需求占用该子分类
     */
    boolean existsBySubCategory(Long subCategoryId);
}
