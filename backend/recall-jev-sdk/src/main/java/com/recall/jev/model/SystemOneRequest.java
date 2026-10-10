package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * System One 请求体：{"state":..., "model":..., "questions":...}。
 *
 * @author recall
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SystemOneRequest {

    /** 待评估内容：字符串、JSON 对象或数组 */
    private Object state;

    private String model;

    /** 问题集合，key=答案标识名 */
    private Map<String, Question> questions;
}
