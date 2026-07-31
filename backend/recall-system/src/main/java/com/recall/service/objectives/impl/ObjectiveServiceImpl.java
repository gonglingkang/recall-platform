package com.recall.service.objectives.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.objectives.ObjectiveMapper;
import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.objectives.ObjectiveUpdateReq;
import com.recall.entity.objectives.KeyResult;
import com.recall.entity.objectives.Objective;
import com.recall.entity.sprint.SprintKeyResult;
import com.recall.enums.KeyResultStatus;
import com.recall.service.objectives.KeyResultRecordService;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.service.sprint.SprintKeyResultService;
import com.recall.vo.objectives.KeyResultVO;
import com.recall.vo.objectives.ObjectiveVO;
import com.recall.vo.plan.MonthCompletionCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 月度绩效目标 O Service 实现（v2.0）。
 * <p>
 * 仅持有 ObjectiveMapper，负责 O 的增删改查与派生计算。
 * K 的数据通过 {@link KeyResultService} 获取；删除 O 时调用其连带删除 K。
 * <p>
 * 目标 O 的派生计算规则（查询时实时聚合其下 K）：
 * <ul>
 *   <li>progress = 已完成K数 / K总数 × 100（取整），无 K 时为 0</li>
 *   <li>status = 全 not_started→not_started；任一 in_progress 或部分 done→in_progress；全 done→done</li>
 *   <li>planCompleteDate = max(K.planCompleteDate)，无 K 时 null</li>
 *   <li>actualCompleteDate = 全 K done 时 max(K.completeDate)，否则 null</li>
 * </ul>
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectiveServiceImpl implements ObjectiveService {

    private final ObjectiveMapper objectiveMapper;
    private final KeyResultService keyResultService;
    private final SprintKeyResultService sprintKeyResultService;
    private final KeyResultRecordService keyResultRecordService;

    // ===================== 目标 O =====================

    @Override
    public List<ObjectiveVO> list(String month) {
        // 防御兜底：month 格式校验（Controller 注解已校验，此处防其他 Service 直接调用）
        validateMonth(month);
        List<Objective> objectives = objectiveMapper.selectList(new LambdaQueryWrapper<Objective>()
                .eq(Objective::getUserId, UserContextHolder.requireUserId())
                .eq(Objective::getMonth, month)
                .orderByAsc(Objective::getId));
        if (objectives.isEmpty()) {
            return Collections.emptyList();
        }
        // 通过 KeyResultService 一次性查出该月所有 K，按 objectiveId 分组（派生计算用）
        List<KeyResult> allKrs = keyResultService.listByObjectives(
                objectives.stream().map(Objective::getId).toList());
        Map<Long, List<KeyResult>> krMap = allKrs.stream()
                .collect(Collectors.groupingBy(KeyResult::getObjectiveId));
        // 批量查出这些 K 关联的冲刺 ID，按 keyResultId 分组（前端提示用）
        Map<Long, List<Long>> sprintIdsByKrId = allKrs.isEmpty()
                ? Collections.emptyMap()
                : sprintKeyResultService.listByKeyResultIds(
                        allKrs.stream().map(KeyResult::getId).toList()).stream()
                        .collect(Collectors.groupingBy(
                                SprintKeyResult::getKeyResultId,
                                Collectors.mapping(SprintKeyResult::getSprintId, Collectors.toList())));
        // 批量查出这些 K 的成果记录 R，按 keyResultId 分组
        Map<Long, List<String>> recordsByKrId = allKrs.isEmpty()
                ? Collections.emptyMap()
                : keyResultRecordService.listContentsByKeyResultIds(
                        allKrs.stream().map(KeyResult::getId).toList());
        return objectives.stream()
                .map(o -> toVO(o, krMap.getOrDefault(o.getId(), Collections.emptyList()),
                        sprintIdsByKrId, recordsByKrId))
                .toList();
    }

    @Override
    public List<MonthCompletionCountVO> countKeyResultsByMonthRange(String startMonth, String endMonth) {
        // K 自身不存月份，需先取区间内的 O 建立 objectiveId → month 映射，再批量取 K（共 2 次查询，与月份跨度无关）
        List<Objective> objectives = objectiveMapper.selectList(new LambdaQueryWrapper<Objective>()
                .select(Objective::getId, Objective::getMonth)
                .eq(Objective::getUserId, UserContextHolder.requireUserId())
                .between(Objective::getMonth, startMonth, endMonth));
        if (objectives.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> monthByObjectiveId = objectives.stream()
                .collect(Collectors.toMap(Objective::getId, Objective::getMonth));
        List<KeyResult> krs = keyResultService.listByObjectives(objectives.stream().map(Objective::getId).toList());
        return krs.stream()
                // 已取消的 K 不进分母，与 toVO 的派生口径一致
                .filter(kr -> !KeyResultStatus.CANCELLED.getValue().equals(kr.getStatus()))
                .collect(Collectors.groupingBy(kr -> monthByObjectiveId.get(kr.getObjectiveId())))
                .entrySet().stream()
                .map(e -> {
                    MonthCompletionCountVO count = new MonthCompletionCountVO();
                    count.setMonth(e.getKey());
                    count.setTotal(e.getValue().size());
                    count.setDone(e.getValue().stream()
                            .filter(kr -> KeyResultStatus.DONE.getValue().equals(kr.getStatus()))
                            .count());
                    return count;
                })
                .toList();
    }

    @Override
    public List<String> listMonths() {
        // 仅取当前用户存在目标 O 的月份，去重 + 倒序；走 idx_user_month 索引
        List<Objective> objectives = objectiveMapper.selectList(new LambdaQueryWrapper<Objective>()
                .select(Objective::getMonth)
                .eq(Objective::getUserId, UserContextHolder.requireUserId())
                .groupBy(Objective::getMonth)
                .orderByDesc(Objective::getMonth));
        return objectives.stream().map(Objective::getMonth).toList();
    }

    @Override
    public ObjectiveVO create(ObjectiveCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        // 同用户同月名称唯一
        if (objectiveMapper.existsByName(userId, req.getMonth(), req.getName(), null)) {
            throw new BusinessException(ResultCode.CONFLICT, "目标名已存在: " + req.getName());
        }
        Objective o = new Objective();
        o.setUserId(userId);
        o.setMonth(req.getMonth());
        o.setName(req.getName());
        o.setDescription(req.getDescription());
        objectiveMapper.insert(o);
        return toVO(o, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public ObjectiveVO update(Long id, ObjectiveUpdateReq req) {
        Objective o = loadOwnedObjective(id);
        // 名称变更需校验同用户同月唯一
        if (!req.getName().equals(o.getName())
                && objectiveMapper.existsByName(o.getUserId(), o.getMonth(), req.getName(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "目标名已存在: " + req.getName());
        }
        o.setName(req.getName());
        if (req.getDescription() != null) o.setDescription(req.getDescription());
        objectiveMapper.updateById(o);
        // 返回含其下 K 的完整视图（K 数据来自 KeyResultService）
        List<KeyResult> krs = keyResultService.listByObjective(id);
        Map<Long, List<Long>> sprintIdsByKrId = krs.isEmpty()
                ? Collections.emptyMap()
                : sprintKeyResultService.listByKeyResultIds(
                        krs.stream().map(KeyResult::getId).toList()).stream()
                        .collect(Collectors.groupingBy(
                                SprintKeyResult::getKeyResultId,
                                Collectors.mapping(SprintKeyResult::getSprintId, Collectors.toList())));
        Map<Long, List<String>> recordsByKrId = krs.isEmpty()
                ? Collections.emptyMap()
                : keyResultRecordService.listContentsByKeyResultIds(
                        krs.stream().map(KeyResult::getId).toList());
        return toVO(o, krs, sprintIdsByKrId, recordsByKrId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        loadOwnedObjective(id);
        // 存在已完成的关键成果时禁止删除（保护已落地成果）
        boolean hasDone = keyResultService.listByObjective(id).stream()
                .anyMatch(k -> KeyResultStatus.DONE.getValue().equals(k.getStatus()));
        if (hasDone) {
            throw new BusinessException(ResultCode.CONFLICT, "目标下存在已完成的关键成果，不可删除");
        }
        // 连带删除其下所有 K（多条写 → 事务）；K 的删除委托给 KeyResultService
        keyResultService.deleteByObjective(id);
        objectiveMapper.deleteById(id);
    }

    // ===================== 派生计算 =====================

    /**
     * 将 Objective + 其下 K 列表转为 VO，并计算 O 的派生字段。
     * <p>已取消的 K 不参与 progress/状态/完成时间的派生计算，但仍保留在 keyResults 列表中返回。
     *
     * @param o               目标实体
     * @param krs             其下 K 列表
     * @param sprintIdsByKrId 每个 K 关联的冲刺 ID 列表（按 keyResultId 索引），可为空 map
     * @param recordsByKrId   每个 K 的成果记录 R 列表（按 keyResultId 索引），可为空 map
     */
    private ObjectiveVO toVO(Objective o, List<KeyResult> krs, Map<Long, List<Long>> sprintIdsByKrId,
                             Map<Long, List<String>> recordsByKrId) {
        List<KeyResult> sorted = krs.stream()
                .sorted(Comparator.comparing(KeyResult::getId))
                .toList();

        // 有效 K = 非取消的 K（取消的不参与进度/状态/完成时间派生）
        List<KeyResult> effective = sorted.stream()
                .filter(k -> !KeyResultStatus.CANCELLED.getValue().equals(k.getStatus()))
                .toList();
        int cancelledCount = (int) sorted.stream()
                .filter(k -> KeyResultStatus.CANCELLED.getValue().equals(k.getStatus()))
                .count();

        int total = effective.size();
        long doneCount = effective.stream().filter(k -> KeyResultStatus.DONE.getValue().equals(k.getStatus())).count();
        int progress = total == 0 ? 0 : (int) (doneCount * 100 / total);

        String status = sorted.isEmpty() ? KeyResultStatus.NOT_STARTED.getValue() : deriveObjectiveStatus(effective);

        LocalDate planCompleteDate = effective.stream()
                .map(KeyResult::getPlanCompleteDate)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        LocalDate actualCompleteDate = null;
        if (total > 0 && doneCount == total) {
            actualCompleteDate = effective.stream()
                    .map(KeyResult::getCompleteDate)
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        List<KeyResultVO> krVOs = sorted.stream()
                .map(kr -> toKrVO(kr,
                        sprintIdsByKrId.getOrDefault(kr.getId(), Collections.emptyList()),
                        recordsByKrId.getOrDefault(kr.getId(), Collections.emptyList())))
                .toList();

        return ObjectiveVO.builder()
                .id(o.getId())
                .month(o.getMonth())
                .name(o.getName())
                .description(o.getDescription())
                .progress(progress)
                .status(status)
                .planCompleteDate(planCompleteDate)
                .actualCompleteDate(actualCompleteDate)
                .cancelledCount(cancelledCount)
                .keyResults(krVOs)
                .build();
    }

    /**
     * 派生 O 的状态（基于有效 K，即排除已取消的 K）。
     * <p>调用方需保证 effectiveKrs 非空（原始 krs 为空即无 K 时直接返回 NOT_STARTED，不进本方法）：
     * 有效 K 为空（即原始 K 全部已取消）→ 已取消；全未开始 → 未开始；全完成 → 已完成；否则进行中。
     */
    private String deriveObjectiveStatus(List<KeyResult> effectiveKrs) {
        if (effectiveKrs.isEmpty()) {
            // 原始 K 非空但有效 K 为空 → 全部已取消
            return KeyResultStatus.CANCELLED.getValue();
        }
        boolean allDone = effectiveKrs.stream().allMatch(k -> KeyResultStatus.DONE.getValue().equals(k.getStatus()));
        if (allDone) {
            return KeyResultStatus.DONE.getValue();
        }
        boolean allNotStarted = effectiveKrs.stream().allMatch(k -> KeyResultStatus.NOT_STARTED.getValue().equals(k.getStatus()));
        if (allNotStarted) {
            return KeyResultStatus.NOT_STARTED.getValue();
        }
        return KeyResultStatus.IN_PROGRESS.getValue();
    }

    private KeyResultVO toKrVO(KeyResult kr, List<Long> sprintIds, List<String> records) {
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

    // ===================== 辅助 =====================

    /**
     * 按 id 查询目标实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     */
    @Override
    public Objective getById(Long id, boolean checkOwnership) {
        if (checkOwnership) {
            return loadOwnedObjective(id);
        }
        return objectiveMapper.selectById(id);
    }

    private Objective loadOwnedObjective(Long id) {
        Objective o = objectiveMapper.selectById(id);
        if (o == null || !UserContextHolder.requireUserId().equals(o.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标不存在或无权访问");
        }
        return o;
    }

    private void validateMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "月份格式应为 YYYY-MM");
        }
    }
}
