package com.recall.dao.todo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.todo.TodoTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周期待办模板 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface TodoTemplateMapper extends BaseMapper<TodoTemplate> {
}
