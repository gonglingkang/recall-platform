package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 导入的目标条目（含其关键成果），归属 {@link ObjectiveImportReq}。
 *
 * @author recall
 */
@Data
@Schema(description = "导入的目标条目")
public class ObjectiveImportItemReq {

    @Schema(description = "目标名称")
    @NotBlank(message = "目标名称不能为空")
    @Size(max = 100, message = "目标名称最长100字符")
    private String name;

    @Schema(description = "维度名称（如 常规指标（员工）70%），写入目标描述")
    @Size(max = 200, message = "维度名称最长200字符")
    private String dimension;

    @Schema(description = "权重原文（如 50%），写入目标描述")
    @Size(max = 50, message = "权重最长50字符")
    private String weight;

    @Schema(description = "关键成果名称列表；允许为空（支持只建目标O不建关键成果）")
    private List<@NotBlank(message = "关键成果名称不能为空") @Size(max = 100, message = "关键成果名称最长100字符") String> keyResults;
}
