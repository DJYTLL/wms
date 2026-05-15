package com.example.wms.dto;

import java.time.Instant;
import java.util.List;

/**

 * 租户列设置用于返回接口响应数据。

 */
public record TenantColumnSettingResponse(
    /**
     * 表示页码键名。
     */
    String pageKey,
    /**
     * 表示可见列。
     */
    List<String> visibleColumns,
    /**
     * 表示更新人。
     */
    String updatedBy,
    /**
     * 表示更新时间。
     */
    Instant updatedAt
) {
}
