package com.recall.dao.stats;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.todo.Todo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统计 Mapper（DAO 层）。
 * <p>
 * 继承 BaseMapper 复用基础查询；复杂聚合 SQL 后续可在对应 XML 中补充。
 *
 * @author recall
 */
@Mapper
public interface StatsMapper extends BaseMapper<Todo> {
}
