package com.recall.service.objectives;

import com.recall.vo.objectives.ObjectiveSearchHitVO;

import java.util.List;

/**
 * 绩效全局搜索 Service：跨月份按关键词检索目标 O / 关键成果 K / 成果记录 R。
 * <p>
 * 命中粒度为 KR：任一层命中即返回该 KR，并携带其全部成果记录（用户可借命中的 R
 * 查看同 K 的其他 R）。月份倒序，条数有上限；LIKE 检索，个人数据规模无需全文索引。
 *
 * @author recall
 */
public interface ObjectiveSearchService {

    /**
     * 全局搜索绩效（当前用户）。
     *
     * @param keyword 关键词（去空格后须非空，长度上限 50）
     * @return 命中的 KR 列表（月份倒序，同月内按目标/K 录入顺序），最多 50 条
     */
    List<ObjectiveSearchHitVO> search(String keyword);
}
