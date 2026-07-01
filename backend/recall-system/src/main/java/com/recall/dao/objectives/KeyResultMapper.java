package com.recall.dao.objectives;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.objectives.KeyResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 关键成果 K Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface KeyResultMapper extends BaseMapper<KeyResult> {

    /**
     * 校验同一目标下关键成果名称是否已存在（名称唯一校验，月度绩效）。
     *
     * @param objectiveId 归属目标 ID
     * @param name        关键成果名称
     * @param excludeId   排除的关键成果 ID（编辑改名时传当前 id 排除自身，新增时传 null）
     * @return 是否存在重名
     */
    boolean existsByName(@Param("objectiveId") Long objectiveId,
                         @Param("name") String name,
                         @Param("excludeId") Long excludeId);
}

