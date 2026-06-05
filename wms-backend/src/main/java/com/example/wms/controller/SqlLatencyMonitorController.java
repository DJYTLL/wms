package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.monitor.SqlRequestTraceRow;
import com.example.wms.dto.monitor.SqlTraceEntryRow;
import com.example.wms.service.monitor.SqlLatencyMonitorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/system/sql-latency")
public class SqlLatencyMonitorController {
    private final SqlLatencyMonitorService sqlLatencyMonitorService;

    public SqlLatencyMonitorController(SqlLatencyMonitorService sqlLatencyMonitorService) {
        this.sqlLatencyMonitorService = sqlLatencyMonitorService;
    }

    @GetMapping("/requests/page")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_sql-latency-monitor:view')")
    public ResponseEntity<ApiResponse<PageResponse<SqlRequestTraceRow>>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) String requestPath,
        @RequestParam(required = false) String requestMethod,
        @RequestParam(required = false) Integer responseStatus,
        @RequestParam(required = false) Long minRequestCostMs,
        @RequestParam(required = false) Long minSqlCostMs,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endAt
    ) {
        return ResponseEntity.ok(ApiResponse.ok(sqlLatencyMonitorService.page(
            page, size, requestPath, requestMethod, responseStatus, minRequestCostMs, minSqlCostMs, startAt, endAt
        )));
    }

    @GetMapping("/requests/{requestId}/entries")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_sql-latency-monitor:view')")
    public ResponseEntity<ApiResponse<List<SqlTraceEntryRow>>> listEntries(@PathVariable String requestId) {
        return ResponseEntity.ok(ApiResponse.ok(sqlLatencyMonitorService.listEntries(requestId)));
    }
}
