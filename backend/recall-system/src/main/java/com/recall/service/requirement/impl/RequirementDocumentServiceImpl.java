package com.recall.service.requirement.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.requirement.RequirementDocumentMapper;
import com.recall.dto.requirement.RequirementDocumentCreateReq;
import com.recall.entity.requirement.RequirementDocument;
import com.recall.service.requirement.RequirementDocumentService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.RequirementDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 需求文档 Service 实现，管理 requirement_documents 表的数据访问。
 * <p>
 * 持有 RequirementDocumentMapper；校验需求归属走 {@link RequirementService}（不直接注入 RequirementMapper）。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class RequirementDocumentServiceImpl implements RequirementDocumentService {

    private final RequirementDocumentMapper requirementDocumentMapper;
    @Lazy
    private final RequirementService requirementService;

    @Override
    public RequirementDocumentVO create(Long requirementId, RequirementDocumentCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        // 校验需求存在且属于当前用户（越权统一 404）
        requirementService.checkOwned(requirementId);
        RequirementDocument doc = new RequirementDocument();
        doc.setUserId(userId);
        doc.setRequirementId(requirementId);
        doc.setType(req.getType().getValue());
        doc.setTitle(req.getTitle());
        doc.setUrl(req.getUrl());
        doc.setDocumentDate(req.getDocumentDate());
        requirementDocumentMapper.insert(doc);
        return toVO(doc);
    }

    @Override
    public void delete(Long documentId) {
        loadOwned(documentId);
        requirementDocumentMapper.deleteById(documentId);
    }

    @Override
    public List<RequirementDocumentVO> listByRequirement(Long requirementId) {
        List<RequirementDocument> docs = requirementDocumentMapper.selectList(
                new LambdaQueryWrapper<RequirementDocument>()
                        .eq(RequirementDocument::getRequirementId, requirementId)
                        .orderByAsc(RequirementDocument::getId));
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }
        return docs.stream().map(this::toVO).toList();
    }

    @Override
    public void deleteByRequirement(Long requirementId) {
        requirementDocumentMapper.delete(new LambdaQueryWrapper<RequirementDocument>()
                .eq(RequirementDocument::getRequirementId, requirementId));
    }

    // ===================== 辅助 =====================

    /** 加载当前用户的文档，越权/不存在抛 404 */
    private RequirementDocument loadOwned(Long documentId) {
        RequirementDocument doc = requirementDocumentMapper.selectById(documentId);
        if (doc == null || !UserContextHolder.requireUserId().equals(doc.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求文档不存在或无权访问");
        }
        return doc;
    }

    private RequirementDocumentVO toVO(RequirementDocument doc) {
        return RequirementDocumentVO.builder()
                .id(doc.getId())
                .requirementId(doc.getRequirementId())
                .type(doc.getType())
                .title(doc.getTitle())
                .url(doc.getUrl())
                .documentDate(doc.getDocumentDate())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
