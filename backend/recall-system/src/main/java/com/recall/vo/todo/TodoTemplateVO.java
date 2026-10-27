package com.recall.vo.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 周期待办模板 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "周期待办模板")
public class TodoTemplateVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "待办标题")
    private String title;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "优先级 0低/1中/2高")
    private String priority;

    @Schema(description = "周期类型 DAILY/WEEKLY/MONTHLY")
    private String recurrenceType;

    @Schema(description = "周期值")
    private String recurrenceValue;

    @Schema(description = "周期日内生成时间点 HH:mm")
    private String recurrenceTime;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "下次生成日期")
    private LocalDate nextDate;

    @Schema(description = "最近生成的待办ID")
    private Long lastTodoId;

    @Schema(description = "最近生成的待办状态 0待处理/1已完成；从未生成或待办已删为 null")
    private String lastTodoStatus;
}
