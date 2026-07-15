package com.recall.vo.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求分类 VO（含子分类树）。
 * <p>
 * 两级分类合并后，主分类与子分类共用此 VO；子分类节点的 subcategories 为空列表。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "需求分类(含子分类树)")
public class RequirementCategoryVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "父分类ID;null=主分类")
    private Long parentId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "颜色;仅主分类有值")
    private String color;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "子分类列表(仅主分类有值)")
    private List<RequirementCategoryVO> subcategories;
}
