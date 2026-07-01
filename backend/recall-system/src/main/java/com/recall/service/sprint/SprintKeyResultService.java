package com.recall.service.sprint;

import com.recall.entity.sprint.SprintKeyResult;

import java.util.List;

/**
 * 冲刺-关键成果关联 Service，管理 sprint_key_results 关联表的数据访问。
 * <p>
 * 其他 Service（如 SprintService）操作关联数据须经本接口，不直接注入 SprintKeyResultMapper。
 *
 * @author recall
 */
public interface SprintKeyResultService {

    /**
     * 批量查询多个冲刺的关联记录。
     *
     * @param sprintIds 冲刺 ID 列表
     * @return 关联记录列表
     */
    List<SprintKeyResult> listBySprintIds(List<Long> sprintIds);

    /**
     * 批量查询多个关键成果的关联记录（供绩效列表按 K 反查关联冲刺用）。
     *
     * @param keyResultIds 关键成果 ID 列表；为空返回空列表
     * @return 关联记录列表
     */
    List<SprintKeyResult> listByKeyResultIds(List<Long> keyResultIds);

    /**
     * 查询指定冲刺关联的关键成果 ID 列表。
     *
     * @param sprintId 冲刺 ID
     * @return 关键成果 ID 列表
     */
    List<Long> listKeyResultIdsBySprintId(Long sprintId);

    /**
     * 查询关联了指定关键成果的所有冲刺 ID（K 状态变更时反查用）。
     *
     * @param keyResultId 关键成果 ID
     * @return 冲刺 ID 列表
     */
    List<Long> listSprintIdsByKeyResultId(Long keyResultId);

    /**
     * 批量新增关联记录。
     *
     * @param sprintId    冲刺 ID
     * @param keyResultIds 关键成果 ID 列表
     */
    void saveAll(Long sprintId, List<Long> keyResultIds);

    /**
     * 删除指定冲刺的全部关联记录。
     *
     * @param sprintId 冲刺 ID
     */
    void deleteBySprintId(Long sprintId);

    /**
     * 删除指定关键成果的全部关联记录（K 删除时清理孤儿关联用）。
     *
     * @param keyResultId 关键成果 ID
     */
    void deleteByKeyResultId(Long keyResultId);

    /**
     * 批量删除多个关键成果的全部关联记录（删 O 级联删 K 时清理用，避免 N 次调用）。
     *
     * @param keyResultIds 关键成果 ID 列表；为空不执行
     */
    void deleteByKeyResultIds(List<Long> keyResultIds);
}
