package com.recall.vo.sprint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队冲刺任务 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "团队冲刺任务")
public class SprintItemVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "月份 YYYY-MM")
    private String month;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态 0未开始/1进行中/2已完成")
    private String status;

    @Schema(description = "是否需我介入")
    private Boolean needInvolved;

    @Schema(description = "备注/说明")
    private String note;

    @Schema(description = "关联的关键成果ID列表")
    private List<Long> keyResultIds;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
