package com.recall.service.objectives.impl;

import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.entity.objectives.KeyResult;
import com.recall.entity.objectives.Objective;
import com.recall.service.objectives.KeyResultRecordService;
import com.recall.service.objectives.KeyResultService;
import com.recall.service.objectives.ObjectiveSearchService;
import com.recall.service.objectives.ObjectiveService;
import com.recall.vo.objectives.ObjectiveSearchHitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 绩效全局搜索 Service 实现。
 * <p>
 * 三路命中取并集：O 名/描述命中（取其下全部 K）、K 名/描述命中、R 内容命中（取所属 K）；
 * 再补全每个命中 K 的目标信息与全部 R。各路查询经各自专属 Service 执行（Mapper 归属不变）。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class ObjectiveSearchServiceImpl implements ObjectiveSearchService {

    /** 内部各路检索的取数上限（最终结果另有 50 条截断） */
    private static final int QUERY_LIMIT = 500;
    /** 最终返回的 KR 条数上限 */
    private static final int RESULT_LIMIT = 50;

    private final ObjectiveService objectiveService;
    private final KeyResultService keyResultService;
    private final KeyResultRecordService keyResultRecordService;

    @Override
    public List<ObjectiveSearchHitVO> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "搜索关键词不能为空");
        }
        if (kw.length() > 50) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_FAILED, "搜索关键词最长50字符");
        }

        // 三路命中
        List<Objective> matchedObjectives = objectiveService.searchOwned(kw, QUERY_LIMIT);
        List<KeyResult> matchedKrs = keyResultService.searchOwned(kw, QUERY_LIMIT);
        List<Long> recordHitKrIds = keyResultRecordService.searchKeyResultIds(kw, QUERY_LIMIT);

        // 汇总命中 KR：目标命中的 K + K 名/描述命中的 K + R 内容命中的 K
        Map<Long, KeyResult> krById = new HashMap<>();
        Set<Long> objectiveIds = new HashSet<>();
        if (!matchedObjectives.isEmpty()) {
            matchedObjectives.forEach(o -> objectiveIds.add(o.getId()));
            keyResultService.listByObjectives(new ArrayList<>(objectiveIds)).forEach(kr -> krById.put(kr.getId(), kr));
        }
        matchedKrs.forEach(kr -> krById.put(kr.getId(), kr));
        if (!recordHitKrIds.isEmpty()) {
            // krIds 来自按用户过滤的 R 检索，无需再校验归属
            keyResultService.listByIds(recordHitKrIds, false).forEach(kr -> krById.put(kr.getId(), kr));
        }
        if (krById.isEmpty()) {
            return List.of();
        }
        krById.values().forEach(kr -> objectiveIds.add(kr.getObjectiveId()));

        // 目标信息补全（含未被关键词直接命中、仅因 R 命中而引入的目标）
        Map<Long, Objective> objectiveById = new HashMap<>();
        matchedObjectives.forEach(o -> objectiveById.put(o.getId(), o));
        for (Long objectiveId : objectiveIds) {
            if (!objectiveById.containsKey(objectiveId)) {
                Objective o = objectiveService.getById(objectiveId, false);
                if (o != null) {
                    objectiveById.put(objectiveId, o);
                }
            }
        }

        // 命中 K 的全部 R（用户要求：借命中的 R 可看同 K 的其他 R）
        List<Long> hitKrIds = new ArrayList<>(krById.keySet());
        Map<Long, List<String>> recordsByKrId = keyResultRecordService.listContentsByKeyResultIds(hitKrIds);

        return krById.values().stream()
                .filter(kr -> objectiveById.containsKey(kr.getObjectiveId()))
                .sorted(Comparator
                        .comparing((KeyResult kr) -> objectiveById.get(kr.getObjectiveId()).getMonth(),
                                Comparator.reverseOrder())
                        .thenComparing(KeyResult::getObjectiveId)
                        .thenComparing(KeyResult::getId))
                .limit(RESULT_LIMIT)
                .map(kr -> {
                    Objective o = objectiveById.get(kr.getObjectiveId());
                    return ObjectiveSearchHitVO.builder()
                            .month(o.getMonth())
                            .objectiveId(o.getId())
                            .objectiveName(o.getName())
                            .krId(kr.getId())
                            .krName(kr.getName())
                            .krDescription(kr.getDescription())
                            .krStatus(kr.getStatus())
                            .cancelReason(kr.getCancelReason())
                            .records(recordsByKrId.getOrDefault(kr.getId(), List.of()))
                            .build();
                })
                .toList();
    }
}
