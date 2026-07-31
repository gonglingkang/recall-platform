package com.recall.service.sprint;

import com.recall.dto.sprint.SprintCreateReq;
import com.recall.dto.sprint.SprintInvolvedReq;
import com.recall.dto.sprint.SprintLinkReq;
import com.recall.dto.sprint.SprintStatusReq;
import com.recall.dto.sprint.SprintUpdateReq;
import com.recall.enums.KeyResultStatus;
import com.recall.vo.plan.MonthCompletionCountVO;
import com.recall.vo.sprint.SprintItemVO;

import java.util.List;

/**
 * 团队冲刺 Service。
 * <p>
 * 按 user_id + month 隔离。冲刺任务可关联多个关键成果 K；
 * 关联 K 后，K 状态变更会联动同步冲刺状态（通过 {@link #syncStatusByKeyResult} 触发）。
 *
 * @author recall
 */
public interface SprintService {

    /**
     * 某月冲刺列表，可按「需我介入」过滤。
     *
     * @param month        月份，格式 yyyy-MM
     * @param needInvolved 是否仅返回需我介入项；为 null 表示不过滤
     * @return 冲刺列表
     */
    List<SprintItemVO> list(String month, Boolean needInvolved);

    /**
     * 按月份区间统计「需我介入」冲刺的完成计数（供月度趋势聚合，查询次数与月份跨度无关）。
     * <p>无需我介入的冲刺不可能变为已完成（既禁止手动改状态，也禁止关联 K），故不计入 total；
     * 区间内无数据的月份不会返回对应行，由调用方按缺省处理。
     *
     * @param startMonth 起始月份 YYYY-MM（含）
     * @param endMonth   截止月份 YYYY-MM（含）
     * @return 按月分组的完成计数，仅含有数据的月份
     */
    List<MonthCompletionCountVO> countInvolvedByMonthRange(String startMonth, String endMonth);

    /**
     * 新建冲刺项（只需 month + title + note）。
     *
     * @param req 创建请求
     * @return 新建的冲刺项
     */
    SprintItemVO create(SprintCreateReq req);

    /**
     * 更新冲刺项（title/note）。
     *
     * @param id  冲刺项 ID
     * @param req 编辑请求
     * @return 更新后的冲刺项
     */
    SprintItemVO update(Long id, SprintUpdateReq req);

    /**
     * 切换冲刺项状态（用户手动切换：未开始/进行中/已完成）。
     * <p>仅 need_involved=true 的冲刺任务允许更改状态，否则抛 409。
     * <p>已关联关键成果的冲刺任务状态由 K 联动派生，禁止手动变更，否则抛 409。
     * <p>手动变更遵循严格状态机：未开始→进行中、进行中→已完成、已完成→进行中（返工）合法；
     * 其余流转（未开始→已完成、进行中→未开始、已完成→未开始、同状态等）抛 409。
     *
     * @param id  冲刺项 ID
     * @param req 状态请求参数
     * @return 更新后的冲刺项
     */
    SprintItemVO changeStatus(Long id, SprintStatusReq req);

    /**
     * 切换「需我介入」标记（true→false 时清空关联的 K）。
     *
     * @param id  冲刺项 ID
     * @param req 介入请求参数
     * @return 更新后的冲刺项
     */
    SprintItemVO toggleInvolved(Long id, SprintInvolvedReq req);

    /**
     * 全量覆盖关联关键成果（仅 need_involved=true 时允许）。
     *
     * @param id  冲刺项 ID
     * @param req 关联请求（keyResultIds 空列表=取消全部）
     * @return 更新后的冲刺项
     */
    SprintItemVO linkKeyResults(Long id, SprintLinkReq req);

    /**
     * 删除冲刺项（连带删除关联关系）。
     *
     * @param id 冲刺项 ID
     */
    void delete(Long id);

    /**
     * 关键成果 K 状态变更后同步关联的冲刺任务状态（供 KeyResultService 调用）。
     * <p>对每个关联冲刺，重新按其当前全部关联 K 的状态派生冲刺状态（与 linkKeyResults 同一套规则）：
     * 已取消的 K 不参与统计，剩余有效 K 全未开始→未开始；全已完成→已完成；其他→进行中；无有效 K→未开始。
     *
     * @param krId     发生状态变更的关键成果 ID
     * @param krStatus 关键成果新状态
     */
    void syncStatusByKeyResult(Long krId, KeyResultStatus krStatus);

    /**
     * 按 sprintId 列表批量重算冲刺状态（供 K 删除/级联删除场景调用）。
     * <p>对每个冲刺，按其当前全部关联 K 的状态派生（与 linkKeyResults / syncStatusByKeyResult 同一套规则）。
     * 读查询全部批量执行，不在循环内查询。
     *
     * @param sprintIds 需重算的冲刺 ID 列表；为空不执行
     */
    void recomputeStatus(List<Long> sprintIds);
}
