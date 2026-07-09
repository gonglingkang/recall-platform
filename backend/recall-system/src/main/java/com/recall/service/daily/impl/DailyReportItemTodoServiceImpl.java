package com.recall.service.daily.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.context.UserContextHolder;
import com.recall.dao.daily.DailyReportItemTodoMapper;
import com.recall.entity.daily.DailyReportItemTodo;
import com.recall.service.daily.DailyReportItemTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 日报项-待办关联 Service 实现，封装 daily_report_item_todos 表的数据访问。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class DailyReportItemTodoServiceImpl implements DailyReportItemTodoService {

    private final DailyReportItemTodoMapper dailyReportItemTodoMapper;

    @Override
    public List<Long> listTodoIdsByItemId(Long itemId) {
        List<DailyReportItemTodo> rels = dailyReportItemTodoMapper.selectList(
                new LambdaQueryWrapper<DailyReportItemTodo>()
                        .eq(DailyReportItemTodo::getItemId, itemId)
                        .orderByAsc(DailyReportItemTodo::getId));
        return rels.stream().map(DailyReportItemTodo::getTodoId).toList();
    }

    @Override
    public Map<Long, List<Long>> listTodoIdsByItemIds(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DailyReportItemTodo> rels = dailyReportItemTodoMapper.selectList(
                new LambdaQueryWrapper<DailyReportItemTodo>()
                        .in(DailyReportItemTodo::getItemId, itemIds)
                        .orderByAsc(DailyReportItemTodo::getId));
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (DailyReportItemTodo r : rels) {
            result.computeIfAbsent(r.getItemId(), k -> new ArrayList<>()).add(r.getTodoId());
        }
        return result;
    }

    /**
     * 全量覆盖：先按 itemId 删旧、再批量插新。
     * <p>
     * 删 + 插为多条写，加事务保证原子性。被上层事务覆盖（REQUIRED 传播）。
     *
     * @param itemId  日报项 ID
     * @param todoIds 待办 ID 列表；null 或空表示清空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveByItemId(Long itemId, List<Long> todoIds) {
        dailyReportItemTodoMapper.delete(new LambdaQueryWrapper<DailyReportItemTodo>()
                .eq(DailyReportItemTodo::getItemId, itemId));
        if (todoIds == null || todoIds.isEmpty()) {
            return;
        }
        Long userId = UserContextHolder.requireUserId();
        for (Long todoId : todoIds) {
            DailyReportItemTodo rel = new DailyReportItemTodo();
            rel.setUserId(userId);
            rel.setItemId(itemId);
            rel.setTodoId(todoId);
            dailyReportItemTodoMapper.insert(rel);
        }
    }

    @Override
    public void deleteByItemId(Long itemId) {
        dailyReportItemTodoMapper.delete(new LambdaQueryWrapper<DailyReportItemTodo>()
                .eq(DailyReportItemTodo::getItemId, itemId));
    }

    @Override
    public void deleteByItemIds(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        dailyReportItemTodoMapper.delete(new LambdaQueryWrapper<DailyReportItemTodo>()
                .in(DailyReportItemTodo::getItemId, itemIds));
    }

    @Override
    public void deleteByTodoId(Long todoId) {
        dailyReportItemTodoMapper.delete(new LambdaQueryWrapper<DailyReportItemTodo>()
                .eq(DailyReportItemTodo::getTodoId, todoId));
    }

    @Override
    public List<DailyReportItemTodo> listByItemIds(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return dailyReportItemTodoMapper.selectList(new LambdaQueryWrapper<DailyReportItemTodo>()
                .in(DailyReportItemTodo::getItemId, itemIds)
                .orderByAsc(DailyReportItemTodo::getId));
    }
}
