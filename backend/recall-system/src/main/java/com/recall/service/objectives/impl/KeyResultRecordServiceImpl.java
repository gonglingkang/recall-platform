package com.recall.service.objectives.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.context.UserContextHolder;
import com.recall.dao.objectives.KeyResultRecordMapper;
import com.recall.entity.objectives.KeyResultRecord;
import com.recall.service.objectives.KeyResultRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键成果成果记录 R Service 实现，封装 key_result_records 表的数据访问。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class KeyResultRecordServiceImpl implements KeyResultRecordService {

    private final KeyResultRecordMapper keyResultRecordMapper;

    @Override
    public List<String> listContentsByKeyResultId(Long keyResultId) {
        List<KeyResultRecord> records = keyResultRecordMapper.selectList(
                new LambdaQueryWrapper<KeyResultRecord>()
                        .eq(KeyResultRecord::getKeyResultId, keyResultId)
                        .orderByAsc(KeyResultRecord::getId));
        return records.stream().map(KeyResultRecord::getContent).toList();
    }

    @Override
    public Map<Long, List<String>> listContentsByKeyResultIds(List<Long> keyResultIds) {
        if (keyResultIds == null || keyResultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<KeyResultRecord> records = keyResultRecordMapper.selectList(
                new LambdaQueryWrapper<KeyResultRecord>()
                        .in(KeyResultRecord::getKeyResultId, keyResultIds)
                        .orderByAsc(KeyResultRecord::getId));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (KeyResultRecord r : records) {
            result.computeIfAbsent(r.getKeyResultId(), k -> new ArrayList<>()).add(r.getContent());
        }
        return result;
    }

    /**
     * 全量覆盖：先按 keyResultId 删旧、再批量插新。
     * <p>
     * 删 + 插为多条写，加事务保证原子性。被 KeyResultService.changeStatus 调用时共享上层事务。
     *
     * @param keyResultId 关键成果 ID
     * @param contents    R 内容列表；null 或空表示清空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceByKeyResultId(Long keyResultId, List<String> contents) {
        Long userId = UserContextHolder.requireUserId();
        keyResultRecordMapper.delete(new LambdaQueryWrapper<KeyResultRecord>()
                .eq(KeyResultRecord::getKeyResultId, keyResultId));
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (String content : contents) {
            KeyResultRecord r = new KeyResultRecord();
            r.setUserId(userId);
            r.setKeyResultId(keyResultId);
            r.setContent(content);
            keyResultRecordMapper.insert(r);
        }
    }

    @Override
    public void deleteByKeyResultId(Long keyResultId) {
        keyResultRecordMapper.delete(new LambdaQueryWrapper<KeyResultRecord>()
                .eq(KeyResultRecord::getKeyResultId, keyResultId));
    }

    @Override
    public void deleteByKeyResultIds(List<Long> keyResultIds) {
        if (keyResultIds == null || keyResultIds.isEmpty()) {
            return;
        }
        keyResultRecordMapper.delete(new LambdaQueryWrapper<KeyResultRecord>()
                .in(KeyResultRecord::getKeyResultId, keyResultIds));
    }
}
