package com.recall.vo.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 绩效全局搜索命中项 VO（粒度=关键成果 KR）。
 * <p>命中来源可以是 O 名/描述、KR 名/描述或 R 内容；命中后展示该 KR 的全部成果记录。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "绩效全局搜索命中项")
public class ObjectiveSearchHitVO {

    @Schema(description = "所属月份 YYYY-MM")
    private String month;

    @Schema(description = "目标 ID")
    private Long objectiveId;

    @Schema(description = "目标名称")
    private String objectiveName;

    @Schema(description = "关键成果 ID")
    private Long krId;

    @Schema(description = "关键成果名称")
    private String krName;

    @Schema(description = "关键成果描述")
    private String krDescription;

    @Schema(description = "关键成果状态")
    private String krStatus;

    @Schema(description = "取消原因（仅已取消的 KR 有值）")
    private String cancelReason;

    @Schema(description = "全部成果记录内容（按提交顺序）")
    private List<String> records;
}
