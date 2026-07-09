package com.recall.dao.daily;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.daily.DailyReportItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日报工作内容项 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface DailyReportItemMapper extends BaseMapper<DailyReportItem> {
}
