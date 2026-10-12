package com.recall.dto.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 日报条目 AI 合规改写请求。
 * <p>改写以条目原文为主素材，关联待办标题/描述为辅助上下文（防止改写歪曲事实）。
 *
 * @author recall
 */
@Data
@Schema(description = "日报条目 AI 合规改写请求")
public class DailyRewriteReq {

    @Schema(description = "日报条目原文（含禁用词）")
    @NotBlank(message = "改写内容不能为空")
    @Size(max = 2000, message = "改写内容最长2000字符")
    private String content;

    @Schema(description = "关联待办标题（辅助上下文，可空）")
    @Size(max = 200, message = "待办标题最长200字符")
    private String todoTitle;

    @Schema(description = "关联待办描述（辅助上下文，可空）")
    @Size(max = 2000, message = "待办描述最长2000字符")
    private String todoNote;
}
