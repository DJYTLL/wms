package com.example.wms.dto;

import java.util.List;

/**

 * 分页响应体，用于返回分页查询结果和分页元信息。

 */
public record PageResponse<T>(
    /**
     * 表示合计数量或总数。
     */
    long total,
    /**
     * 表示当前页码。
     */
    long page,
    /**
     * 表示每页条数。
     */
    long size,
    /**
     * 表示明细项列表。
     */
    List<T> items
) {
}
