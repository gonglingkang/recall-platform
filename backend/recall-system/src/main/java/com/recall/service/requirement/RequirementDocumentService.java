package com.recall.service.requirement;

import com.recall.dto.requirement.RequirementDocumentCreateReq;
import com.recall.vo.requirement.RequirementDocumentVO;

import java.util.List;

/**
 * 需求文档 Service，管理 requirement_documents 表的数据访问。
 * <p>
 * 文档归属需求；新增/删除均校验需求存在且属于当前用户（越权统一 404）。
 *
 * @author recall
 */
public interface RequirementDocumentService {

    /**
     * 新增需求文档。
     *
     * @param requirementId 归属需求 ID
     * @param req           创建请求
     * @return 新建的文档
     */
    RequirementDocumentVO create(Long requirementId, RequirementDocumentCreateReq req);

    /**
     * 删除需求文档。
     *
     * @param documentId 文档 ID
     */
    void delete(Long documentId);

    /**
     * 查询指定需求下的全部文档（供 RequirementService 详情填充用）。
     *
     * @param requirementId 需求 ID
     * @return 文档 VO 列表
     */
    List<RequirementDocumentVO> listByRequirement(Long requirementId);

    /**
     * 删除指定需求下的全部文档（供删需求时级联调用）。
     *
     * @param requirementId 需求 ID
     */
    void deleteByRequirement(Long requirementId);
}
