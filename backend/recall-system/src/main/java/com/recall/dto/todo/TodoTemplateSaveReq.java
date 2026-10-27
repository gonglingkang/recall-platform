package com.recall.dto.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 周期待办模板保存请求（新增/编辑共用）。
 * <p>周期类型：DAILY 每天；WEEKLY 每周（recurrenceValue=周几 1-7 逗号分隔，周一=1）；
 * MONTHLY 每月（recurrenceValue=几号 1-31 逗号分隔）。
 *
 * @author recall
 */
@Data
@Schema(description = "周期待办模板保存请求")
public class TodoTemplateSaveReq {

    @Schema(description = "待办标题")
    @NotBlank(message = "待办标题不能为空")
    @Size(max = 100, message = "标题最长100字符")
    private String title;

    @Schema(description = "备注（可空）")
    @Size(max = 500, message = "备注最长500字符")
    private String note;

    @Schema(description = "分类ID（可空=未分类）")
    private Long categoryId;

    @Schema(description = "优先级 0低/1中/2高")
    @NotBlank(message = "优先级不能为空")
    @Pattern(regexp = "^[012]$", message = "优先级取值非法")
    private String priority;

    @Schema(description = "周期类型 DAILY/WEEKLY/MONTHLY")
    @NotBlank(message = "周期类型不能为空")
    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY)$", message = "周期类型取值非法")
    private String recurrenceType;

    @Schema(description = "周期值：WEEKLY=周几1-7逗号分隔(周一=1)；MONTHLY=几号1-31逗号分隔；DAILY 传 *")
    private String recurrenceValue;

    @Schema(description = "周期日内生成时间点 HH:mm（默认 09:00）")
    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "时间格式应为 HH:mm")
    private String recurrenceTime;

    @Schema(description = "是否启用（默认启用）")
    private Boolean enabled = true;
}
