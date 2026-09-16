package com.recall.dao.daily;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.daily.DailyAttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤打卡记录 Mapper，归 {@link com.recall.service.daily.DailyAttendanceService} 专属管理。
 *
 * @author recall
 */
@Mapper
public interface DailyAttendanceRecordMapper extends BaseMapper<DailyAttendanceRecord> {
}
