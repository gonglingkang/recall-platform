package com.recall.dto.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 请假保存请求（upsert：不存在则建，存在则覆盖）。
 *
 * @author recall
 */
@Data
@Schema(description = "请假保存请求")
public class DailyLeaveSaveReq {

    @Schema(description = "请假类型：1病假 2年假 3事假 4育儿假")
    @NotNull(message = "请假类型不能为空")
    private Integer leaveType;

    @Schema(description = "请假时段：1全天 2上午 3下午")
    @NotNull(message = "请假时段不能为空")
    private Integer period;

    @Schema(description = "请假事由(选填，仅作查看记录)")
    @Size(max = 500, message = "请假事由不能超过500字")
    private String reason;
}
