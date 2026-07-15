package com.recall.dao.requirement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.requirement.Requirement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 需求 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface RequirementMapper extends BaseMapper<Requirement> {

    /**
     * 校验同用户需求标题是否已存在（标题唯一校验）。
     *
     * @param userId     用户 ID
     * @param title      需求标题
     * @param excludeId  排除的需求 ID（编辑改名时传当前 id 排除自身，新增时传 null）
     * @return 是否存在重名
     */
    boolean existsByTitle(@Param("userId") Long userId,
                          @Param("title") String title,
                          @Param("excludeId") Long excludeId);
}
