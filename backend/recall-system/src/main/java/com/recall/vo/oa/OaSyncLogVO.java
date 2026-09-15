package com.recall.vo.oa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OA 同步日志视图。
 *
 * @author recall
 */
@Data
@Schema(description = "OA 同步日志视图")
public class OaSyncLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "平台周起始(周一)")
    private LocalDate weekStart;

    @Schema(description = "触发方式: 1自动 2手动")
    private Integer triggerType;

    @Schema(description = "状态: 1运行中 2成功 3失败")
    private Integer status;

    @Schema(description = "执行步骤(失败定位用)")
    private String step;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "OA 工时提交截止时间（同步时从表单读取）")
    private LocalDateTime deadline;

    @Schema(description = "同步时 OA 端该周工时是否已填报: true已提交 false未提交")
    private Boolean oaSubmitted;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
