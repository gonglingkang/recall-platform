package com.recall.service.requirement.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recall.common.api.PageResp;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.requirement.RequirementMapper;
import com.recall.dto.requirement.RequirementCreateReq;
import com.recall.dto.requirement.RequirementPageReq;
import com.recall.dto.requirement.RequirementStatusReq;
import com.recall.dto.requirement.RequirementUpdateReq;
import com.recall.entity.objectives.KeyResult;
import com.recall.entity.requirement.Requirement;
import com.recall.entity.requirement.RequirementCategory;
import com.recall.enums.KeyResultStatus;
import com.recall.enums.RequirementStatus;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.requirement.RequirementCategoryService;
import com.recall.service.requirement.RequirementDocumentService;
import com.recall.service.requirement.RequirementService;
import com.recall.vo.requirement.RequirementDocumentVO;
import com.recall.vo.requirement.RequirementKeyResultVO;
import com.recall.vo.requirement.RequirementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 需求 Service 实现。
 * <p>
 * 持有 RequirementMapper，负责需求的增删改查、状态流转与关键成果 K 绑定。
 * 绑 K 时，讨论中/进行中/开发完成三态由 K 状态映射驱动；未绑 K 时手动维护。
 * 进入不涉及/验收完成/发布完成时自动解绑 K，之后 K 不再驱动。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementServiceImpl implements RequirementService {

    private final RequirementMapper requirementMapper;
    private final RequirementDocumentService requirementDocumentService;
    private final RequirementCategoryService requirementCategoryService;
    @Lazy
    private final KeyResultService keyResultService;

    @Override
    public PageResp<RequirementVO> page(RequirementPageReq req) {
        Long userId = UserContextHolder.requireUserId();
        LambdaQueryWrapper<Requirement> wrapper = new LambdaQueryWrapper<Requirement>()
                .eq(Requirement::getUserId, userId);
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            wrapper.like(Requirement::getTitle, req.getKeyword().trim());
        }
        // 时间范围按首次需求时间过滤（与 createdAt 区分，用于补录历史需求）
        if (req.getStartDate() != null) {
            wrapper.ge(Requirement::getFirstDemandDate, req.getStartDate());
        }
        if (req.getEndDate() != null) {
            wrapper.le(Requirement::getFirstDemandDate, req.getEndDate());
        }
        if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
            // 校验每个状态码合法（非法抛 422）
            req.getStatuses().forEach(RequirementStatus::of);
            wrapper.in(Requirement::getStatus, req.getStatuses());
        }
        // 分类筛选：传子分类精确查；只传主分类联动其下所有子分类
        if (req.getSubCategoryId() != null) {
            wrapper.eq(Requirement::getSubCategoryId, req.getSubCategoryId());
        } else if (req.getCategoryId() != null) {
            // 联动：主分类本身(category_id匹配) + 其下所有子分类(sub_category_id匹配)
            List<Long> subIds = requirementCategoryService.listSubCategoryIds(req.getCategoryId());
            if (subIds.isEmpty()) {
                wrapper.eq(Requirement::getCategoryId, req.getCategoryId());
            } else {
                wrapper.and(w -> w.eq(Requirement::getCategoryId, req.getCategoryId())
                        .or().in(Requirement::getSubCategoryId, subIds));
            }
        }
        wrapper.orderByDesc(Requirement::getFirstDemandDate);
        IPage<Requirement> result = requirementMapper.selectPage(
                new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<RequirementVO> records = result.getRecords().stream()
                .map(r -> toVO(r, requirementDocumentService.listByRequirement(r.getId()), toKeyResultVO(r.getKeyResultId())))
                .toList();
        return PageResp.<RequirementVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .pages(result.getPages())
                .build();
    }

    @Override
    public RequirementVO getById(Long id) {
        Requirement r = loadOwned(id);
        List<RequirementDocumentVO> docs = requirementDocumentService.listByRequirement(id);
        return toVO(r, docs, toKeyResultVO(r.getKeyResultId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequirementVO create(RequirementCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        // 同用户标题唯一
        if (requirementMapper.existsByTitle(userId, req.getTitle(), null)) {
            throw new BusinessException(ResultCode.REQUIREMENT_TITLE_DUPLICATED, req.getTitle());
        }
        Requirement r = new Requirement();
        r.setUserId(userId);
        r.setTitle(req.getTitle());
        r.setDescription(req.getDescription());
        r.setFirstDemandDate(req.getFirstDemandDate());
        r.setStatus(RequirementStatus.DISCUSSING.getValue());
        // 分类校验 + 赋值（主分类必选，子分类可选）
        requirementCategoryService.validateCategoryBinding(req.getCategoryId(), req.getSubCategoryId());
        r.setCategoryId(req.getCategoryId());
        r.setSubCategoryId(req.getSubCategoryId());
        requirementMapper.insert(r);
        // 可选绑定 K
        if (req.getKeyResultId() != null) {
            bindKr(r, req.getKeyResultId());
        }
        return toVO(r, requirementDocumentService.listByRequirement(r.getId()),
                toKeyResultVO(r.getKeyResultId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequirementVO update(Long id, RequirementUpdateReq req) {
        Requirement r = loadOwned(id);
        // 发布完成（终态）的需求禁止编辑
        if (RequirementStatus.of(r.getStatus()).isTerminal()) {
            throw new BusinessException(ResultCode.CONFLICT, "已发布完成的需求不可编辑");
        }
        // 标题变更需校验同用户唯一
        if (!req.getTitle().equals(r.getTitle())
                && requirementMapper.existsByTitle(r.getUserId(), req.getTitle(), id)) {
            throw new BusinessException(ResultCode.REQUIREMENT_TITLE_DUPLICATED, req.getTitle());
        }
        r.setTitle(req.getTitle());
        r.setDescription(req.getDescription());
        r.setFirstDemandDate(req.getFirstDemandDate());
        // 分类校验 + 赋值（主分类必选，子分类可选）
        requirementCategoryService.validateCategoryBinding(req.getCategoryId(), req.getSubCategoryId());
        r.setCategoryId(req.getCategoryId());
        r.setSubCategoryId(req.getSubCategoryId());
        requirementMapper.updateById(r);
        // keyResultId 全量覆盖：传值则绑该 K（与当前不同先解绑再绑），null 则解绑（回讨论中）
        if (req.getKeyResultId() != null) {
            if (!req.getKeyResultId().equals(r.getKeyResultId())) {
                // 与当前绑定不同：先解绑当前（若有），再绑新 K
                if (r.getKeyResultId() != null) {
                    unbindKr(r);
                }
                bindKr(r, req.getKeyResultId());
            }
        } else if (r.getKeyResultId() != null) {
            // 传 null 且当前已绑 K：解绑回讨论中
            unbindKr(r);
        }
        return toVO(r, requirementDocumentService.listByRequirement(id),
                toKeyResultVO(r.getKeyResultId()));
    }

    /**
     * 手动切换需求状态。
     * <p>绑 K 时，讨论中/进行中/开发完成三态由 K 驱动，禁止手动改这三态；
     * 手动只能进入不涉及/验收完成/发布完成。进入不涉及解绑 K；
     * 进入验收完成/发布完成保留 K 关联（仅断开 K 联动，便于事后追溯关联的关键成果）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequirementVO changeStatus(Long id, RequirementStatusReq req) {
        Requirement r = loadOwned(id);
        RequirementStatus current = RequirementStatus.of(r.getStatus());
        RequirementStatus target = req.getStatus();

        // 终态不可再变更
        if (current.isTerminal()) {
            throw new BusinessException(ResultCode.CONFLICT, "已发布完成的需求不可变更状态");
        }
        // 绑 K 时禁止手动改 K 活跃态（这三态跟 K 走）
        if (r.getKeyResultId() != null && target.isActiveKrState()) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "已绑定关键成果的需求，讨论中/进行中/开发完成状态由关键成果联动，不可手动变更");
        }
        // 手动状态机校验
        validateManualTransition(current, target);

        if (target == RequirementStatus.NOT_INVOLVED) {
            // 进入不涉及：决定不做，解绑 K，填不涉及原因，清开发完成时间
            r.setStatus(target.getValue());
            r.setKeyResultId(null);
            r.setCancelReason(req.getCancelReason());
            r.setDevCompleteDate(null);
        } else if (target == RequirementStatus.ACCEPTANCE_DONE) {
            // 进入验收完成：保留 K 关联（仅断开联动），填验收完成时间(不传默认当天)与验收人，保留开发完成时间
            r.setStatus(target.getValue());
            r.setAcceptanceDate(req.getAcceptanceDate() != null ? req.getAcceptanceDate() : LocalDate.now());
            r.setAcceptancePerson(req.getAcceptancePerson());
        } else if (target == RequirementStatus.RELEASED) {
            // 进入发布完成：保留 K 关联（仅断开联动），填发布完成时间(不传默认当天，终态)，保留开发完成时间
            r.setStatus(target.getValue());
            r.setReleaseDate(req.getReleaseDate() != null ? req.getReleaseDate() : LocalDate.now());
        } else {
            // 未绑 K 时的手动流转（讨论中/进行中/开发完成）：清不涉及原因
            r.setStatus(target.getValue());
            r.setCancelReason(null);
            if (target == RequirementStatus.DEV_DONE) {
                if (r.getDevCompleteDate() == null) {
                    // 手动进入开发完成（仅未绑 K 时可达）：填当天
                    r.setDevCompleteDate(LocalDate.now());
                }
            } else {
                // 回退到讨论中/进行中：清开发完成时间
                r.setDevCompleteDate(null);
            }
        }
        applyUpdate(r);
        return toVO(r, requirementDocumentService.listByRequirement(id),
                toKeyResultVO(r.getKeyResultId()));
    }

    /**
     * 删除需求（连带删除其下文档）。
     * <p>多条写（删需求 + 删文档）-> 事务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        loadOwned(id);
        requirementDocumentService.deleteByRequirement(id);
        requirementMapper.deleteById(id);
    }

    @Override
    public void checkOwned(Long requirementId) {
        loadOwned(requirementId);
    }

    /**
     * 关键成果 K 状态变更后同步所有绑定该 K 的需求状态。
     * <p>K 状态映射：未开始->讨论中，进行中->进行中，已完成->开发完成，已取消->解绑回讨论中。
     * 仅需求处于 K 活跃态时生效；脱钩态不受影响。一个 K 可被多个需求绑定，逐个同步。
     */
    @Override
    public void syncStatusByKeyResult(Long krId, KeyResultStatus krStatus) {
        List<Requirement> requirements = findListByKeyResultId(krId);
        if (requirements.isEmpty()) {
            return;
        }
        // K 已完成时取其 completeDate 供需求开发完成时间用
        KeyResult kr = null;
        LocalDate krCompleteDate = null;
        if (krStatus != KeyResultStatus.CANCELLED) {
            kr = keyResultService.getById(krId, false);
            krCompleteDate = kr != null ? kr.getCompleteDate() : null;
        }
        for (Requirement r : requirements) {
            RequirementStatus current = RequirementStatus.of(r.getStatus());
            // 脱钩态（不涉及/验收完成/发布完成）不受 K 驱动
            if (!current.isActiveKrState()) {
                continue;
            }
            if (krStatus == KeyResultStatus.CANCELLED) {
                // K 取消：解绑回讨论中
                r.setStatus(RequirementStatus.DISCUSSING.getValue());
                r.setKeyResultId(null);
                r.setDevCompleteDate(null);
            } else {
                // K 未开始/进行中/已完成：映射需求状态
                applyKrStatus(r, krStatus, krCompleteDate);
            }
            applyUpdate(r);
            log.info("关键成果 K={} 状态变更为 {}，同步需求={} 状态", krId, krStatus.getValue(), r.getId());
        }
    }

    /**
     * 关键成果 K 被删除时，解绑所有绑定该 K 的需求；K 活跃态的需求回讨论中。
     */
    @Override
    public void handleKeyResultDeleted(Long krId) {
        List<Requirement> requirements = findListByKeyResultId(krId);
        if (requirements.isEmpty()) {
            return;
        }
        for (Requirement r : requirements) {
            RequirementStatus current = RequirementStatus.of(r.getStatus());
            r.setKeyResultId(null);
            // 脱钩态解绑后状态不变（已脱钩），仅 K 活跃态回讨论中
            if (current.isActiveKrState()) {
                r.setStatus(RequirementStatus.DISCUSSING.getValue());
                r.setDevCompleteDate(null);
            }
            applyUpdate(r);
            log.info("关键成果 K={} 被删除，解绑需求={}", krId, r.getId());
        }
    }

    /**
     * 批量关键成果 K 被删除时，解绑这些 K 的需求并回讨论中。
     */
    @Override
    public void handleKeyResultsDeleted(List<Long> krIds) {
        if (krIds == null || krIds.isEmpty()) {
            return;
        }
        for (Long krId : krIds) {
            handleKeyResultDeleted(krId);
        }
    }

    @Override
    public boolean existsByCategory(Long categoryId) {
        return requirementMapper.exists(new LambdaQueryWrapper<Requirement>()
                .eq(Requirement::getCategoryId, categoryId));
    }

    @Override
    public boolean existsBySubCategory(Long subCategoryId) {
        return requirementMapper.exists(new LambdaQueryWrapper<Requirement>()
                .eq(Requirement::getSubCategoryId, subCategoryId));
    }

    // ===================== K 绑定（内部复用） =====================

    /**
     * 绑定关键成果 K（一个 K 可被多个需求绑定），绑定后按 K 状态同步需求状态。
     * <p>仅 K 活跃态允许绑定；K 须属于当前用户、非已取消。
     * 修改 r 的内存字段后由调用方落库。
     *
     * @param r    需求实体（须已 loadOwned）
     * @param krId 关键成果 ID
     */
    private void bindKr(Requirement r, Long krId) {
        RequirementStatus current = RequirementStatus.of(r.getStatus());
        // 仅 K 活跃态允许绑定
        if (!current.isActiveKrState()) {
            throw new BusinessException(ResultCode.REQUIREMENT_KR_BIND_STATE_NOT_ALLOWED,
                    "当前状态不可绑定关键成果，仅讨论中/进行中/开发完成可绑定");
        }
        // 校验 K 属于当前用户（越权统一 404）
        KeyResult kr = keyResultService.getById(krId, true);
        // K 已取消不可绑定
        if (KeyResultStatus.CANCELLED.getValue().equals(kr.getStatus())) {
            throw new BusinessException(ResultCode.REQUIREMENT_KR_CANCELLED, "不能绑定已取消的关键成果");
        }
        // 绑定并按 K 状态同步（一个 K 可被多个需求绑定，不校验独占）
        r.setKeyResultId(krId);
        applyKrStatus(r, KeyResultStatus.of(kr.getStatus()), kr.getCompleteDate());
        applyUpdate(r);
        log.info("需求={} 绑定关键成果 K={}", r.getId(), krId);
    }

    /**
     * 解绑关键成果 K（回讨论中，清开发完成时间）。
     * <p>修改 r 的内存字段后由调用方落库。
     *
     * @param r 需求实体（须已 loadOwned）
     */
    private void unbindKr(Requirement r) {
        r.setStatus(RequirementStatus.DISCUSSING.getValue());
        r.setKeyResultId(null);
        r.setDevCompleteDate(null);
        applyUpdate(r);
    }

    // ===================== 辅助 =====================

    /**
     * 加载当前用户的需求，越权/不存在抛 404
     */
    private Requirement loadOwned(Long id) {
        Requirement r = requirementMapper.selectById(id);
        if (r == null || !UserContextHolder.requireUserId().equals(r.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在或无权访问");
        }
        return r;
    }

    /**
     * 按关键成果 ID 查绑定的所有需求（一个 K 可被多个需求绑定）。
     *
     * @param keyResultId 关键成果 ID
     * @return 绑定该 K 的需求列表，可能为空
     */
    private List<Requirement> findListByKeyResultId(Long keyResultId) {
        if (keyResultId == null) {
            return Collections.emptyList();
        }
        return requirementMapper.selectList(new LambdaQueryWrapper<Requirement>()
                .eq(Requirement::getKeyResultId, keyResultId));
    }

    /**
     * 按 keyResultId 查 K 实体并转为摘要 VO（仅 id/name/status），未绑定为 null。
     * <p>不查 sprintIds/records 等重数据，轻量展示。
     */
    private RequirementKeyResultVO toKeyResultVO(Long keyResultId) {
        if (keyResultId == null) {
            return null;
        }
        KeyResult kr = keyResultService.getById(keyResultId, false);
        if (kr == null) {
            return null;
        }
        return RequirementKeyResultVO.builder()
                .id(kr.getId())
                .name(kr.getName())
                .status(kr.getStatus())
                .build();
    }

    /**
     * 解析日期字符串为当天 00:00:00
     */
    private LocalDateTime parseStartOfDay(String date) {
        return LocalDate.parse(date).atStartOfDay();
    }

    /**
     * 解析日期字符串为次日 00:00:00（便于 createdAt < 次日零点 的范围查询）
     */
    private LocalDateTime parseEndOfDay(String date) {
        return LocalDate.parse(date).plusDays(1).atStartOfDay();
    }

    /**
     * 显式更新需求的状态相关字段（含可能为 null 的字段）。
     * <p>用 LambdaUpdateWrapper 绕开 MyBatis-Plus 默认 NOT_NULL 策略，确保 keyResultId/devCompleteDate
     * 等字段置 null 能落库。
     *
     * @param r 已修改字段值的实体（按 id 更新）
     */
    private void applyUpdate(Requirement r) {
        requirementMapper.update(null, new LambdaUpdateWrapper<Requirement>()
                .eq(Requirement::getId, r.getId())
                .set(Requirement::getStatus, r.getStatus())
                .set(Requirement::getKeyResultId, r.getKeyResultId())
                .set(Requirement::getDevCompleteDate, r.getDevCompleteDate())
                .set(Requirement::getAcceptanceDate, r.getAcceptanceDate())
                .set(Requirement::getAcceptancePerson, r.getAcceptancePerson())
                .set(Requirement::getReleaseDate, r.getReleaseDate())
                .set(Requirement::getCancelReason, r.getCancelReason()));
    }

    /**
     * 按 K 状态映射需求状态（K 活跃态时调用，仅修改内存对象，落库由 applyUpdate 完成）。
     * <p>未开始->讨论中，进行中->进行中，已完成->开发完成（devCompleteDate 取 K 的 completeDate）。
     * 已取消由调用方单独处理（需解绑）。
     *
     * @param r              需求实体（内存修改）
     * @param krStatus       K 当前状态
     * @param krCompleteDate K 的完成时间（仅 DONE 分支使用；为 null 时兜底当天）
     */
    private void applyKrStatus(Requirement r, KeyResultStatus krStatus, LocalDate krCompleteDate) {
        switch (krStatus) {
            case NOT_STARTED -> {
                r.setStatus(RequirementStatus.DISCUSSING.getValue());
                r.setDevCompleteDate(null);
            }
            case IN_PROGRESS -> {
                r.setStatus(RequirementStatus.IN_PROGRESS.getValue());
                r.setDevCompleteDate(null);
            }
            case DONE -> {
                r.setStatus(RequirementStatus.DEV_DONE.getValue());
                if (r.getDevCompleteDate() == null) {
                    // 取 K 的完成时间作为需求开发完成时间（绑定历史已完成的 K 时尤其重要）
                    r.setDevCompleteDate(krCompleteDate != null ? krCompleteDate : LocalDate.now());
                }
            }
            default -> { /* CANCELLED 不应进入此方法，调用方单独处理 */
            }
        }
    }

    /**
     * 校验手动状态流转是否合法。
     * <p>合法流转：
     * <ul>
     *   <li>讨论中 -> 不涉及 / 进行中</li>
     *   <li>不涉及 -> 讨论中 / 进行中</li>
     *   <li>进行中 -> 讨论中 / 不涉及 / 开发完成</li>
     *   <li>开发完成 -> 进行中 / 不涉及 / 验收完成 / 发布完成</li>
     *   <li>验收完成 -> 发布完成（不可回退）</li>
     * </ul>
     * 发布完成为终态，不可流转。其余流转非法，抛 409。
     *
     * @param current 当前状态
     * @param target  目标状态
     */
    private void validateManualTransition(RequirementStatus current, RequirementStatus target) {
        boolean legal = switch (current) {
            case DISCUSSING -> target == RequirementStatus.NOT_INVOLVED
                    || target == RequirementStatus.IN_PROGRESS;
            case NOT_INVOLVED -> target == RequirementStatus.DISCUSSING
                    || target == RequirementStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == RequirementStatus.DISCUSSING
                    || target == RequirementStatus.NOT_INVOLVED
                    || target == RequirementStatus.DEV_DONE;
            case DEV_DONE -> target == RequirementStatus.IN_PROGRESS
                    || target == RequirementStatus.NOT_INVOLVED
                    || target == RequirementStatus.ACCEPTANCE_DONE
                    || target == RequirementStatus.RELEASED;
            case ACCEPTANCE_DONE -> target == RequirementStatus.RELEASED;
            case RELEASED -> false;
        };
        if (!legal) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "状态流转非法：" + current.getValue() + " -> " + target.getValue());
        }
    }

    private RequirementVO toVO(Requirement r, List<RequirementDocumentVO> docs, RequirementKeyResultVO krVO) {
        // 分类名称映射（分类与需求同用户，这里不再重复鉴权）
        String categoryName = null;
        String subCategoryName = null;
        if (r.getCategoryId() != null) {
            RequirementCategory main = requirementCategoryService.getById(r.getCategoryId(), false);
            if (main != null) {
                categoryName = main.getName();
            }
        }
        if (r.getSubCategoryId() != null) {
            RequirementCategory sub = requirementCategoryService.getById(r.getSubCategoryId(), false);
            if (sub != null) {
                subCategoryName = sub.getName();
            }
        }
        return RequirementVO.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .status(r.getStatus())
                .keyResultId(r.getKeyResultId())
                .categoryId(r.getCategoryId())
                .categoryName(categoryName)
                .subCategoryId(r.getSubCategoryId())
                .subCategoryName(subCategoryName)
                .firstDemandDate(r.getFirstDemandDate())
                .keyResult(krVO)
                .devCompleteDate(r.getDevCompleteDate())
                .acceptanceDate(r.getAcceptanceDate())
                .acceptancePerson(r.getAcceptancePerson())
                .releaseDate(r.getReleaseDate())
                .cancelReason(r.getCancelReason())
                .documents(docs == null ? Collections.emptyList() : docs)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
