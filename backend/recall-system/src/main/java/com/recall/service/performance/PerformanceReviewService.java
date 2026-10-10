package com.recall.service.performance;

import com.recall.vo.performance.PerformanceReviewVO;

/**
 * 绩效面谈内容生成 Service。
 * <p>
 * 汇总指定月份的绩效目标（O/KR）与日报工作内容作为素材，调用大模型生成
 * 面谈四部分内容。仅生成文本供用户复制到 OA 面谈表，不做 OA 同步、不落库。
 *
 * @author recall
 */
public interface PerformanceReviewService {

    /**
     * 生成某月的绩效面谈四部分内容。
     *
     * @param month 月份，格式 yyyy-MM
     * @return 四部分内容
     */
    PerformanceReviewVO generate(String month);
}
