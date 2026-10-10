package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 绩效表格文本解析请求（目标导入第一步：粘贴文本 → LLM 结构化预览，不写库）。
 *
 * @author recall
 */
@Data
@Schema(description = "绩效表格文本解析请求")
public class ObjectiveImportParseReq {

    @Schema(description = "从公司绩效表格复制的文本（TAB 分隔的 维度/目标O/关键成果/权重 结构）")
    @NotBlank(message = "粘贴内容不能为空")
    @Size(max = 20000, message = "粘贴内容不能超过20000字符")
    private String text;
}
