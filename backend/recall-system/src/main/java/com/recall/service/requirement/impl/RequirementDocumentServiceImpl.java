package com.recall.service.requirement.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.requirement.RequirementDocumentMapper;
import com.recall.dto.requirement.MeetingMinutesQuickCreateReq;
import com.recall.dto.requirement.MeetingMinutesSaveReq;
import com.recall.dto.requirement.RequirementCreateReq;
import com.recall.dto.requirement.RequirementDocumentCreateReq;
import com.recall.entity.requirement.RequirementDocument;
import com.recall.enums.RequirementDocumentType;
import com.recall.service.requirement.RequirementDocumentService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.RequirementDocumentVO;
import com.recall.vo.requirement.RequirementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 需求文档 Service 实现，管理 requirement_documents 表的数据访问。
 * <p>
 * 持有 RequirementDocumentMapper；校验需求归属走 {@link RequirementService}（不直接注入 RequirementMapper）。
 *
 * @author recall
 */
@Slf4j
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

    // ===================== 会议纪要池 =====================

    @Override
    public List<RequirementDocumentVO> listUnboundMinutes() {
        List<RequirementDocument> docs = requirementDocumentMapper.selectList(
                new LambdaQueryWrapper<RequirementDocument>()
                        .eq(RequirementDocument::getUserId, UserContextHolder.requireUserId())
                        .eq(RequirementDocument::getType, RequirementDocumentType.MEETING.getValue())
                        .isNull(RequirementDocument::getRequirementId)
                        .orderByDesc(RequirementDocument::getDocumentDate)
                        .orderByDesc(RequirementDocument::getId));
        return docs.stream().map(this::toVO).toList();
    }

    @Override
    public RequirementDocumentVO upsertPushedMinute(Long userId, MeetingMinutesSaveReq req, LocalDate defaultDate) {
        RequirementDocument existing = requirementDocumentMapper.selectOne(
                new LambdaQueryWrapper<RequirementDocument>()
                        .eq(RequirementDocument::getUserId, userId)
                        .eq(RequirementDocument::getType, RequirementDocumentType.MEETING.getValue())
                        .eq(RequirementDocument::getUrl, req.getUrl().trim())
                        .last("LIMIT 1"));
        if (existing == null) {
            RequirementDocument doc = new RequirementDocument();
            doc.setUserId(userId);
            doc.setRequirementId(null);
            doc.setType(RequirementDocumentType.MEETING.getValue());
            doc.setTitle(req.getTitle().trim());
            doc.setUrl(req.getUrl().trim());
            doc.setDocumentDate(req.getMeetingDate() != null ? req.getMeetingDate() : defaultDate);
            requirementDocumentMapper.insert(doc);
            log.info("会议纪要推送入库: userId={}, docId={}, title={}", userId, doc.getId(), doc.getTitle());
            return toVO(doc);
        }
        // 幂等更新：标题/日期以最新推送为准，绑定关系不动
        existing.setTitle(req.getTitle().trim());
        if (req.getMeetingDate() != null) {
            existing.setDocumentDate(req.getMeetingDate());
        }
        requirementDocumentMapper.updateById(existing);
        log.info("会议纪要推送幂等更新: userId={}, docId={}", userId, existing.getId());
        return toVO(existing);
    }

    @Override
    public void bindMinute(Long documentId, Long requirementId) {
        RequirementDocument doc = loadOwnedMinute(documentId);
        requirementService.checkOwned(requirementId);
        doc.setRequirementId(requirementId);
        requirementDocumentMapper.updateById(doc);
    }

    @Override
    public void unbindMinute(Long documentId) {
        RequirementDocument doc = loadOwnedMinute(documentId);
        doc.setRequirementId(null);
        requirementDocumentMapper.updateById(doc);
    }

    @Override
    public void updateMinuteTitle(Long documentId, String title) {
        RequirementDocument doc = loadOwnedMinute(documentId);
        doc.setTitle(title.trim());
        requirementDocumentMapper.updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequirementVO createRequirementAndBindMinute(Long documentId, MeetingMinutesQuickCreateReq req) {
        RequirementDocument doc = loadOwnedMinute(documentId);
        // 复用需求创建：标题唯一、分类归属校验、初始状态讨论中
        RequirementCreateReq createReq = new RequirementCreateReq();
        createReq.setTitle(req.getTitle().trim());
        createReq.setCategoryId(req.getCategoryId());
        createReq.setSubCategoryId(req.getSubCategoryId());
        // 首次需求时间 = 纪要的会议日期（需求在会议上首次提出），而非创建当天
        createReq.setFirstDemandDate(doc.getDocumentDate() != null ? doc.getDocumentDate() : LocalDate.now());
        RequirementVO created = requirementService.create(createReq);
        doc.setRequirementId(created.getId());
        requirementDocumentMapper.updateById(doc);
        log.info("会议纪要快捷新建需求并绑定: docId={}, requirementId={}", documentId, created.getId());
        return created;
    }

    /** 加载当前用户的会议纪要（type=3），越权/不存在/类型不符统一 404 */
    private RequirementDocument loadOwnedMinute(Long documentId) {
        RequirementDocument doc = loadOwned(documentId);
        if (!RequirementDocumentType.MEETING.getValue().equals(doc.getType())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会议纪要不存在或无权访问");
        }
        return doc;
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
