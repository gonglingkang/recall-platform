package com.recall.vo.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 日报工作内容项 VO（v1.0）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "日报工作内容项")
public class DailyReportItemVO {

    @Schema(description = "项ID")
    private Long id;

    @Schema(description = "工作内容")
    private String content;

    @Schema(description = "进度百分比 0-100")
    private int progress;

    @Schema(description = "关联的待办概要")
    private List<RelatedTodoVO> todos;
}
