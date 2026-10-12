package com.recall.service.daily;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 日报禁用词（公司合规要求，配置于 recall.daily.forbidden-words）。
 * <p>
 * 保存兜底校验、前端实时提示与 AI 改写共用同一份清单；匹配忽略大小写
 * （兼容 PPT/Deepseek 等英文词的大小写变体）。
 *
 * @author recall
 */
@Component
public class DailyForbiddenWords {

    private final List<String> words;

    public DailyForbiddenWords(@Value("${recall.daily.forbidden-words:}") String config) {
        this.words = Arrays.stream(config.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<String> getWords() {
        return words;
    }

    /**
     * 返回文本中命中的禁用词（按配置顺序去重，忽略大小写）。
     *
     * @param content 待检测文本，可为 null
     * @return 命中的禁用词列表，无命中返回空列表
     */
    public List<String> findHits(String content) {
        if (content == null || content.isBlank() || words.isEmpty()) {
            return List.of();
        }
        String lower = content.toLowerCase();
        return words.stream().filter(w -> lower.contains(w.toLowerCase())).toList();
    }
}
