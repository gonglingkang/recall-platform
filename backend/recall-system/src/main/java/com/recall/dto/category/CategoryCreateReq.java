package com.recall.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类创建请求（PRD 6.4.1）。
 * <p>
 * parentId 为空创建大分类；非空创建子分类（最多两级，由 Service 校验）。
 *
 * @author recall
 */
@Data
@Schema(description = "分类创建请求")
public class CategoryCreateReq {

    @Schema(description = "父分类ID;空=大分类,非空=子分类")
    private Long parentId;

    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    @Size(max = 20, message = "名称最长20字符")
    private String name;

    @Schema(description = "颜色;仅大分类可用,子分类强制为空")
    private String color;

    @Schema(description = "排序")
    private Integer sortOrder;
}
