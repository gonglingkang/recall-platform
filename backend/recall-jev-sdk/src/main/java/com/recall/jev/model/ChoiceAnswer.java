package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 选择答案：命中的标签、置信度与各选项概率。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChoiceAnswer extends Answer {

    /** 概率最高的选项名 */
    private String choice;

    /** 置信度 0~1，值低时建议人工复核 */
    private double confidence;

    /** 各选项概率（key=选项名，合计约 1） */
    private Map<String, Double> probabilities;
}
