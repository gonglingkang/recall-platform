package com.recall.dao.sprint;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.sprint.SprintKeyResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 冲刺-关键成果关联 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface SprintKeyResultMapper extends BaseMapper<SprintKeyResult> {

    /**
     * 查询关联了指定关键成果的所有冲刺任务 ID（K 状态变更时反查用）。
     *
     * @param keyResultId 关键成果 ID
     * @return 冲刺任务 ID 列表
     */
    List<Long> selectSprintIdsByKeyResultId(@Param("keyResultId") Long keyResultId);
}
