package com.recall.dao.requirement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recall.entity.requirement.RequirementDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 需求文档 Mapper（DAO 层）。
 *
 * @author recall
 */
@Mapper
public interface RequirementDocumentMapper extends BaseMapper<RequirementDocument> {
}
