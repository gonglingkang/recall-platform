package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 评分答案：概率加权期望分（可落在整数档之间）、图例与各档概率。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreAnswer extends Answer {

    /** 期望分（概率加权平均，可为小数） */
    private double score;

    /** 置信度 0~1 */
    private double confidence;

    /** 评分图例：档位 → 说明（key 为档位字符串，如 "0"/"1"/"2"） */
    private Map<String, Object> legend;

    /** 各档概率（key 与 legend 一致，合计约 1） */
    private Map<String, Double> probabilities;
}
