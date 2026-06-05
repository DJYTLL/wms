package com.example.wms.service.monitor.impl;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.monitor.SqlRequestTraceRow;
import com.example.wms.dto.monitor.SqlTraceEntryRow;
import com.example.wms.mapper.monitor.SqlRequestTraceMapper;
import com.example.wms.mapper.monitor.SqlTraceEntryMapper;
import com.example.wms.service.monitor.SqlLatencyMonitorService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SqlLatencyMonitorServiceImpl implements SqlLatencyMonitorService {
    private final SqlRequestTraceMapper sqlRequestTraceMapper;
    private final SqlTraceEntryMapper sqlTraceEntryMapper;

    public SqlLatencyMonitorServiceImpl(SqlRequestTraceMapper sqlRequestTraceMapper,
                                        SqlTraceEntryMapper sqlTraceEntryMapper) {
        this.sqlRequestTraceMapper = sqlRequestTraceMapper;
        this.sqlTraceEntryMapper = sqlTraceEntryMapper;
    }

    @Override
    public PageResponse<SqlRequestTraceRow> page(long page,
                                                 long size,
                                                 String requestPath,
                                                 String requestMethod,
                                                 Integer responseStatus,
                                                 Long minRequestCostMs,
                                                 Long minSqlCostMs,
                                                 Instant startAt,
                                                 Instant endAt) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);
        long offset = (safePage - 1) * safeSize;
        Long tenantId = resolveTenantId();
        long total = sqlRequestTraceMapper.count(
            tenantId, requestPath, requestMethod, responseStatus, minRequestCostMs, minSqlCostMs, startAt, endAt
        );
        List<SqlRequestTraceRow> items = sqlRequestTraceMapper.page(
            tenantId, requestPath, requestMethod, responseStatus, minRequestCostMs, minSqlCostMs, startAt, endAt, safeSize, offset
        );
        return new PageResponse<>(total, safePage, safeSize, items);
    }

    @Override
    public List<SqlTraceEntryRow> listEntries(String requestId) {
        return sqlTraceEntryMapper.findByRequestId(resolveTenantId(), requestId);
    }

    private Long resolveTenantId() {
        if (isSuperAdmin()) {
            Long tenantId = TenantContext.getTenantId();
            return tenantId == null ? 0L : tenantId;
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
