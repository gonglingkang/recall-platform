package com.recall.dto.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 关键成果 K 编辑请求（月度绩效 v2.0）。
 * <p>
 * 编辑不改变 status 和 completeDate（改状态请用状态切换接口）。
 *
 * @author recall
 */
@Data
@Schema(description = "关键成果编辑请求")
public class KeyResultUpdateReq {

    @Schema(description = "关键成果名称")
    @Size(max = 100, message = "名称最长100字符")
    private String name;

    @Schema(description = "描述")
    @Size(max = 2000, message = "描述最长2000字符")
    private String description;

    @Schema(description = "计划完成时间")
    private LocalDate planCompleteDate;
}
