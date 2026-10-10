package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 可用模型清单（GET /v1/models 响应）。
 *
 * @author recall
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListModelsResponse {

    private List<ModelMetadata> models;
}
