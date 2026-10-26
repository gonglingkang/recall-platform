package com.recall.dao.requirement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.requirement.RequirementPrep;
import org.apache.ibatis.annotations.Mapper;

/**
 * 需求上线准备项 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface RequirementPrepMapper extends BaseMapper<RequirementPrep> {
}
