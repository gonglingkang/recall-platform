package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 目标 O 编辑请求（月度绩效 v2.0）。
 * <p>
 * name 必填；description 可选，为 null 表示不改。派生字段与月份均不可改。
 * name 改为与同月另一目标同名时由 Service 校验冲突。
 *
 * @author recall
 */
@Data
@Schema(description = "目标编辑请求")
public class ObjectiveUpdateReq {

    @Schema(description = "目标名称")
    @NotBlank(message = "目标名称不能为空")
    @Size(max = 100, message = "目标名称最长100字符")
    private String name;

    @Schema(description = "目标描述")
    @Size(max = 2000, message = "目标描述最长2000字符")
    private String description;
}
