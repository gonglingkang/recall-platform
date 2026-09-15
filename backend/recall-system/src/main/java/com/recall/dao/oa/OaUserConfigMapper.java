package com.recall.dao.oa;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.oa.OaUserConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 对接配置 Mapper，归 {@link com.recall.service.oa.OaConfigService} 专属管理。
 *
 * @author recall
 */
@Mapper
public interface OaUserConfigMapper extends BaseMapper<OaUserConfig> {
}
