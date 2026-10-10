package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Token 用量（API 未上报时字段为 null）。
 *
 * @author recall
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usage {

    private Long inputTokens;

    private Long outputTokens;
}
