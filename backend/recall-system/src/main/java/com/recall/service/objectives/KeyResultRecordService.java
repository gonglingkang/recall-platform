package com.recall.service.objectives;

import java.util.List;
import java.util.Map;

/**
 * 关键成果成果记录 R Service，管理 key_result_records 表的数据访问。
 * <p>
 * 其他 Service（如 KeyResultService）操作 R 数据须经本接口，不直接注入 KeyResultRecordMapper。
 * R 在 K 切换到「已完成」时随状态变更全量提交，也可通过 K 的全量更新接口单独覆盖。
 *
 * @author recall
 */
public interface KeyResultRecordService {

    /**
     * 查询指定关键成果的 R 内容列表（按 id 升序，即提交顺序）。
     *
     * @param keyResultId 关键成果 ID
     * @return R 内容列表
     */
    List<String> listContentsByKeyResultId(Long keyResultId);

    /**
     * 批量查询多个关键成果的 R 内容列表（供绩效列表批量加载用）。
     *
     * @param keyResultIds 关键成果 ID 列表；为空返回空 map
     * @return keyResultId -> R 内容列表
     */
    Map<Long, List<String>> listContentsByKeyResultIds(List<Long> keyResultIds);

    /**
     * 全量覆盖指定关键成果的 R：先删旧、再插新。
     * <p>
     * contents 为 null 或空列表时清空该 K 下全部 R。
     *
     * @param keyResultId 关键成果 ID
     * @param contents    R 内容列表；null 或空表示清空
     */
    void replaceByKeyResultId(Long keyResultId, List<String> contents);

    /**
     * 删除指定关键成果的全部 R（K 删除时级联调用）。
     *
     * @param keyResultId 关键成果 ID
     */
    void deleteByKeyResultId(Long keyResultId);

    /**
     * 批量删除多个关键成果的全部 R（删 O 级联删 K 时清理用，避免 N 次调用）。
     *
     * @param keyResultIds 关键成果 ID 列表；为空不执行
     */
    void deleteByKeyResultIds(List<Long> keyResultIds);
}
