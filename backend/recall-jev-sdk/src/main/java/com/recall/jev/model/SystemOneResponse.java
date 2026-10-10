package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System One 响应：按题型分组的答案 + 模型与用量元数据（对应 SystemOneResponse）。
 *
 * @author recall
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemOneResponse {

    /** 实际应答的模型名 */
    private String model;

    private Usage usage;

    /** 全部答案，key=问题名 */
    private Map<String, Answer> answers = new LinkedHashMap<>();

    /** 是/否答案子集 */
    public Map<String, NoulAnswer> nouls() {
        Map<String, NoulAnswer> result = new LinkedHashMap<>();
        answers.forEach((name, answer) -> {
            if (answer instanceof NoulAnswer noul) {
                result.put(name, noul);
            }
        });
        return result;
    }

    /** 选择答案子集 */
    public Map<String, ChoiceAnswer> choices() {
        Map<String, ChoiceAnswer> result = new LinkedHashMap<>();
        answers.forEach((name, answer) -> {
            if (answer instanceof ChoiceAnswer choice) {
                result.put(name, choice);
            }
        });
        return result;
    }

    /** 评分答案子集 */
    public Map<String, ScoreAnswer> scores() {
        Map<String, ScoreAnswer> result = new LinkedHashMap<>();
        answers.forEach((name, answer) -> {
            if (answer instanceof ScoreAnswer score) {
                result.put(name, score);
            }
        });
        return result;
    }
}
