package com.recall.dao.daily;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.daily.DailyReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日报主表 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface DailyReportMapper extends BaseMapper<DailyReport> {
}
