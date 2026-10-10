package com.recall.jev.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 模型元数据。
 *
 * @author recall
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelMetadata {

    /** 模型名或别名（用于 systemOne 的 model 字段），如 jev-latest */
    private String name;

    private String description;

    /** 发布日期 YYYY-MM-DD */
    private String releaseDate;
}
