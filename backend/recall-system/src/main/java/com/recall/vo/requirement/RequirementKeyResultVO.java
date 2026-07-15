package com.recall.vo.requirement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 需求绑定的关键成果 K 摘要 VO（仅 id/name/status，供需求列表/详情轻量展示）。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "需求绑定的关键成果摘要")
public class RequirementKeyResultVO {

    @Schema(description = "关键成果ID")
    private Long id;

    @Schema(description = "关键成果名称")
    private String name;

    @Schema(description = "关键成果状态 0未开始/1进行中/2已完成/3已取消")
    private String status;
}
