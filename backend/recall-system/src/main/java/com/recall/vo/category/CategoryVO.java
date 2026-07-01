package com.recall.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类 VO（含子分类树，PRD 6.4 / 11.3）。
 * <p>
 * 两层分类合并后，大分类与子分类共用此 VO；子分类节点的 subcategories 为空列表。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "分类（含子分类树）")
public class CategoryVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "父分类ID;null=大分类")
    private Long parentId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "颜色;仅大分类有值")
    private String color;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "子分类列表(仅大分类有值)")
    private List<CategoryVO> subcategories;
}
