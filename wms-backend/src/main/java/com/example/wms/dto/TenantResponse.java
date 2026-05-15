package com.example.wms.dto;

import java.time.Instant;

/**

 * 租户用于返回接口响应数据。

 */
public record TenantResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示业务编码。
     */
    String code,
    /**
     * 表示名称。
     */
    String name,
    /**
     * 表示是否启用。
     */
    boolean enabled,
    /**
     * 表示创建时间。
     */
    Instant createdAt,
    /**
     * 表示更新时间。
     */
    Instant updatedAt
) {
}
