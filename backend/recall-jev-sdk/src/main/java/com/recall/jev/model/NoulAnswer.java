package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 是/否答案：noul 为「是」的概率，0~1，接近 0.5 表示不确定。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoulAnswer extends Answer {

    private double noul;
}
