package com.recall.dao.objectives;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.objectives.Objective;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 目标 O Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface ObjectiveMapper extends BaseMapper<Objective> {

    /**
     * 校验同用户同月目标名称是否已存在（名称唯一校验，PRD 月度绩效）。
     * <p>
     * userId 显式传入做数据隔离双保险。
     *
     * @param userId     用户 ID
     * @param month      月份 YYYY-MM
     * @param name       目标名称
     * @param excludeId  排除的目标 ID（编辑改名时传当前 id 排除自身，新增时传 null）
     * @return 是否存在重名
     */
    boolean existsByName(@Param("userId") Long userId,
                         @Param("month") String month,
                         @Param("name") String name,
                         @Param("excludeId") Long excludeId);
}

