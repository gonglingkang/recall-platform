package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 会议纪要保存请求（推送入库与手动补录共用）。
 * <p>纪要 = type=3 的需求文档，未绑定需求时 requirement_id 为 NULL（在纪要池中）。
 *
 * @author recall
 */
@Data
@Schema(description = "会议纪要保存请求")
public class MeetingMinutesSaveReq {

    @Schema(description = "纪要标题")
    @NotBlank(message = "纪要标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    @Schema(description = "纪要链接")
    @NotBlank(message = "纪要链接不能为空")
    @Size(max = 2000, message = "链接最长2000字符")
    private String url;

    @Schema(description = "会议日期（推送未带时用推送日/当天兜底）")
    private LocalDate meetingDate;
}
