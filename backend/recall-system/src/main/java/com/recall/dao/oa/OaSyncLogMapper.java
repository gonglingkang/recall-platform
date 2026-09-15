package com.recall.dao.oa;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.oa.OaSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 同步日志 Mapper，归 {@link com.recall.service.oa.OaSyncService} 专属管理。
 *
 * @author recall
 */
@Mapper
public interface OaSyncLogMapper extends BaseMapper<OaSyncLog> {
}
