package com.recall.dto.requirement;

import com.recall.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 需求分页查询请求。所有过滤条件均可空，同时非空时 AND 生效。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "需求分页查询请求")
public class RequirementPageReq extends PageQuery {

    @Schema(description = "首次需求时间起始日期 YYYY-MM-DD，按 firstDemandDate >= 当日过滤")
    private LocalDate startDate;

    @Schema(description = "首次需求时间结束日期 YYYY-MM-DD，按 firstDemandDate <= 当日过滤")
    private LocalDate endDate;

    @Schema(description = "状态列表(多选)，0讨论中/1不涉及/2进行中/3开发完成/4验收完成/5发布完成；不传查全部")
    private List<String> statuses;

    @Schema(description = "搜索关键词（匹配标题）")
    private String keyword;

    @Schema(description = "需求主分类ID(筛选时联动其下所有子分类)")
    private Long categoryId;

    @Schema(description = "需求子分类ID(精确筛选)")
    private Long subCategoryId;
}
