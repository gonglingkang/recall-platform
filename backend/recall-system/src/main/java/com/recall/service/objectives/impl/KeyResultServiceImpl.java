package com.recall.service.objectives.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.common.util.DateUtils;
import com.recall.dao.objectives.KeyResultMapper;
import com.recall.dto.objectives.KeyResultCreateReq;
import com.recall.dto.objectives.KeyResultRecordsUpdateReq;
import com.recall.dto.objectives.KeyResultStatusReq;
import com.recall.dto.objectives.KeyResultUpdateReq;
import com.recall.entity.objectives.KeyResult;
import com.recall.entity.objectives.Objective;
import com.recall.enums.KeyResultStatus;
import com.recall.service.objectives.KeyResultRecordService;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.requirement.RequirementService;
import com.recall.service.sprint.SprintKeyResultService;
import com.recall.service.sprint.SprintService;
import com.recall.vo.objectives.KeyResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 关键成果 K Service 实现（v2.0）。
 * <p>
 * 持有 KeyResultMapper，负责 K 的增删改查、状态切换与 completeDate 维护。
 * 创建时校验归属目标 O 存在且属于当前用户。
 * completeDate 维护规则：status→done 填当天（已有值不覆盖），切回非 done 清空。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyResultServiceImpl implements KeyResultService {

    private final KeyResultMapper keyResultMapper;
    @Lazy
    private final ObjectiveService objectiveService;
    @Lazy
    private final SprintService sprintService;
    @Lazy
    private final RequirementService requirementService;
    private final SprintKeyResultService sprintKeyResultService;
    private final KeyResultRecordService keyResultRecordService;

    @Override
    public KeyResultVO create(KeyResultCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        // 校验归属目标存在且属于当前用户（越权统一 404，不暴露存在性）
        Objective obj = objectiveService.getById(req.getObjectiveId(), true);
        // 同一目标下名称唯一
        if (keyResultMapper.existsByName(req.getObjectiveId(), req.getName(), null)) {
            throw new BusinessException(ResultCode.CONFLICT, "关键成果名已存在: " + req.getName());
        }

        KeyResult kr = new KeyResult();
        kr.setUserId(userId);
        kr.setObjectiveId(req.getObjectiveId());
        kr.setName(req.getName());
        kr.setDescription(req.getDescription());
        kr.setStatus(req.getStatus() == null ? KeyResultStatus.NOT_STARTED.getValue() : req.getStatus().getValue());
        // 创建时不允许已取消状态：取消是执行过程中发现无法落实才做的操作，新建即取消无意义
        if (KeyResultStatus.CANCELLED.getValue().equals(kr.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "创建关键成果不允许选择已取消状态");
        }
        // 计划完成时间默认值：
        //   用户传了就用用户的；未传时：已完成 → 当天，未开始/进行中 → 目标月份月底最后一天
        if (req.getPlanCompleteDate() != null) {
            kr.setPlanCompleteDate(req.getPlanCompleteDate());
        } else if (KeyResultStatus.DONE.getValue().equals(kr.getStatus())) {
            kr.setPlanCompleteDate(LocalDate.now());
        } else {
            kr.setPlanCompleteDate(DateUtils.endOfMonth(obj.getMonth()));
        }
        // 已完成填当天完成时间
        if (KeyResultStatus.DONE.getValue().equals(kr.getStatus())) {
            kr.setCompleteDate(LocalDate.now());
        }
        keyResultMapper.insert(kr);
        return toVO(kr, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public KeyResultVO update(Long id, KeyResultUpdateReq req) {
        KeyResult kr = loadOwned(id);
        boolean done = KeyResultStatus.DONE.getValue().equals(kr.getStatus());
        boolean cancelled = KeyResultStatus.CANCELLED.getValue().equals(kr.getStatus());
        boolean locked = done || cancelled; // 已完成/已取消的 K 禁改计划完成时间
        if (req.getName() != null) {
            if (req.getName().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "关键成果名称不能为空");
            }
            // 名称变更需校验同一目标下唯一
            if (!req.getName().equals(kr.getName())
                    && keyResultMapper.existsByName(kr.getObjectiveId(), req.getName(), id)) {
                throw new BusinessException(ResultCode.CONFLICT, "关键成果名已存在: " + req.getName());
            }
            kr.setName(req.getName());
        }
        if (req.getDescription() != null) kr.setDescription(req.getDescription());
        // 已完成/已取消的 K 只能编辑名称和描述，计划完成时间不可改
        if (!locked && req.getPlanCompleteDate() != null) {
            kr.setPlanCompleteDate(req.getPlanCompleteDate());
        }
        if (locked && req.getPlanCompleteDate() != null) {
            throw new BusinessException(ResultCode.CONFLICT,
                    done ? "已完成的关键成果不可修改计划完成时间" : "已取消的关键成果不可修改计划完成时间");
        }
        // 编辑不改变 status 和 completeDate
        keyResultMapper.updateById(kr);
        return toVO(kr, sprintKeyResultService.listSprintIdsByKeyResultId(id),
                keyResultRecordService.listContentsByKeyResultId(id));
    }

    /**
     * 切换关键成果 K 的状态（改变进度的唯一入口，completeDate 后端自动维护）。
     * <p>含 K 自身 update + 跨调 Sprint 同步（多写 + 跨 Service → 事务），保证 K 状态与冲刺联动原子提交。
     *
     * @param id  关键成果 ID
     * @param req 状态请求
     * @return 更新后的关键成果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeyResultVO changeStatus(Long id, KeyResultStatusReq req) {
        KeyResult kr = loadOwned(id);
        KeyResultStatus current = KeyResultStatus.of(kr.getStatus());
        KeyResultStatus target = req.getStatus();

        // 状态机校验（严格线性 + 跳级完成）：仅允许 5 条合法流转
        validateTransition(current, target);

        kr.setStatus(target.getValue());
        if (target == KeyResultStatus.DONE) {
            // ->已完成：已有 completeDate 不覆盖，否则填当天；清空取消原因
            if (kr.getCompleteDate() == null) {
                kr.setCompleteDate(LocalDate.now());
            }
            kr.setCancelReason(null);
            // 全量覆盖成果记录 R：传了覆盖旧 R，不传(null)或空清空旧 R
            keyResultRecordService.replaceByKeyResultId(id, req.getRecords());
        } else if (target == KeyResultStatus.CANCELLED) {
            // ->已取消：清空完成时间，写入取消原因；R 保留不动
            kr.setCompleteDate(null);
            kr.setCancelReason(req.getCancelReason());
        } else {
            // ->未开始/进行中：清空完成时间与取消原因；R 保留不动
            kr.setCompleteDate(null);
            kr.setCancelReason(null);
        }
        keyResultMapper.updateById(kr);
        // 联动同步关联该 K 的冲刺任务状态
        sprintService.syncStatusByKeyResult(id, target);
        // 联动同步绑该 K 的需求状态（K 状态映射需求讨论中/进行中/开发完成）
        requirementService.syncStatusByKeyResult(id, target);
        return toVO(kr, sprintKeyResultService.listSprintIdsByKeyResultId(id),
                keyResultRecordService.listContentsByKeyResultId(id));
    }

    /**
     * 全量更新关键成果的成果记录 R。
     * <p>仅已完成的 K 允许更新 R（R 只在已完成时产生）。委托 KeyResultRecordService.replaceByKeyResultId
     * （先删后插，多条写）-> 加事务保证原子性。不改 K 状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeyResultVO updateRecords(Long id, KeyResultRecordsUpdateReq req) {
        KeyResult kr = loadOwned(id);
        // 仅已完成的 K 才有 R，非已完成禁止更新
        if (!KeyResultStatus.DONE.getValue().equals(kr.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅已完成的关键成果可更新成果记录");
        }
        // 全量覆盖 R（传了覆盖，空清空）
        keyResultRecordService.replaceByKeyResultId(id, req.getRecords());
        return toVO(kr, sprintKeyResultService.listSprintIdsByKeyResultId(id),
                keyResultRecordService.listContentsByKeyResultId(id));
    }

    /**
     * 校验状态流转是否合法（严格状态机）。
     * <p>合法流转：
     * <ul>
     *   <li>未开始(0) → 进行中(1)：开始</li>
     *   <li>未开始(0) → 已完成(2)：跳级完成</li>
     *   <li>未开始(0) → 已取消(3)：取消</li>
     *   <li>进行中(1) → 已完成(2)：完成</li>
     *   <li>进行中(1) → 已取消(3)：取消</li>
     *   <li>已完成(2) → 进行中(1)：取消完成（返工）</li>
     *   <li>已取消(3) → 未开始(0)：恢复</li>
     * </ul>
     * 其余流转（已完成→已取消、已取消→进行中/已完成、同状态等）非法，抛 409。
     *
     * @param current 当前状态
     * @param target  目标状态
     */
    private void validateTransition(KeyResultStatus current, KeyResultStatus target) {
        boolean legal = switch (current) {
            case NOT_STARTED -> target == KeyResultStatus.IN_PROGRESS
                    || target == KeyResultStatus.DONE
                    || target == KeyResultStatus.CANCELLED;
            case IN_PROGRESS -> target == KeyResultStatus.DONE
                    || target == KeyResultStatus.CANCELLED;
            case DONE -> target == KeyResultStatus.IN_PROGRESS;
            case CANCELLED -> target == KeyResultStatus.NOT_STARTED;
        };
        if (!legal) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "状态流转非法：" + current.getValue() + " → " + target.getValue());
        }
    }

    /**
     * 删除关键成果 K。
     * <p>连带清理 sprint_key_results 关联表，并触发受影响冲刺任务状态重算（多条写 + 跨 Service 调用 → 事务）。
     *
     * @param id 关键成果 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        loadOwned(id);
        // 先反查受影响的冲刺（关联记录还在时才能查到），再清关联、删 R、删 K、重算冲刺
        List<Long> affectedSprintIds = sprintKeyResultService.listSprintIdsByKeyResultId(id);
        sprintKeyResultService.deleteByKeyResultId(id);
        keyResultRecordService.deleteByKeyResultId(id);
        keyResultMapper.deleteById(id);
        if (!affectedSprintIds.isEmpty()) {
            sprintService.recomputeStatus(affectedSprintIds);
        }
        // 联动解绑该 K 的需求并回讨论中
        requirementService.handleKeyResultDeleted(id);
    }

    @Override
    public List<KeyResult> listByObjective(Long objectiveId) {
        return keyResultMapper.selectList(new LambdaQueryWrapper<KeyResult>()
                .eq(KeyResult::getObjectiveId, objectiveId)
                .orderByAsc(KeyResult::getId));
    }

    @Override
    public List<KeyResult> listByObjectives(List<Long> objectiveIds) {
        if (objectiveIds == null || objectiveIds.isEmpty()) {
            return Collections.emptyList();
        }
        return keyResultMapper.selectList(new LambdaQueryWrapper<KeyResult>()
                .in(KeyResult::getObjectiveId, objectiveIds)
                .orderByAsc(KeyResult::getId));
    }

    /**
     * 删除指定目标 O 下的所有关键成果（供 ObjectiveService 删除 O 时连带调用）。
     * <p>连带清理这些 K 在 sprint_key_results 的关联记录，并触发受影响冲刺任务状态重算
     * （多条写 + 跨 Service 调用 → 事务；被 ObjectiveService.delete 外层事务覆盖，REQUIRED 传播）。
     *
     * @param objectiveId 目标 ID
     * @return 删除的 K 条数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByObjective(Long objectiveId) {
        // 先查出 O 下所有 K 的 id，用于反查受影响冲刺
        List<Long> krIds = keyResultMapper.selectList(new LambdaQueryWrapper<KeyResult>()
                        .eq(KeyResult::getObjectiveId, objectiveId))
                .stream().map(KeyResult::getId).toList();
        if (krIds.isEmpty()) {
            return 0;
        }
        // 汇总这些 K 关联的所有冲刺（去重），用于删后重算
        List<Long> affectedSprintIds = krIds.stream()
                .flatMap(krId -> sprintKeyResultService.listSprintIdsByKeyResultId(krId).stream())
                .distinct()
                .toList();
        // 批量清关联 + 删 R + 删 K
        sprintKeyResultService.deleteByKeyResultIds(krIds);
        keyResultRecordService.deleteByKeyResultIds(krIds);
        int deleted = keyResultMapper.delete(new LambdaQueryWrapper<KeyResult>()
                .eq(KeyResult::getObjectiveId, objectiveId));
        if (!affectedSprintIds.isEmpty()) {
            sprintService.recomputeStatus(affectedSprintIds);
        }
        // 联动解绑这些 K 的需求并回讨论中
        requirementService.handleKeyResultsDeleted(krIds);
        return deleted;
    }

    // ===================== 辅助 =====================

    /**
     * 按 id 查询关键成果实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     */
    @Override
    public KeyResult getById(Long id, boolean checkOwnership) {
        if (checkOwnership) {
            return loadOwned(id);
        }
        return keyResultMapper.selectById(id);
    }

    /** 加载当前用户的关键成果，越权/不存在抛 404 */
    private KeyResult loadOwned(Long id) {
        KeyResult kr = keyResultMapper.selectById(id);
        if (kr == null || !UserContextHolder.requireUserId().equals(kr.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "关键成果不存在或无权访问");
        }
        return kr;
    }

    /**
     * 按 id 批量查询关键成果实体。
     * <p>checkOwnership=true 时校验：任一 id 不存在或不属于当前用户均抛 404（越权统一 404）。
     */
    @Override
    public List<KeyResult> listByIds(List<Long> ids, boolean checkOwnership) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<KeyResult> krs = keyResultMapper.selectList(new LambdaQueryWrapper<KeyResult>()
                .in(KeyResult::getId, ids));
        if (!checkOwnership) {
            return krs;
        }
        // 越权统一 404：数量不符直接判为存在不属于当前用户的 id（不暴露具体哪个）
        if (krs.size() != ids.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "关键成果不存在或无权访问");
        }
        Long userId = UserContextHolder.requireUserId();
        for (KeyResult kr : krs) {
            if (!userId.equals(kr.getUserId())) {
                throw new BusinessException(ResultCode.NOT_FOUND, "关键成果不存在或无权访问");
            }
        }
        return krs;
    }

    private KeyResultVO toVO(KeyResult kr, List<Long> sprintIds, List<String> records) {
        return KeyResultVO.builder()
                .id(kr.getId())
                .name(kr.getName())
                .description(kr.getDescription())
                .status(kr.getStatus())
                .planCompleteDate(kr.getPlanCompleteDate())
                .completeDate(kr.getCompleteDate())
                .cancelReason(kr.getCancelReason())
                .sprintIds(sprintIds == null ? Collections.emptyList() : sprintIds)
                .records(records == null ? Collections.emptyList() : records)
                .build();
    }
}
