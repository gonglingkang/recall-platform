package com.recall.service.daily;

import com.recall.dto.daily.DailyRewriteReq;

/**
 * 日报条目 AI 合规改写 Service。
 * <p>
 * 公司要求日报不得出现特定词汇（见 {@link DailyForbiddenWords}）；本服务用 LLM 网关
 * 对命中禁用词的条目做措辞改写：只换说法不换事实，改写结果仍过禁用词校验兜底。
 *
 * @author recall
 */
public interface DailyRewriteService {

    /**
     * 改写日报条目为合规表述。
     *
     * @param req 条目原文 + 关联待办标题/描述（辅助上下文）
     * @return 改写后的条目文本（已通过禁用词校验）
     */
    String rewrite(DailyRewriteReq req);
}
