package com.recall.vo.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 目标 O VO（月度绩效 v2.0）。
 * <p>
 * progress / status / planCompleteDate / actualCompleteDate 为派生字段，
 * 由其下关键成果 K 实时聚合计算。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "月度绩效目标")
public class ObjectiveVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "月份 YYYY-MM")
    private String month;

    @Schema(description = "目标名称")
    private String name;

    @Schema(description = "目标描述")
    private String description;

    @Schema(description = "进度 0-100(派生)")
    private int progress;

    @Schema(description = "状态 not_started/in_progress/done/cancelled(派生)")
    private String status;

    @Schema(description = "计划完成时间(派生)")
    private LocalDate planCompleteDate;

    @Schema(description = "实际完成时间(派生,全K完成时)")
    private LocalDate actualCompleteDate;

    @Schema(description = "已取消的关键成果数量(派生)")
    private int cancelledCount;

    @Schema(description = "关键成果列表")
    private List<KeyResultVO> keyResults;
}
