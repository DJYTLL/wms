package com.example.wms.dto;

import java.time.Instant;

/**

 * 审计日志用于返回接口响应数据。

 */
public record AuditLogResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示租户 ID。
     */
    Long tenantId,
    /**
     * 表示租户编码。
     */
    String tenantCode,
    /**
     * 表示鉴权租户 ID。
     */
    Long authTenantId,
    /**
     * 表示鉴权租户编码。
     */
    String authTenantCode,
    /**
     * 表示跨租户租户。
     */
    boolean crossTenant,
    /**
     * 表示操作人用户名。
     */
    String actorUsername,
    /**
     * 表示操作动作。
     */
    String action,
    /**
     * 表示实体类型。
     */
    String entityType,
    /**
     * 表示实体 ID。
     */
    String entityId,
    /**
     * 表示详情。
     */
    String detail,
    /**
     * 表示删除原因。
     */
    String deleteReason,
    /**
     * 表示状态。
     */
    String status,
    /**
     * 表示请求 ID。
     */
    String requestId,
    /**
     * 表示客户端IP。
     */
    String clientIp,
    /**
     * 表示用户浏览器标识。
     */
    String userAgent,
    /**
     * 表示方式。
     */
    String method,
    /**
     * 表示路径。
     */
    String path,
    /**
     * 表示HTTP状态。
     */
    Integer httpStatus,
    /**
     * 表示错误编码。
     */
    String errorCode,
    /**
     * 表示错误消息。
     */
    String errorMessage,
    /**
     * 表示耗时毫秒。
     */
    Long durationMs,
    /**
     * 表示创建时间。
     */
    Instant createdAt
) {
}
