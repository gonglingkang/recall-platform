package com.recall.service.objectives;

import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.KeyResultRecordsUpdateReq;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.objectives.KeyResultUpdateReq;
import com.recall.entity.objectives.KeyResult;
import com.recall.vo.objectives.KeyResultVO;

import java.util.List;

/**
 * 关键成果 K Service（v2.0）。
 * <p>
 * 持有 KeyResultMapper，负责 K 的增删改查、状态切换与 completeDate 维护。
 * 状态切换是改变 K 进度的唯一入口。
 *
 * @author recall
 */
public interface KeyResultService {

    /**
     * 新增关键成果 K（objectiveId 由请求体传入，指定归属目标）。
     * <p>名称在同一目标下唯一，冲突抛 409。
     * <p>计划完成时间：用户传了用用户的；未传时已完成 → 当天，未开始/进行中 → 目标月份月底最后一天。
     *
     * @param req 创建请求（含 objectiveId）
     * @return 创建后的关键成果
     */
    KeyResultVO create(KeyResultCreateReq req);

    /**
     * 编辑关键成果 K（name/description/planCompleteDate，不改状态）。
     * <p>名称改为同一目标下另一 K 同名时抛 409；已完成的 K 仅可改名称与描述，
     * 传 planCompleteDate 抛 409。
     *
     * @param id  关键成果 ID
     * @param req 编辑请求
     * @return 更新后的关键成果
     */
    KeyResultVO update(Long id, KeyResultUpdateReq req);

    /**
     * 切换关键成果 K 的状态（改变进度的唯一入口，completeDate 后端自动维护）。
     *
     * @param id  关键成果 ID
     * @param req 状态请求
     * @return 更新后的关键成果
     */
    KeyResultVO changeStatus(Long id, KeyResultStatusReq req);

    /**
     * 全量更新关键成果的成果记录 R（不改 K 状态，仅覆盖 R）。
     * <p>传了覆盖旧 R，传空清空旧 R。
     *
     * @param id  关键成果 ID
     * @param req 全量更新请求
     * @return 更新后的关键成果（含最新 R 列表）
     */
    KeyResultVO updateRecords(Long id, KeyResultRecordsUpdateReq req);

    /**
     * 删除关键成果 K。
     * <p>连带清理 sprint_key_results 关联表，并触发受影响冲刺任务状态重算。
     *
     * @param id 关键成果 ID
     */
    void delete(Long id);

    /**
     * 按 id 查询关键成果实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     *
     * @param id             关键成果 ID
     * @param checkOwnership 是否校验归属当前用户；为 true 时查不到或不属于当前用户均抛 404
     * @return 关键成果实体；checkOwnership=false 且查不到时返回 null
     */
    KeyResult getById(Long id, boolean checkOwnership);

    /**
     * 按 id 批量查询关键成果实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     * <p>checkOwnership=true 时，任一 id 不存在或不属于当前用户均抛 404（越权统一 404，不暴露存在性）。
     * 返回顺序与传入 ids 无关，调用方按需自行映射。
     *
     * @param ids            关键成果 ID 列表；为空返回空列表
     * @param checkOwnership 是否校验归属当前用户
     * @return 关键成果实体列表
     */
    List<KeyResult> listByIds(List<Long> ids, boolean checkOwnership);

    /**
     * 查询指定目标 O 下的所有关键成果（供 ObjectiveService 派生计算用）。
     *
     * @param objectiveId 目标 ID
     * @return 关键成果实体列表
     */
    List<KeyResult> listByObjective(Long objectiveId);

    /**
     * 查询多个目标 O 下的所有关键成果（供 ObjectiveService 列表派生计算用）。
     *
     * @param objectiveIds 目标 ID 列表
     * @return 关键成果实体列表
     */
    List<KeyResult> listByObjectives(List<Long> objectiveIds);

    /**
     * 删除指定目标 O 下的所有关键成果（供 ObjectiveService 删除 O 时连带调用）。
     * <p>连带清理这些 K 在 sprint_key_results 的关联记录，并触发受影响冲刺任务状态重算。
     *
     * @param objectiveId 目标 ID
     * @return 删除条数
     */
    int deleteByObjective(Long objectiveId);
}
