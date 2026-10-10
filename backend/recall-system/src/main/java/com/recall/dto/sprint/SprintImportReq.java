package com.recall.dto.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 团队冲刺任务批量导入请求（粘贴编号列表批量建任务）。
 *
 * @author recall
 */
@Data
@Schema(description = "团队冲刺任务批量导入请求")
public class SprintImportReq {

    @Schema(description = "导入月份 YYYY-MM")
    @NotBlank(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String month;

    @Schema(description = "任务标题列表")
    @NotEmpty(message = "导入任务不能为空")
    @Size(max = 100, message = "单次导入任务数不能超过100个")
    private List<@NotBlank(message = "任务标题不能为空") @Size(max = 100, message = "标题最长100字符") String> titles;
}
