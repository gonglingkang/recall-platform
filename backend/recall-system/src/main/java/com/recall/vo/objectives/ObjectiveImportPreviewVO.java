package com.recall.vo.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 绩效表格文本解析预览 VO（不写库，前端确认后随导入请求回传）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "绩效表格解析预览")
public class ObjectiveImportPreviewVO {

    @Schema(description = "目标名称")
    private String name;

    @Schema(description = "维度名称")
    private String dimension;

    @Schema(description = "权重原文")
    private String weight;

    @Schema(description = "关键成果名称列表")
    private List<String> keyResults;
}
