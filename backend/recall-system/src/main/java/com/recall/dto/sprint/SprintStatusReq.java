package com.recall.dto.sprint;

import com.recall.enums.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 冲刺状态变更请求（PRD 11.4 PATCH /api/sprint/items/:id/status）。
 *
 * @author recall
 */
@Data
@Schema(description = "冲刺状态变更请求")
public class SprintStatusReq {

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private SprintStatus status;
}
