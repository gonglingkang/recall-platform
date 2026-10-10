package com.recall.vo.objectives;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 绩效目标批量导入结果 VO。
 *
 * @author recall
 */
@Data
@Builder
@Schema(description = "绩效目标批量导入结果")
public class ObjectiveImportResultVO {

    @Schema(description = "创建目标数")
    private int objectives;

    @Schema(description = "创建关键成果数")
    private int keyResults;
}
