package com.recall.service.requirement;

import com.recall.dto.requirement.RequirementPrepCreateReq;
import com.recall.dto.requirement.RequirementPrepUpdateReq;
import com.recall.vo.requirement.RequirementPrepVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 需求上线准备项 Service，管理 requirement_preps 表的数据访问。
 * <p>
 * 仅「开发完成/验收完成」状态的需求可配置（写入校验）；清单可为空。
 * 条目 = 名称 + 详细内容（SQL/脚本说明）+ 完成勾选。
 *
 * @author recall
 */
public interface RequirementPrepService {

    /** 单需求的上线准备进度统计 */
    record PrepStat(long total, long done) {
    }

    /**
     * 查询需求的上线准备清单（按排序、id 升序）。
     *
     * @param requirementId 需求 ID
     * @return 准备项列表（可为空）
     */
    List<RequirementPrepVO> listByRequirement(Long requirementId);

    /**
     * 按模板生成上线准备清单（仅当清单为空时允许，幂等保护）。
     *
     * @param requirementId 需求 ID
     * @return 生成后的清单
     */
    List<RequirementPrepVO> generateByTemplate(Long requirementId);

    /**
     * 手动新增准备项。
     *
     * @param requirementId 需求 ID
     * @param req           名称与内容
     * @return 新建的准备项
     */
    RequirementPrepVO create(Long requirementId, RequirementPrepCreateReq req);

    /**
     * 更新准备项（名称/内容/完成状态）。
     *
     * @param prepId 准备项 ID
     * @param req    更新请求
     * @return 更新后的准备项
     */
    RequirementPrepVO update(Long prepId, RequirementPrepUpdateReq req);

    /**
     * 删除准备项。
     *
     * @param prepId 准备项 ID
     */
    void delete(Long prepId);

    /**
     * 批量统计多个需求的准备进度（供需求列表卡片展示，避免 N+1 查询）。
     *
     * @param requirementIds 需求 ID 列表；为空返回空 map
     * @return requirementId -> 进度统计（无准备项的需求不出现在 map 中）
     */
    Map<Long, PrepStat> countByRequirementIds(Collection<Long> requirementIds);

    /**
     * 删除需求下全部准备项（删需求时级联调用）。
     *
     * @param requirementId 需求 ID
     */
    void deleteByRequirement(Long requirementId);
}
