package com.example.wms.monitor;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.entity.SqlRequestTrace;
import com.example.wms.entity.SqlTraceEntry;
import com.example.wms.mapper.monitor.SqlRequestTraceMapper;
import com.example.wms.mapper.monitor.SqlTraceEntryMapper;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.security.CurrentActor;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class RequestSqlTraceRecorder {
    private final SqlRequestTraceMapper sqlRequestTraceMapper;
    private final SqlTraceEntryMapper sqlTraceEntryMapper;

    public RequestSqlTraceRecorder(SqlRequestTraceMapper sqlRequestTraceMapper,
                                   SqlTraceEntryMapper sqlTraceEntryMapper) {
        this.sqlRequestTraceMapper = sqlRequestTraceMapper;
        this.sqlTraceEntryMapper = sqlTraceEntryMapper;
    }

    @Transactional
    public void record(RequestAuditContext context, int responseStatus, long requestCostMs, Instant finishedAt) {
        if (context == null || context.getSqlTraceContext() == null) {
            return;
        }
        RequestSqlTraceContext traceContext = context.getSqlTraceContext();
        if (traceContext.getEntries().isEmpty()) {
            return;
        }

        SqlRequestTrace requestTrace = new SqlRequestTrace();
        requestTrace.setTenantId(TenantContext.requireTenantId());
        requestTrace.setRequestId(context.getRequestId());
        requestTrace.setRequestPath(context.getPath());
        requestTrace.setRequestMethod(context.getMethod());
        requestTrace.setResponseStatus(responseStatus);
        requestTrace.setRequestCostMs(requestCostMs);
        requestTrace.setSqlTotalCostMs(traceContext.getTotalCostMs());
        requestTrace.setSqlCount(traceContext.getEntryCount());
        requestTrace.setUsername(CurrentActor.username());
        requestTrace.setUserId(resolveCurrentUserId());
        requestTrace.setStartedAt(resolveStartedAt(context, finishedAt, requestCostMs));
        requestTrace.setFinishedAt(finishedAt);
        sqlRequestTraceMapper.insert(requestTrace);

        List<SqlTraceEntry> entries = traceContext.getEntries().stream()
            .map(entry -> toEntity(requestTrace, entry))
            .toList();
        sqlTraceEntryMapper.batchInsert(entries);
    }

    private Instant resolveStartedAt(RequestAuditContext context, Instant finishedAt, long requestCostMs) {
        if (context.getStartNanos() <= 0L) {
            return finishedAt.minusMillis(Math.max(requestCostMs, 0L));
        }
        return finishedAt.minusMillis(Math.max(requestCostMs, 0L));
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user.getUserId();
    }

    private SqlTraceEntry toEntity(SqlRequestTrace requestTrace, RequestSqlTraceEntry entry) {
        SqlTraceEntry traceEntry = new SqlTraceEntry();
        traceEntry.setTenantId(requestTrace.getTenantId());
        traceEntry.setRequestTraceId(requestTrace.getId());
        traceEntry.setRequestId(entry.getRequestId());
        traceEntry.setSequenceNo(entry.getSequenceNo());
        traceEntry.setMapperId(entry.getMapperId());
        traceEntry.setSqlType(entry.getSqlType());
        traceEntry.setCostMs(entry.getCostMs());
        traceEntry.setSqlText(entry.getSqlText());
        traceEntry.setParamsSummary(entry.getParamsSummary());
        traceEntry.setExecutedAt(entry.getExecutedAt());
        return traceEntry;
    }
}
