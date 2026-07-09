package com.recall.dto.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 日报保存请求（v1.0，全量覆盖）。
 * <p>
 * 按 date 整体提交当天所有工作内容，后端先删后插覆盖。
 * items 为空列表会被 {@code @NotEmpty} 拒绝（不允许提交空日报）。
 *
 * @author recall
 */
@Data
@Schema(description = "日报保存请求(全量覆盖)")
public class DailyReportSaveReq {

    @Schema(description = "工作内容列表(全量覆盖)")
    @NotEmpty(message = "工作内容不能为空")
    @Valid
    private List<DailyReportItemReq> items;
}
