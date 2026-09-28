package com.recall.service.holiday;

import com.recall.vo.holiday.HolidayDayVO;

import java.util.List;

/**
 * 法定节假日/补班 Service。
 * <p>
 * 数据按年缓存入库：首次访问某年时从 timor 节假日 API 拉取
 * （{@code https://timor.tech/api/holiday/year/{year}}，国务院公告发布后社区及时更新）；
 * 外部调用失败时降级返回内置兜底数据（2025/2026），不影响页面展示。
 * 国家政策每年不同，故不在前端硬编码，统一由本 Service 提供。
 *
 * @author recall
 */
public interface HolidayService {

    /**
     * 查询某年的法定节假日与补班日。
     *
     * @param year 年份
     * @return 节假日/补班日列表（按日期升序）
     */
    List<HolidayDayVO> getYear(int year);
}
