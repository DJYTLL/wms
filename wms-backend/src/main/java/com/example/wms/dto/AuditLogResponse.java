package com.example.wms.dto;

import java.time.Instant;

// 审计日志响应
public record AuditLogResponse(
    Long id,
    Long tenantId,
    String tenantCode,
    Long authTenantId,
    String authTenantCode,
    boolean crossTenant,
    String actorUsername,
    String action,
    String entityType,
    String entityId,
    String detail,
    String status,
    String requestId,
    String clientIp,
    String userAgent,
    String method,
    String path,
    Integer httpStatus,
    String errorCode,
    String errorMessage,
    Long durationMs,
    Instant createdAt
) {
}
