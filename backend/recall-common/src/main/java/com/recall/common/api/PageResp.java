package com.recall.common.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页响应对象。
 *
 * @param <T> 记录类型
 * @author recall
 */
@Data
@Builder
public class PageResp<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总条数 */
    private long total;

    /** 当前页码 */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /** 总页数 */
    private long pages;
}
