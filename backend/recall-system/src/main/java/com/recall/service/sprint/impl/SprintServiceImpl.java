package com.recall.service.sprint.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.api.ResultCode;
import com.recall.common.context.UserContextHolder;
import com.recall.common.exception.BusinessException;
import com.recall.dao.sprint.SprintItemMapper;
import com.recall.dto.sprint.SprintCreateReq;
import com.recall.dto.sprint.SprintInvolvedReq;
import com.recall.dto.sprint.SprintLinkReq;
import com.recall.dto.sprint.SprintStatusReq;
import com.recall.dto.sprint.SprintUpdateReq;
import com.recall.entity.sprint.SprintItem;
import com.recall.entity.sprint.SprintKeyResult;
import com.recall.entity.objectives.KeyResult;
import com.recall.enums.KeyResultStatus;
import com.recall.enums.SprintStatus;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.sprint.SprintKeyResultService;
import com.recall.service.sprint.SprintService;
import com.recall.vo.plan.MonthCompletionCountVO;
import com.recall.vo.sprint.SprintItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 团队冲刺 Service 实现。
 * <p>
 * 按 user_id + month 隔离。冲刺任务可关联多个关键成果 K（通过 sprint_key_results 关联表）。
 * 关联 K 后，K 状态变更会通过 {@link #syncStatusByKeyResult} 联动同步冲刺状态。
 *
 * @author recall
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintItemMapper sprintItemMapper;
    private final SprintKeyResultService sprintKeyResultService;
    @Lazy
    private final KeyResultService keyResultService;

    @Override
    public List<SprintItemVO> list(String month, Boolean needInvolved) {
        Long userId = UserContextHolder.requireUserId();
        LambdaQueryWrapper<SprintItem> wrapper = new LambdaQueryWrapper<SprintItem>()
                .eq(SprintItem::getUserId, userId)
                .eq(SprintItem::getMonth, month);
        if (Boolean.TRUE.equals(needInvolved)) {
            wrapper.eq(SprintItem::getNeedInvolved, true);
        }
        wrapper.orderByAsc(SprintItem::getId);
        List<SprintItem> items = sprintItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查出关联的 keyResultId，按 sprintId 分组
        List<Long> sprintIds = items.stream().map(SprintItem::getId).toList();
        List<SprintKeyResult> links = sprintKeyResultService.listBySprintIds(sprintIds);
        Map<Long, List<Long>> krMap = links.stream()
                .collect(Collectors.groupingBy(
                        SprintKeyResult::getSprintId,
                        Collectors.mapping(SprintKeyResult::getKeyResultId, Collectors.toList())));
        return items.stream().map(i -> toVO(i, krMap.getOrDefault(i.getId(), Collections.emptyList()))).toList();
    }

    @Override
    public List<MonthCompletionCountVO> countInvolvedByMonthRange(String startMonth, String endMonth) {
        // 只取需我介入项：无需我介入的冲刺既不可手动改状态也不可关联 K，状态恒为未开始，计入分母只会无条件拉低完成率
        List<SprintItem> items = sprintItemMapper.selectList(new LambdaQueryWrapper<SprintItem>()
                .select(SprintItem::getMonth, SprintItem::getStatus)
                .eq(SprintItem::getUserId, UserContextHolder.requireUserId())
                .eq(SprintItem::getNeedInvolved, true)
                .between(SprintItem::getMonth, startMonth, endMonth));
        return items.stream()
                .collect(Collectors.groupingBy(SprintItem::getMonth))
                .entrySet().stream()
                .map(e -> {
                    MonthCompletionCountVO count = new MonthCompletionCountVO();
                    count.setMonth(e.getKey());
                    count.setTotal(e.getValue().size());
                    count.setDone(e.getValue().stream()
                            .filter(i -> SprintStatus.DONE.getValue().equals(i.getStatus()))
                            .count());
                    return count;
                })
                .toList();
    }

    @Override
    public SprintItemVO create(SprintCreateReq req) {
        Long userId = UserContextHolder.requireUserId();
        // 同用户同月标题唯一
        if (sprintItemMapper.existsByName(userId, req.getMonth(), req.getTitle(), null)) {
            throw new BusinessException(ResultCode.CONFLICT, "冲刺任务标题已存在: " + req.getTitle());
        }
        SprintItem item = new SprintItem();
        item.setUserId(userId);
        item.setMonth(req.getMonth());
        item.setTitle(req.getTitle());
        item.setNeedInvolved(false);
        item.setNote(req.getNote());
        item.setStatus(SprintStatus.NOT_STARTED.getValue());
        sprintItemMapper.insert(item);
        return toVO(item, Collections.emptyList());
    }

    @Override
    public SprintItemVO update(Long id, SprintUpdateReq req) {
        SprintItem item = loadOwned(id);
        // 标题变更需校验同用户同月唯一
        if (!req.getTitle().equals(item.getTitle())
                && sprintItemMapper.existsByName(item.getUserId(), item.getMonth(), req.getTitle(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "冲刺任务标题已存在: " + req.getTitle());
        }
        item.setTitle(req.getTitle());
        if (req.getNote() != null) item.setNote(req.getNote());
        sprintItemMapper.updateById(item);
        return toVO(item, listKeyResultIds(id));
    }

    @Override
    public SprintItemVO changeStatus(Long id, SprintStatusReq req) {
        SprintItem item = loadOwned(id);
        // 仅需我介入的冲刺任务才可更改状态
        if (!Boolean.TRUE.equals(item.getNeedInvolved())) {
            throw new BusinessException(ResultCode.CONFLICT, "无需我介入的冲刺任务不可更改状态");
        }
        // 已关联关键成果的冲刺任务，状态由 K 联动派生，禁止手动变更
        if (!listKeyResultIds(id).isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "已关联关键成果的冲刺任务状态由关键成果联动，不可手动变更");
        }
        // 严格状态机校验（仅未开始→进行中、进行中→已完成、已完成→进行中合法）
        SprintStatus current = SprintStatus.of(item.getStatus());
        SprintStatus target = req.getStatus();
        validateStatusTransition(current, target);
        item.setStatus(target.getValue());
        sprintItemMapper.updateById(item);
        return toVO(item, listKeyResultIds(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SprintItemVO toggleInvolved(Long id, SprintInvolvedReq req) {
        SprintItem item = loadOwned(id);
        item.setNeedInvolved(req.getNeedInvolved());
        item.setStatus(SprintStatus.NOT_STARTED.getValue());
        if (!Boolean.TRUE.equals(req.getNeedInvolved())) {
            // true→false：清空关联的 K
            sprintKeyResultService.deleteBySprintId(id);
        }
        sprintItemMapper.updateById(item);
        return toVO(item, listKeyResultIds(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SprintItemVO linkKeyResults(Long id, SprintLinkReq req) {
        SprintItem item = loadOwned(id);
        if (!Boolean.TRUE.equals(item.getNeedInvolved())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "需先标记为需我介入，才可关联关键成果");
        }
        // 去除 null 与重复元素，避免脏关联；keyResultIds 为 null 视为空（清除全部关联）
        List<Long> krIds = req.getKeyResultIds() == null
                ? Collections.emptyList()
                : req.getKeyResultIds().stream().filter(Objects::nonNull).distinct().toList();
        // 全量覆盖：先删后插
        sprintKeyResultService.deleteBySprintId(id);
        if (!krIds.isEmpty()) {
            // 一次批量查询完成归属校验 + 状态派生（替代逐条 for 循环）
            List<KeyResult> krs = keyResultService.listByIds(krIds, true);
            sprintKeyResultService.saveAll(id, krIds);
            item.setStatus(deriveSprintStatus(krs));
        } else {
            // 取消全部关联，状态回未开始
            item.setStatus(SprintStatus.NOT_STARTED.getValue());
        }
        sprintItemMapper.updateById(item);
        return toVO(item, krIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        loadOwned(id);
        // 连带删除关联关系
        sprintKeyResultService.deleteBySprintId(id);
        sprintItemMapper.deleteById(id);
    }

    /**
     * 关键成果 K 状态变更后同步关联的冲刺任务状态。
     * <p>反查受影响冲刺后委托 {@link #recomputeStatus} 批量重算（派生规则一致）。
     *
     * @param krId     发生状态变更的关键成果 ID
     * @param krStatus 关键成果新状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncStatusByKeyResult(Long krId, KeyResultStatus krStatus) {
        List<Long> sprintIds = sprintKeyResultService.listSprintIdsByKeyResultId(krId);
        if (sprintIds.isEmpty()) {
            return;
        }
        recomputeStatus(sprintIds);
        log.info("关键成果 K={} 状态变更为 {}，重新派生并同步 {} 个冲刺任务", krId, krStatus.getValue(), sprintIds.size());
    }

    /**
     * 按 sprintId 列表批量重算冲刺状态。
     * <p>读查询全部批量执行（冲刺/关联/K 各 1 次），循环内仅内存派生 + 逐条更新，避免 N+1。
     * 派生规则：已取消的 K 不参与统计，剩余有效 K 全未开始→未开始；全已完成→已完成；其他→进行中；无有效 K→未开始。
     *
     * @param sprintIds 需重算的冲刺 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recomputeStatus(List<Long> sprintIds) {
        if (sprintIds == null || sprintIds.isEmpty()) {
            return;
        }
        // 1) 批量查出受影响的冲刺任务
        List<SprintItem> items = sprintItemMapper.selectBatchIds(sprintIds);
        if (items.isEmpty()) {
            return;
        }
        List<Long> existSprintIds = items.stream().map(SprintItem::getId).toList();
        // 2) 批量查出这些冲刺的全部关联记录，按 sprintId 分组
        Map<Long, List<Long>> krIdsBySprint = sprintKeyResultService.listBySprintIds(existSprintIds).stream()
                .collect(Collectors.groupingBy(
                        SprintKeyResult::getSprintId,
                        Collectors.mapping(SprintKeyResult::getKeyResultId, Collectors.toList())));
        // 3) 汇总去重后一次查出全部 K 实体，转成 id→KeyResult 映射
        List<Long> allKrIds = krIdsBySprint.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        Map<Long, KeyResult> krMap = allKrIds.isEmpty()
                ? Collections.emptyMap()
                : keyResultService.listByIds(allKrIds, false).stream()
                        .collect(Collectors.toMap(KeyResult::getId, kr -> kr));
        // 4) 内存派生 + 逐条更新（写操作无法批量，因每个冲刺状态可能不同）
        for (SprintItem item : items) {
            List<Long> krIds = krIdsBySprint.getOrDefault(item.getId(), Collections.emptyList());
            List<KeyResult> krs = krIds.stream().map(krMap::get).filter(Objects::nonNull).toList();
            item.setStatus(deriveSprintStatus(krs));
            sprintItemMapper.updateById(item);
        }
    }

    // ===================== 辅助 =====================

    private SprintItem loadOwned(Long id) {
        SprintItem item = sprintItemMapper.selectById(id);
        if (item == null || !UserContextHolder.requireUserId().equals(item.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "冲刺任务不存在或无权访问");
        }
        return item;
    }

    /**
     * 校验冲刺任务状态流转是否合法（严格状态机）。
     * <p>合法流转：
     * <ul>
     *   <li>未开始(0) → 进行中(1)：开始</li>
     *   <li>进行中(1) → 已完成(2)：完成</li>
     *   <li>已完成(2) → 进行中(1)：返工（取消完成）</li>
     * </ul>
     * 其余流转（未开始→已完成、进行中→未开始、已完成→未开始、同状态等）非法，抛 409。
     *
     * @param current 当前状态
     * @param target  目标状态
     */
    private void validateStatusTransition(SprintStatus current, SprintStatus target) {
        boolean legal = switch (current) {
            case NOT_STARTED -> target == SprintStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == SprintStatus.DONE;
            case DONE -> target == SprintStatus.IN_PROGRESS;
        };
        if (!legal) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "状态流转非法：" + current.getValue() + " → " + target.getValue());
        }
    }

    /**
     * 根据关联的 K 状态派生冲刺状态（新规则，已批量查出的 K 实体直接传入，不再逐条查库）。
     * <p>规则（排除已取消的 K 不参与统计）：
     * <ul>
     *   <li>无有效 K（全部已取消或列表为空）→ 未开始</li>
     *   <li>剩余 K 全未开始 → 未开始</li>
     *   <li>剩余 K 全已完成 → 已完成</li>
     *   <li>其他（含未开始与已完成混合、含进行中）→ 进行中</li>
     * </ul>
     *
     * @param krs 已查出的关键成果实体列表
     * @return 冲刺状态码
     */
    private String deriveSprintStatus(List<KeyResult> krs) {
        if (krs == null || krs.isEmpty()) {
            return SprintStatus.NOT_STARTED.getValue();
        }
        // 排除已取消的 K，不参与统计
        List<KeyResult> effective = krs.stream()
                .filter(kr -> !KeyResultStatus.CANCELLED.getValue().equals(kr.getStatus()))
                .toList();
        if (effective.isEmpty()) {
            // 全部已取消，视为无有效 K
            return SprintStatus.NOT_STARTED.getValue();
        }
        boolean allNotStarted = effective.stream()
                .allMatch(kr -> KeyResultStatus.NOT_STARTED.getValue().equals(kr.getStatus()));
        if (allNotStarted) {
            return SprintStatus.NOT_STARTED.getValue();
        }
        boolean allDone = effective.stream()
                .allMatch(kr -> KeyResultStatus.DONE.getValue().equals(kr.getStatus()));
        if (allDone) {
            return SprintStatus.DONE.getValue();
        }
        return SprintStatus.IN_PROGRESS.getValue();
    }

    private List<Long> listKeyResultIds(Long sprintId) {
        return sprintKeyResultService.listKeyResultIdsBySprintId(sprintId);
    }

    private SprintItemVO toVO(SprintItem i, List<Long> keyResultIds) {
        return SprintItemVO.builder()
                .id(i.getId())
                .month(i.getMonth())
                .title(i.getTitle())
                .status(i.getStatus())
                .needInvolved(i.getNeedInvolved())
                .note(i.getNote())
                .keyResultIds(keyResultIds)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
