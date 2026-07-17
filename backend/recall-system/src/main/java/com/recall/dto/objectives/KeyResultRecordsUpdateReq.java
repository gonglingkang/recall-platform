package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 关键成果成果记录 R 全量更新请求。
 * <p>
 * 对指定 K 的所有 R 进行全量覆盖：传了覆盖旧 R，传空清空旧 R。不改 K 状态。
 *
 * @author recall
 */
@Data
@Schema(description = "关键成果成果记录R全量更新请求")
public class KeyResultRecordsUpdateReq {

    @Schema(description = "成果记录R列表(全量覆盖：传了覆盖旧R，空清空旧R；每条最长2000字符)")
    @NotNull(message = "成果记录不能为空(可传空数组清空)")
    @Size(max = 50, message = "成果记录最多50条")
    private List<@Size(max = 2000, message = "成果记录单条最长2000字符") String> records;
}
