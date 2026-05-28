package com.example.wms.dto;

import java.time.Instant;

/**

 * 系统配置用于返回接口响应数据。

 */
public record SystemConfigResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示键名。
     */
    String key,
    /**
     * 表示值。
     */
    String value,
    /**
     * 表示值类型。
     */
    String valueType,
    /**
     * 表示描述。
     */
    String description,
    /**
     * 表示是否公开。
     */
    boolean isPublic,
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
