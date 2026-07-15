package com.recall.dto.requirement;

import com.recall.enums.RequirementDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 需求文档新增请求。
 * <p>文档仅存外部链接（标题 + URL + 文档时间），不存文件本体。
 *
 * @author recall
 */
@Data
@Schema(description = "需求文档新增请求")
public class RequirementDocumentCreateReq {

    @Schema(description = "文档类型 1原型设计/2需求文档/3会议纪要")
    @NotNull(message = "文档类型不能为空")
    private RequirementDocumentType type;

    @Schema(description = "文档标题")
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    @Schema(description = "外部链接")
    @NotBlank(message = "链接不能为空")
    @Size(max = 2000, message = "链接最长2000字符")
    private String url;

    @Schema(description = "文档时间")
    @NotNull(message = "文档时间不能为空")
    private LocalDate documentDate;
}
