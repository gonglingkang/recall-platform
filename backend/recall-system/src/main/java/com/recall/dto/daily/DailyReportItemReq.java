package com.recall.dto.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 日报工作内容项请求（v1.0）。
 *
 * @author recall
 */
@Data
@Schema(description = "日报工作内容项")
public class DailyReportItemReq {

    @Schema(description = "工作内容")
    @NotBlank(message = "工作内容不能为空")
    @Size(max = 2000, message = "内容最长2000字符")
    private String content;

    @Schema(description = "进度百分比 0-100")
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private int progress;

    @Schema(description = "关联的待办ID列表(可空,不关联则不传)")
    private List<Long> todoIds;
}
