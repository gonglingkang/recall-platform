package com.recall.service.stats.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.context.UserContextHolder;
import com.recall.dao.category.CategoryMapper;
import com.recall.dao.todo.TodoMapper;
import com.recall.entity.category.Category;
import com.recall.entity.todo.Todo;
import com.recall.enums.TodoStatus;
import com.recall.service.stats.StatsService;
import com.recall.vo.stats.TodayStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计 Service 实现（PRD 6.7）。
 * <p>
 * 今日概览已实现；趋势/分类占比为 P2 待补。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final TodoMapper todoMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public TodayStatsVO today() {
        Long userId = UserContextHolder.requireUserId();
        LocalDate today = LocalDate.now();
        // 今日待办（pending + done）：按 createdAt 自然日过滤（00:00 ~ 次日 00:00）
        List<Todo> todos = todoMapper.selectList(new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .ge(Todo::getCreatedAt, today.atStartOfDay())
                .lt(Todo::getCreatedAt, today.plusDays(1).atStartOfDay())
                .in(Todo::getStatus, TodoStatus.PENDING.getValue(), TodoStatus.DONE.getValue()));

        int total = todos.size();
        int done = (int) todos.stream().filter(t -> TodoStatus.DONE.getValue().equals(t.getStatus())).count();
        double rate = total == 0 ? 0 : (double) done / total;

        // 按大分类分布（PRD 6.7.1）
        List<TodayStatsVO.CategoryCount> categoryCounts = buildCategoryCounts(todos, userId);

        return TodayStatsVO.builder()
                .total(total)
                .done(done)
                .rate(round(rate))
                .categoryCounts(categoryCounts)
                .build();
    }

    private List<TodayStatsVO.CategoryCount> buildCategoryCounts(List<Todo> todos, Long userId) {
        if (todos.isEmpty()) {
            return List.of();
        }
        // 按 categoryId 计数（null 归为未分类）
        Map<Long, Long> countByCat = todos.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategoryId() == null ? 0L : t.getCategoryId(),
                        Collectors.counting()));

        // 取分类名（未分类用"未分类"）
        List<Long> catIds = countByCat.keySet().stream().filter(id -> id != 0L).toList();
        Map<Long, String> nameMap = new HashMap<>();
        if (!catIds.isEmpty()) {
            for (Category c : categoryMapper.selectBatchIds(catIds)) {
                nameMap.put(c.getId(), c.getName());
            }
        }

        List<TodayStatsVO.CategoryCount> result = new ArrayList<>();
        countByCat.forEach((catId, count) -> {
            String name = catId == 0L ? "未分类" : nameMap.getOrDefault(catId, "已删除分类");
            result.add(TodayStatsVO.CategoryCount.builder()
                    .categoryId(catId == 0L ? null : catId)
                    .categoryName(name)
                    .count(count.intValue())
                    .build());
        });
        return result;
    }

    private double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
