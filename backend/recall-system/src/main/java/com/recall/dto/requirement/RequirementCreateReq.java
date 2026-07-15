package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * 需求创建请求。
 * <p>创建只需 title（+ 可选 description）；初始状态为讨论中。
 * 可选传入 keyResultId 绑定关键成果 K（绑定后状态跟 K 走）。
 *
 * @author recall
 */
@Data
@Schema(description = "需求创建请求")
public class RequirementCreateReq {

    @Schema(description = "需求标题")
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    @Schema(description = "需求描述")
    @Size(max = 2000, message = "描述最长2000字符")
    private String description;

    @Schema(description = "首次需求时间(默认当天，不能是未来的时间)")
    @NotNull(message = "首次需求时间不能为空")
    @PastOrPresent(message = "首次需求时间不能是未来的时间")
    private LocalDate firstDemandDate;

    @Schema(description = "绑定的关键成果K的ID(可选,传则绑定)")
    private Long keyResultId;

    @Schema(description = "需求主分类ID(必选)")
    @NotNull(message = "需求主分类不能为空")
    private Long categoryId;

    @Schema(description = "需求子分类ID(可选)")
    private Long subCategoryId;
}
