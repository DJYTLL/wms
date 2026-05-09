package com.example.wms.service;

import com.example.wms.dto.AuditLogResponse;
import com.example.wms.dto.PageResponse;

import java.time.Instant;

// 审计日志服务接口
public interface AuditLogService {
    // 写入审计日志
    default void record(String action, String entityType, String entityId, String detail) {
        record(action, entityType, entityId, detail, "SUCCESS", 200, null, null, null);
    }

    // 写入审计日志（增强字段）
    void record(String action,
                String entityType,
                String entityId,
                String detail,
                String status,
                Integer httpStatus,
                Long durationMs,
                String errorCode,
                String errorMessage);

    // 分页查询审计日志
    PageResponse<AuditLogResponse> page(long page,
                                        long size,
                                        Long tenantId,
                                        String keyword,
                                        String action,
                                        String entityType,
                                        String actorUsername,
                                        String status,
                                        String requestId,
                                        String method,
                                        String path,
                                        String errorCode,
                                        String errorMessage,
                                        Integer httpStatus,
                                        Instant startTime,
                                        Instant endTime);

    java.util.List<AuditLogResponse> export(Long tenantId,
                                            String keyword,
                                            String action,
                                            String entityType,
                                            String actorUsername,
                                            String status,
                                            String requestId,
                                            String method,
                                            String path,
                                            String errorCode,
                                            String errorMessage,
                                            Integer httpStatus,
                                            Instant startTime,
                                            Instant endTime,
                                            long limit);
}
