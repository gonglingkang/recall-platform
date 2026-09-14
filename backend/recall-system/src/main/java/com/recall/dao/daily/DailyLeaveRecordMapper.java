package com.recall.dao.daily;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.daily.DailyLeaveRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日报请假记录 Mapper，归 {@link com.recall.service.daily.DailyLeaveService} 专属管理。
 *
 * @author recall
 */
@Mapper
public interface DailyLeaveRecordMapper extends BaseMapper<DailyLeaveRecord> {
}
