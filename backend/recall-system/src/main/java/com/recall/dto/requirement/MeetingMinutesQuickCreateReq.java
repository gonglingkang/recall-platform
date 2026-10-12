package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 会议纪要快捷新建需求请求（新建并立即绑定该纪要）。
 * <p>首次需求时间取当天；初始状态讨论中（复用需求创建逻辑）。
 *
 * @author recall
 */
@Data
@Schema(description = "会议纪要快捷新建需求请求")
public class MeetingMinutesQuickCreateReq {

    @Schema(description = "需求标题")
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    @Schema(description = "需求主分类ID（必选）")
    @NotNull(message = "需求主分类不能为空")
    private Long categoryId;

    @Schema(description = "需求子分类ID（可选）")
    private Long subCategoryId;
}
