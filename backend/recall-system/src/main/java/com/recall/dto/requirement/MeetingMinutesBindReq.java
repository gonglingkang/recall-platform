package com.recall.dto.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会议纪要绑定需求请求。
 *
 * @author recall
 */
@Data
@Schema(description = "会议纪要绑定需求请求")
public class MeetingMinutesBindReq {

    @Schema(description = "目标需求 ID")
    @NotNull(message = "需求ID不能为空")
    private Long requirementId;
}
