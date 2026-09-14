package com.recall.service.daily;

import com.recall.dto.daily.DailyLeaveSaveReq;
import com.recall.entity.daily.DailyLeaveRecord;
import com.recall.vo.daily.DailyLeaveVO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 日报请假记录 Service。
 * <p>
 * 请假是个人日报的纯记录（无审批流，审批以 OA 为准），一天一条，支持全天/半天。
 * 核心约束：
 * <ul>
 *   <li>所有查询/操作强制按当前用户 userId 过滤（数据隔离）。</li>
 *   <li>leave_date 不可为未来（复用日报 4601）。</li>
 *   <li>保存为 upsert：同用户同日期不存在则建，存在则覆盖。</li>
 * </ul>
 * <p>
 * 本 Service 是 DailyLeaveRecordMapper 的唯一归属方，
 * 日报详情组装请假信息经本 Service 暴露的方法完成。
 *
 * @author recall
 */
public interface DailyLeaveService {

    /**
     * 保存（upsert）指定日期的请假记录：不存在则建，存在则覆盖。
     * <p>
     * 校验：date 不可为未来（4601）；请假类型/时段数字码不合法（4603）。
     *
     * @param date 请假日期 YYYY-MM-DD
     * @param req  保存请求
     * @return 保存后的请假记录视图
     */
    DailyLeaveVO save(LocalDate date, DailyLeaveSaveReq req);

    /**
     * 删除指定日期的请假记录（物理删除），不存在或越权抛 404。
     *
     * @param date 请假日期 YYYY-MM-DD
     */
    void delete(LocalDate date);

    /**
     * 查询当前用户某月的全部请假记录（按日期升序）。
     * <p>独立于日报存在：没写日报但请了假的天也会返回。
     *
     * @param month 月份 YYYY-MM
     * @return 请假记录视图列表
     */
    List<DailyLeaveVO> listByMonth(String month);

    /**
     * 查询当前用户指定日期的请假记录。
     *
     * @param date 请假日期
     * @return 请假记录实体；无请假返回 null
     */
    DailyLeaveRecord getByDate(LocalDate date);

    /**
     * 批量查询当前用户多天的请假记录（避免日报列表 N+1）。
     *
     * @param dates 请假日期集合
     * @return 日期 -> 请假记录实体；无记录的日期不在 Map 中
     */
    Map<LocalDate, DailyLeaveRecord> mapByDates(Collection<LocalDate> dates);

    /**
     * 请假记录实体转 VO（补充类型/时段中文名称）。
     *
     * @param record 请假记录实体
     * @return 请假记录视图
     */
    DailyLeaveVO toVO(DailyLeaveRecord record);
}
