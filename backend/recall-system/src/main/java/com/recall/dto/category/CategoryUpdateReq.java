package com.recall.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类编辑请求（PRD 6.4.1）。
 * <p>
 * 不含 parentId：父分类不允许变更。color 仅大分类可改（由 Service 按当前节点层级校验）。
 *
 * @author recall
 */
@Data
@Schema(description = "分类编辑请求")
public class CategoryUpdateReq {

    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    @Size(max = 20, message = "名称最长20字符")
    private String name;

    @Schema(description = "颜色;仅大分类可用,子分类强制为空")
    private String color;

    @Schema(description = "排序")
    private Integer sortOrder;
}
