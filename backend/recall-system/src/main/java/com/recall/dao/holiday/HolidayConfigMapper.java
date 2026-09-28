package com.recall.dao.holiday;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.holiday.HolidayConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节假日配置 Mapper，归 {@link com.recall.service.holiday.HolidayService} 专属管理。
 *
 * @author recall
 */
@Mapper
public interface HolidayConfigMapper extends BaseMapper<HolidayConfig> {
}
