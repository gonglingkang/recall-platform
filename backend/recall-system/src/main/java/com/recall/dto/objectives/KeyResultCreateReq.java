package com.recall.dto.objectives;

import com.recall.enums.KeyResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 关键成果 K 创建请求（月度绩效 v2.0）。
 * <p>
 * 创建时需传 objectiveId 指定归属目标。
 * 注意：completeDate 由后端管理，本请求不接受该字段。
 *
 * @author recall
 */
@Data
@Schema(description = "关键成果创建请求")
public class KeyResultCreateReq {

    @Schema(description = "归属目标ID")
    @NotNull(message = "归属目标ID不能为空")
    private Long objectiveId;

    @Schema(description = "关键成果名称")
    @NotBlank(message = "关键成果名称不能为空")
    @Size(max = 100, message = "名称最长100字符")
    private String name;

    @Schema(description = "描述")
    @Size(max = 2000, message = "描述最长2000字符")
    private String description;

    @Schema(description = "状态 not_started/in_progress/done，默认 not_started；创建时不允许 cancelled")
    private KeyResultStatus status;

    @Schema(description = "计划完成时间")
    private LocalDate planCompleteDate;
}
