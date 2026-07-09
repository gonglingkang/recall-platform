package com.recall.dao.daily;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.daily.DailyReportItemTodo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日报项-待办关联 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface DailyReportItemTodoMapper extends BaseMapper<DailyReportItemTodo> {
}
