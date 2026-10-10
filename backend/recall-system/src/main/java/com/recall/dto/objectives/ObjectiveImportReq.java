package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 绩效目标批量导入请求（目标导入第二步：按预览结构建目标与关键成果）。
 *
 * @author recall
 */
@Data
@Schema(description = "绩效目标批量导入请求")
public class ObjectiveImportReq {

    @Schema(description = "导入月份 YYYY-MM")
    @NotBlank(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String month;

    @Schema(description = "是否覆盖导入：true 时先清空该月全部已有目标及其关键成果（前端须已向用户确认）")
    private boolean replace = false;

    @Schema(description = "目标条目列表")
    @NotEmpty(message = "导入目标不能为空")
    @Size(max = 50, message = "单次导入目标数不能超过50个")
    private List<@Valid ObjectiveImportItemReq> objectives;
}
