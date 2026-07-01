package com.recall.service.sprint.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.dao.sprint.SprintKeyResultMapper;
import com.recall.entity.sprint.SprintKeyResult;
import com.recall.service.sprint.SprintKeyResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 冲刺-关键成果关联 Service 实现，封装 sprint_key_results 关联表的数据访问。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class SprintKeyResultServiceImpl implements SprintKeyResultService {

    private final SprintKeyResultMapper sprintKeyResultMapper;

    @Override
    public List<SprintKeyResult> listBySprintIds(List<Long> sprintIds) {
        if (sprintIds == null || sprintIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sprintKeyResultMapper.selectList(new LambdaQueryWrapper<SprintKeyResult>()
                .in(SprintKeyResult::getSprintId, sprintIds));
    }

    @Override
    public List<SprintKeyResult> listByKeyResultIds(List<Long> keyResultIds) {
        if (keyResultIds == null || keyResultIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sprintKeyResultMapper.selectList(new LambdaQueryWrapper<SprintKeyResult>()
                .in(SprintKeyResult::getKeyResultId, keyResultIds));
    }

    @Override
    public List<Long> listKeyResultIdsBySprintId(Long sprintId) {
        List<SprintKeyResult> links = sprintKeyResultMapper.selectList(new LambdaQueryWrapper<SprintKeyResult>()
                .eq(SprintKeyResult::getSprintId, sprintId));
        return links.stream().map(SprintKeyResult::getKeyResultId).toList();
    }

    @Override
    public List<Long> listSprintIdsByKeyResultId(Long keyResultId) {
        return sprintKeyResultMapper.selectSprintIdsByKeyResultId(keyResultId);
    }

    @Override
    public void saveAll(Long sprintId, List<Long> keyResultIds) {
        if (keyResultIds == null || keyResultIds.isEmpty()) {
            return;
        }
        for (Long krId : keyResultIds) {
            SprintKeyResult link = new SprintKeyResult();
            link.setSprintId(sprintId);
            link.setKeyResultId(krId);
            sprintKeyResultMapper.insert(link);
        }
    }

    @Override
    public void deleteBySprintId(Long sprintId) {
        sprintKeyResultMapper.delete(new LambdaQueryWrapper<SprintKeyResult>()
                .eq(SprintKeyResult::getSprintId, sprintId));
    }

    @Override
    public void deleteByKeyResultId(Long keyResultId) {
        sprintKeyResultMapper.delete(new LambdaQueryWrapper<SprintKeyResult>()
                .eq(SprintKeyResult::getKeyResultId, keyResultId));
    }

    @Override
    public void deleteByKeyResultIds(List<Long> keyResultIds) {
        if (keyResultIds == null || keyResultIds.isEmpty()) {
            return;
        }
        sprintKeyResultMapper.delete(new LambdaQueryWrapper<SprintKeyResult>()
                .in(SprintKeyResult::getKeyResultId, keyResultIds));
    }
}
