package com.recall.dto.todo;

import com.recall.common.api.PageQuery;
import com.recall.enums.Priority;
import com.recall.enums.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 待办列表查询请求。所有过滤条件均可空，同时非空时 AND 生效。
 *
 * @author recall
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "待办列表查询请求")
public class TodoListReq extends PageQuery {

    @Schema(description = "创建起始日期 YYYY-MM-DD，按 createdAt >= 当日 00:00 过滤")
    private LocalDate startDate;

    @Schema(description = "创建结束日期 YYYY-MM-DD，按 createdAt <= 当日 23:59:59 过滤")
    private LocalDate endDate;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "优先级 HIGH/MEDIUM/LOW")
    private Priority priority;

    @Schema(description = "完成状态 PENDING/DONE；不传查全部")
    private TodoStatus status;

    @Schema(description = "搜索关键词（匹配标题+备注）")
    private String keyword;
}
