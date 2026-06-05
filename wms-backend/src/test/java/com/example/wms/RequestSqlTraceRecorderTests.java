package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.entity.SqlRequestTrace;
import com.example.wms.entity.SqlTraceEntry;
import com.example.wms.mapper.monitor.SqlRequestTraceMapper;
import com.example.wms.mapper.monitor.SqlTraceEntryMapper;
import com.example.wms.monitor.RequestSqlTraceContext;
import com.example.wms.monitor.RequestSqlTraceEntry;
import com.example.wms.monitor.RequestSqlTraceRecorder;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestSqlTraceRecorderTests {
    @Mock
    private SqlRequestTraceMapper sqlRequestTraceMapper;

    @Mock
    private SqlTraceEntryMapper sqlTraceEntryMapper;

    @InjectMocks
    private RequestSqlTraceRecorder requestSqlTraceRecorder;

    @Test
    void persistsRequestTraceAndEntriesAtRequestCompletion() {
        TenantContext.setTenantId(1L);
        try {
            RequestAuditContext context = new RequestAuditContext();
            context.setRequestId("req-001");
            context.setPath("/erp/sale-orders/approved/86");
            context.setMethod("GET");
            RequestSqlTraceContext traceContext = new RequestSqlTraceContext();
            traceContext.append(new RequestSqlTraceEntry(1, "req-001", "mapper.a", "query", 120L, "SELECT 1", "[disabled]", Instant.parse("2026-05-31T11:05:03Z")));
            traceContext.append(new RequestSqlTraceEntry(2, "req-001", "mapper.b", "query", 80L, "SELECT 2", "[disabled]", Instant.parse("2026-05-31T11:05:04Z")));
            context.setSqlTraceContext(traceContext);

            doAnswer(invocation -> {
                SqlRequestTrace trace = invocation.getArgument(0);
                trace.setId(99L);
                return 1;
            }).when(sqlRequestTraceMapper).insert(org.mockito.ArgumentMatchers.any(SqlRequestTrace.class));

            requestSqlTraceRecorder.record(context, 200, 6457L, Instant.parse("2026-05-31T11:05:10Z"));

            ArgumentCaptor<SqlRequestTrace> traceCaptor = ArgumentCaptor.forClass(SqlRequestTrace.class);
            verify(sqlRequestTraceMapper).insert(traceCaptor.capture());
            SqlRequestTrace requestTrace = traceCaptor.getValue();
            assertThat(requestTrace.getTenantId()).isEqualTo(1L);
            assertThat(requestTrace.getRequestId()).isEqualTo("req-001");
            assertThat(requestTrace.getRequestPath()).isEqualTo("/erp/sale-orders/approved/86");
            assertThat(requestTrace.getRequestMethod()).isEqualTo("GET");
            assertThat(requestTrace.getResponseStatus()).isEqualTo(200);
            assertThat(requestTrace.getRequestCostMs()).isEqualTo(6457L);
            assertThat(requestTrace.getSqlTotalCostMs()).isEqualTo(200L);
            assertThat(requestTrace.getSqlCount()).isEqualTo(2);

            ArgumentCaptor<List<SqlTraceEntry>> entryCaptor = ArgumentCaptor.forClass(List.class);
            verify(sqlTraceEntryMapper).batchInsert(entryCaptor.capture());
            List<SqlTraceEntry> entries = entryCaptor.getValue();
            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getRequestTraceId()).isEqualTo(99L);
            assertThat(entries.get(0).getSequenceNo()).isEqualTo(1);
            assertThat(entries.get(1).getSequenceNo()).isEqualTo(2);
        } finally {
            TenantContext.clear();
        }
    }
}
