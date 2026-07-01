package com.recall.dao.todo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.todo.Todo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface TodoMapper extends BaseMapper<Todo> {
}
