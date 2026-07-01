package com.recall.vo.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 关键成果 K VO（月度绩效 v2.0）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "关键成果")
public class KeyResultVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 not_started/in_progress/done/cancelled")
    private String status;

    @Schema(description = "计划完成时间")
    private LocalDate planCompleteDate;

    @Schema(description = "实际完成时间(后端管理)")
    private LocalDate completeDate;

    @Schema(description = "取消原因(状态为已取消时填)")
    private String cancelReason;

    @Schema(description = "关联的冲刺任务ID列表")
    private List<Long> sprintIds;
}
