package com.recall.dao.category;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.category.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 大分类 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
