package com.recall.service.objectives;

import com.recall.dto.objectives.ObjectiveCreateReq;
import com.recall.dto.objectives.ObjectiveUpdateReq;
import com.recall.entity.objectives.Objective;
import com.recall.vo.objectives.ObjectiveVO;

import java.util.List;

/**
 * 月度绩效目标 O Service（v2.0）。
 * <p>
 * 核心特性：目标 O 的进度/状态/完成时间由其下关键成果 K 派生计算（查询时实时聚合）。
 * 数据隔离：按 user_id + month 双重隔离。
 * <p>
 * K 的增删改查、状态切换由 {@link KeyResultService} 维护；本 Service 仅在派生计算 O 时
 * 调用 KeyResultService 获取 K 数据，删除 O 时调用其连带删除 K。
 * <p>
 * 注：{@link #getById(Long, boolean)} 返回 entity 仅供 Service 间内部调用（如 KeyResultService
 * 创建 K 时校验归属、取 month），不得透传至 Controller/前端。
 *
 * @author recall
 */
public interface ObjectiveService {

    /**
     * 查询某月目标列表（每个 O 内嵌其下所有 K，派生字段已算好）。
     *
     * @param month 月份，格式 yyyy-MM
     * @return 目标列表
     */
    List<ObjectiveVO> list(String month);

    /**
     * 新增目标 O（名称在同用户同月内唯一，冲突抛 409）。
     *
     * @param req 创建请求
     * @return 创建后的目标
     */
    ObjectiveVO create(ObjectiveCreateReq req);

    /**
     * 编辑目标 O（仅 name/description，名称改为与同月另一目标同名时抛 409）。
     *
     * @param id  目标 ID
     * @param req 编辑请求
     * @return 更新后的目标
     */
    ObjectiveVO update(Long id, ObjectiveUpdateReq req);

    /**
     * 删除目标 O（连带删除其下所有 K，通过调用 KeyResultService）。
     * <p>存在已完成的关键成果时禁止删除，抛 409。
     *
     * @param id 目标 ID
     */
    void delete(Long id);

    /**
     * 按 id 查询目标实体（供 Service 间内部调用，禁止透传至 Controller/前端）。
     *
     * @param id            目标 ID
     * @param checkOwnership 是否校验归属当前用户；为 true 时查不到或不属于当前用户均抛 404
     * @return 目标实体；checkOwnership=false 且查不到时返回 null
     */
    Objective getById(Long id, boolean checkOwnership);
}
