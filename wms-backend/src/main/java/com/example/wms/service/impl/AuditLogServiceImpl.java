package com.example.wms.service.impl;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.AuditLogResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.AuditLog;
import com.example.wms.mapper.AuditLogMapper;
import com.example.wms.service.AuditLogService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 审计日志服务实现
@Service
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void record(String action,
                       String entityType,
                       String entityId,
                       String detail,
                       String status,
                       Integer httpStatus,
                       Long durationMs,
                       String errorCode,
                       String errorMessage) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.requireTenantId());
        log.setActorUsername(resolveActor());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetail(detail);
        log.setStatus(status == null ? "SUCCESS" : status);
        log.setHttpStatus(httpStatus);
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(durationMs);
        RequestAuditContext context = RequestAuditContext.get();
        if (context != null) {
            log.setRequestId(context.getRequestId());
            log.setClientIp(context.getClientIp());
            log.setUserAgent(context.getUserAgent());
            log.setMethod(context.getMethod());
            log.setPath(context.getPath());
        }
        auditLogMapper.insert(log);
    }

    @Override
    public PageResponse<AuditLogResponse> page(long page,
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
                                               Instant endTime) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);
        long offset = (safePage - 1) * safeSize;
        long total = auditLogMapper.count(
            resolvedTenantId, keyword, action, entityType, actorUsername, status, requestId, method, path, errorCode, errorMessage, httpStatus, startTime, endTime
        );
        List<AuditLog> items = auditLogMapper.page(
            resolvedTenantId, keyword, action, entityType, actorUsername, status, requestId, method, path, errorCode, errorMessage, httpStatus, startTime, endTime, safeSize, offset
        );
        List<AuditLogResponse> responses = items.stream().map(this::toResponse).toList();
        return new PageResponse<>(total, safePage, safeSize, responses);
    }

    @Override
    public List<AuditLogResponse> export(Long tenantId,
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
                                         long limit) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        long safeLimit = Math.max(limit, 1);
        List<AuditLog> items = auditLogMapper.export(
            resolvedTenantId, keyword, action, entityType, actorUsername, status, requestId, method, path, errorCode, errorMessage, httpStatus, startTime, endTime, safeLimit
        );
        return items.stream().map(this::toResponse).toList();
    }

    // 从安全上下文获取操作者
    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getTenantId(),
            log.getTenantCode(),
            log.getActorUsername(),
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getDetail(),
            log.getStatus(),
            log.getRequestId(),
            log.getClientIp(),
            log.getUserAgent(),
            log.getMethod(),
            log.getPath(),
            log.getHttpStatus(),
            log.getErrorCode(),
            log.getErrorMessage(),
            log.getDurationMs(),
            log.getCreatedAt()
        );
    }

    private Long resolveTenantId(Long tenantId) {
        if (isSuperAdmin()) {
            return tenantId;
        }
        return TenantContext.requireTenantId();
    }

    private boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_super_admin".equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
