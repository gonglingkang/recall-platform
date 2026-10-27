package com.recall.service.requirement.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.requirement.RequirementPrepMapper;
import com.recall.dto.requirement.RequirementPrepCreateReq;
import com.recall.dto.requirement.RequirementPrepUpdateReq;
import com.recall.entity.requirement.RequirementPrep;
import com.recall.enums.RequirementStatus;
import com.recall.service.requirement.RequirementPrepService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.RequirementPrepVO;
import com.recall.vo.requirement.RequirementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需求上线准备项 Service 实现。
 * <p>
 * 持有 RequirementPrepMapper；需求归属与状态校验走 {@link RequirementService}（@Lazy 防循环依赖）。
 * 写入仅限「开发完成/验收完成」状态；清单可为空（不做强制生成）。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class RequirementPrepServiceImpl implements RequirementPrepService {

    /** 可配置上线准备的需求状态：开发完成 / 验收完成 */
    private static final Set<String> CONFIGURABLE_STATUSES = Set.of(
            RequirementStatus.DEV_DONE.getValue(), RequirementStatus.ACCEPTANCE_DONE.getValue());

    private final RequirementPrepMapper requirementPrepMapper;
    @Lazy
    private final RequirementService requirementService;

    @Override
    public List<RequirementPrepVO> listByRequirement(Long requirementId) {
        requirementService.checkOwned(requirementId);
        return requirementPrepMapper.selectList(new LambdaQueryWrapper<RequirementPrep>()
                        .eq(RequirementPrep::getRequirementId, requirementId)
                        .orderByAsc(RequirementPrep::getSortOrder)
                        .orderByAsc(RequirementPrep::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public RequirementPrepVO create(Long requirementId, RequirementPrepCreateReq req) {
        RequirementVO requirement = requireConfigurable(requirementId);
        Long userId = UserContextHolder.requireUserId();
        Integer maxSort = requirementPrepMapper.selectList(new LambdaQueryWrapper<RequirementPrep>()
                        .eq(RequirementPrep::getRequirementId, requirement.getId())
                        .orderByDesc(RequirementPrep::getSortOrder)
                        .last("LIMIT 1"))
                .stream().map(RequirementPrep::getSortOrder).findFirst().orElse(-1);
        RequirementPrep prep = new RequirementPrep();
        prep.setUserId(userId);
        prep.setRequirementId(requirement.getId());
        prep.setTitle(req.getTitle().trim());
        prep.setContent(req.getContent() == null || req.getContent().isBlank() ? null : req.getContent().trim());
        prep.setDone(0);
        prep.setSortOrder(maxSort + 1);
        requirementPrepMapper.insert(prep);
        return toVO(prep);
    }

    @Override
    public RequirementPrepVO update(Long prepId, RequirementPrepUpdateReq req) {
        RequirementPrep prep = loadOwned(prepId);
        requireConfigurable(prep.getRequirementId());
        if (req.getTitle() != null) {
            if (req.getTitle().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "准备项名称不能为空");
            }
            prep.setTitle(req.getTitle().trim());
        }
        if (req.getContent() != null) {
            prep.setContent(req.getContent().isBlank() ? null : req.getContent().trim());
        }
        if (req.getDone() != null) {
            prep.setDone(req.getDone() ? 1 : 0);
        }
        requirementPrepMapper.updateById(prep);
        return toVO(prep);
    }

    @Override
    public void delete(Long prepId) {
        RequirementPrep prep = loadOwned(prepId);
        requireConfigurable(prep.getRequirementId());
        requirementPrepMapper.deleteById(prepId);
    }

    @Override
    public Map<Long, PrepStat> countByRequirementIds(Collection<Long> requirementIds) {
        if (requirementIds == null || requirementIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RequirementPrep> preps = requirementPrepMapper.selectList(new LambdaQueryWrapper<RequirementPrep>()
                .select(RequirementPrep::getRequirementId, RequirementPrep::getDone)
                .in(RequirementPrep::getRequirementId, requirementIds));
        return preps.stream().collect(Collectors.groupingBy(
                RequirementPrep::getRequirementId,
                Collectors.collectingAndThen(Collectors.toList(), list -> new PrepStat(
                        list.size(),
                        list.stream().filter(p -> p.getDone() != null && p.getDone() == 1).count()))));
    }

    @Override
    public void deleteByRequirement(Long requirementId) {
        requirementPrepMapper.delete(new LambdaQueryWrapper<RequirementPrep>()
                .eq(RequirementPrep::getRequirementId, requirementId));
    }

    // ===================== 辅助 =====================

    /** 校验需求归属 + 状态可配置（开发完成/验收完成），返回需求 */
    private RequirementVO requireConfigurable(Long requirementId) {
        RequirementVO requirement = requirementService.getById(requirementId);
        if (requirement == null || !CONFIGURABLE_STATUSES.contains(requirement.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅「开发完成/验收完成」状态的需求可配置上线准备");
        }
        return requirement;
    }

    /** 加载当前用户的准备项，越权/不存在抛 404 */
    private RequirementPrep loadOwned(Long prepId) {
        RequirementPrep prep = requirementPrepMapper.selectById(prepId);
        if (prep == null || !UserContextHolder.requireUserId().equals(prep.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "上线准备项不存在或无权访问");
        }
        return prep;
    }

    private RequirementPrepVO toVO(RequirementPrep prep) {
        return RequirementPrepVO.builder()
                .id(prep.getId())
                .requirementId(prep.getRequirementId())
                .title(prep.getTitle())
                .content(prep.getContent())
                .done(prep.getDone() != null && prep.getDone() == 1)
                .sortOrder(prep.getSortOrder())
                .build();
    }
}
