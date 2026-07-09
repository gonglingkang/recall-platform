package com.recall.service.daily.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recall.common.context.UserContextHolder;
import com.recall.dao.daily.DailyReportItemMapper;
import com.recall.dto.daily.DailyReportItemReq;
import com.recall.entity.daily.DailyReportItem;
import com.recall.service.daily.DailyReportItemService;
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
 * 日报工作内容项 Service 实现，封装 daily_report_items 表的数据访问。
 * <p>
 * 关联数据经 {@link DailyReportItemTodoService} 管理；全量覆盖时先删旧关联再删旧项、插新项再插新关联。
 *
 * @author recall
 */
@Service
@RequiredArgsConstructor
public class DailyReportItemServiceImpl implements DailyReportItemService {

    private final DailyReportItemMapper dailyReportItemMapper;
    private final DailyReportItemTodoService dailyReportItemTodoService;

    @Override
    public List<DailyReportItem> listByReportId(Long reportId) {
        return dailyReportItemMapper.selectList(new LambdaQueryWrapper<DailyReportItem>()
                .eq(DailyReportItem::getReportId, reportId)
                .orderByAsc(DailyReportItem::getId));
    }

    @Override
    public Map<Long, List<DailyReportItem>> listByReportIds(List<Long> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DailyReportItem> items = dailyReportItemMapper.selectList(new LambdaQueryWrapper<DailyReportItem>()
                .in(DailyReportItem::getReportId, reportIds)
                .orderByAsc(DailyReportItem::getId));
        Map<Long, List<DailyReportItem>> result = new LinkedHashMap<>();
        for (DailyReportItem item : items) {
            result.computeIfAbsent(item.getReportId(), k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 全量覆盖指定日报的工作内容项：先删旧关联 -> 删旧项 -> 插新项 -> 插新关联。
     * <p>
     * 删 + 插为多条写，加事务保证原子性，被调用方上层事务覆盖（REQUIRED 传播）。
     *
     * @param reportId 日报 ID
     * @param items    工作内容项请求列表；空列表表示清空
     * @return 插入后的日报项实体列表（含 id）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DailyReportItem> replaceByReportId(Long reportId, List<DailyReportItemReq> items) {
        // 1. 查出旧日报项 id，用于级联删关联
        List<Long> oldItemIds = dailyReportItemMapper.selectList(
                        new LambdaQueryWrapper<DailyReportItem>()
                                .eq(DailyReportItem::getReportId, reportId))
                .stream().map(DailyReportItem::getId).toList();
        // 2. 删旧关联 + 删旧项
        if (!oldItemIds.isEmpty()) {
            dailyReportItemTodoService.deleteByItemIds(oldItemIds);
        }
        dailyReportItemMapper.delete(new LambdaQueryWrapper<DailyReportItem>()
                .eq(DailyReportItem::getReportId, reportId));
        // 3. 插新项
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        Long userId = UserContextHolder.requireUserId();
        List<DailyReportItem> inserted = new ArrayList<>(items.size());
        for (DailyReportItemReq req : items) {
            DailyReportItem item = new DailyReportItem();
            item.setUserId(userId);
            item.setReportId(reportId);
            item.setContent(req.getContent());
            item.setProgress(req.getProgress());
            dailyReportItemMapper.insert(item);
            inserted.add(item);
            // 4. 插新关联（去重，uk_item_todo 兜底）
            if (req.getTodoIds() != null && !req.getTodoIds().isEmpty()) {
                List<Long> distinctTodoIds = req.getTodoIds().stream().distinct().toList();
                dailyReportItemTodoService.saveByItemId(item.getId(), distinctTodoIds);
            }
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByReportId(Long reportId) {
        List<Long> itemIds = dailyReportItemMapper.selectList(
                        new LambdaQueryWrapper<DailyReportItem>()
                                .eq(DailyReportItem::getReportId, reportId))
                .stream().map(DailyReportItem::getId).toList();
        if (itemIds.isEmpty()) {
            return;
        }
        dailyReportItemTodoService.deleteByItemIds(itemIds);
        dailyReportItemMapper.delete(new LambdaQueryWrapper<DailyReportItem>()
                .eq(DailyReportItem::getReportId, reportId));
    }
}
