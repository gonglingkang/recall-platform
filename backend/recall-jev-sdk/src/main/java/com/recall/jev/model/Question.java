package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.recall.jev.exception.JevException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * System One 问题（noul 是/否、choice 选择、score 评分），对应 Python SDK 的 Question。
 * <p>
 * instructions/criteria 为 JSONContent（字符串、对象或数组），null 字段不上报 wire。
 * 通过静态工厂创建，wire 形态与 OpenAPI 一致：{"type":..., "instructions":..., "criteria":...}。
 *
 * @author recall
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Question {

    public static final String TYPE_NOUL = "noul";
    public static final String TYPE_CHOICE = "choice";
    public static final String TYPE_SCORE = "score";

    private String type;
    private Object instructions;
    private Object criteria;

    public Question() {
    }

    private Question(String type, Object instructions, Object criteria) {
        this.type = type;
        this.instructions = instructions;
        this.criteria = criteria;
    }

    /** 是/否问题（对应 Noul），说明可选 */
    public static Question noul(Object instructions) {
        return new Question(TYPE_NOUL, instructions, null);
    }

    /** 是/否问题，附是/否结果说明（对应 NoulCriteria） */
    public static Question noul(Object instructions, Object trueDescription, Object falseDescription) {
        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("true", trueDescription);
        criteria.put("false", falseDescription);
        return new Question(TYPE_NOUL, instructions, criteria);
    }

    /** 选择题：标签 → 描述（null 表示仅按标签名理解） */
    public static Question choice(Object instructions, Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new JevException("Choice question requires non-empty criteria.");
        }
        return new Question(TYPE_CHOICE, instructions, new LinkedHashMap<>(criteria));
    }

    /** 评分题：有序评分说明列表（下标即分值，从 0 起），至少一条 */
    public static Question score(Object instructions, List<Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new JevException("Score question has no criteria; at least one score is required.");
        }
        return new Question(TYPE_SCORE, instructions, List.copyOf(criteria));
    }

    public String getType() {
        return type;
    }

    public Object getInstructions() {
        return instructions;
    }

    public Object getCriteria() {
        return criteria;
    }
}
