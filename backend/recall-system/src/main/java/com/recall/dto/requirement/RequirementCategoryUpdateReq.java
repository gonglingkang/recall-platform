package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 需求分类编辑请求。
 * <p>
 * 不含 parentId：父分类不允许变更。color 仅主分类可改（由 Service 按当前节点层级校验）。
 *
 * @author recall
 */
@Data
@Schema(description = "需求分类编辑请求")
public class RequirementCategoryUpdateReq {

    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    @Size(max = 20, message = "名称最长20字符")
    private String name;

    @Schema(description = "颜色;仅主分类可用,子分类强制为空")
    private String color;

    @Schema(description = "排序")
    private Integer sortOrder;
}
