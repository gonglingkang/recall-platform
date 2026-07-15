package com.recall.dao.requirement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.requirement.RequirementCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 需求分类 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface RequirementCategoryMapper extends BaseMapper<RequirementCategory> {
}
