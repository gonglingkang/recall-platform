package com.recall.common.api;

import lombok.Data;

/**
 * 分页查询基类。Controller 层的查询 DTO 可继承本类获得分页能力。
 *
 * @author recall
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数，默认 20，上限 100（见 PRD 8.1：单页加载上限 100 条） */
    private Integer pageSize = 20;

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
