package com.recall.dto.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切换「需我介入」请求（PRD 11.4 PATCH /api/sprint/items/:id/involved）。
 *
 * @author recall
 */
@Data
@Schema(description = "切换需我介入请求")
public class SprintInvolvedReq {

    @Schema(description = "是否需我介入")
    @NotNull(message = "needInvolved 不能为空")
    private Boolean needInvolved;
}
