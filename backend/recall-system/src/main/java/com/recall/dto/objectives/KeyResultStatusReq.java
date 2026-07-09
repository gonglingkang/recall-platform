package com.recall.dto.objectives;

import com.recall.enums.KeyResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 关键成果 K 状态切换请求（月度绩效 v2.0）。
 * <p>
 * 改变 K 进度的唯一入口：切换 status，后端自动维护 completeDate/cancelReason。
 *
 * @author recall
 */
@Data
@Schema(description = "关键成果状态切换请求")
public class KeyResultStatusReq {

    @Schema(description = "状态 not_started/in_progress/done/cancelled")
    @NotNull(message = "状态不能为空")
    private KeyResultStatus status;

    @Schema(description = "取消原因(仅切到 cancelled 时写入，最长500字符)")
    @Size(max = 500, message = "取消原因最长500字符")
    private String cancelReason;

    @Schema(description = "成果记录R(仅切到 done 时生效：传了全量覆盖旧R，不传或空清空旧R；每条最长2000字符)")
    @Size(max = 50, message = "成果记录最多50条")
    private List<@Size(max = 2000, message = "成果记录单条最长2000字符") String> records;
}

