package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.AuditLogResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;

// 审计日志接口
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final long exportMax;

    public AuditLogController(AuditLogService auditLogService,
                              @org.springframework.beans.factory.annotation.Value("${wms.audit.export-max:5000}")
                              long exportMax) {
        this.auditLogService = auditLogService;
        this.exportMax = exportMax;
    }

    // 分页查询审计日志
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_audit:view')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) Long tenantId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String actorUsername,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String requestId,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) String path,
        @RequestParam(required = false) String errorCode,
        @RequestParam(required = false) String errorMessage,
        @RequestParam(required = false) Integer httpStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            auditLogService.page(page, size, tenantId, keyword, action, entityType, actorUsername, status, requestId, method, path, errorCode, errorMessage, httpStatus, startTime, endTime)
        ));
    }

    // 导出审计日志（CSV）
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_audit:view')")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) Long tenantId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String entityType,
                       @RequestParam(required = false) String actorUsername,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String requestId,
                       @RequestParam(required = false) String method,
                       @RequestParam(required = false) String path,
                       @RequestParam(required = false) String errorCode,
                       @RequestParam(required = false) String errorMessage,
                       @RequestParam(required = false) Integer httpStatus,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime)
        throws java.io.IOException {
        List<AuditLogResponse> logs = auditLogService.export(
            tenantId, keyword, action, entityType, actorUsername, status, requestId, method, path, errorCode, errorMessage, httpStatus, startTime, endTime, exportMax
        );
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-logs.csv\"");
        StringBuilder builder = new StringBuilder();
        builder.append("id,tenantId,tenantCode,authTenantId,authTenantCode,crossTenant,actorUsername,action,entityType,entityId,detail,status,requestId,clientIp,userAgent,method,path,httpStatus,errorCode,errorMessage,durationMs,createdAt\n");
        for (AuditLogResponse log : logs) {
            builder.append(value(log.id()))
                .append(',')
                .append(value(log.tenantId()))
                .append(',')
                .append(value(log.tenantCode()))
                .append(',')
                .append(value(log.authTenantId()))
                .append(',')
                .append(escapeCsv(log.authTenantCode()))
                .append(',')
                .append(value(log.crossTenant()))
                .append(',')
                .append(escapeCsv(log.actorUsername()))
                .append(',')
                .append(escapeCsv(log.action()))
                .append(',')
                .append(escapeCsv(log.entityType()))
                .append(',')
                .append(escapeCsv(log.entityId()))
                .append(',')
                .append(escapeCsv(log.detail()))
                .append(',')
                .append(escapeCsv(log.status()))
                .append(',')
                .append(escapeCsv(log.requestId()))
                .append(',')
                .append(escapeCsv(log.clientIp()))
                .append(',')
                .append(escapeCsv(log.userAgent()))
                .append(',')
                .append(escapeCsv(log.method()))
                .append(',')
                .append(escapeCsv(log.path()))
                .append(',')
                .append(value(log.httpStatus()))
                .append(',')
                .append(escapeCsv(log.errorCode()))
                .append(',')
                .append(escapeCsv(log.errorMessage()))
                .append(',')
                .append(value(log.durationMs()))
                .append(',')
                .append(value(log.createdAt()))
                .append('\n');
        }
        response.getWriter().write(builder.toString());
        response.flushBuffer();
    }

    private static String value(Object value) {
        return value == null ? "" : escapeCsv(String.valueOf(value));
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        if (needsQuote) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
