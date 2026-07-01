package com.recall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 偏好设置请求（PRD 6.9.3 / 11.6）。
 *
 * @author recall
 */
@Data
@Schema(description = "偏好设置请求")
public class PreferencesReq {

    @Schema(description = "提醒通知开关")
    private Boolean reminderEnabled;

    @Schema(description = "时区")
    private String timezone;

    @Schema(description = "主题")
    private String theme;
}
