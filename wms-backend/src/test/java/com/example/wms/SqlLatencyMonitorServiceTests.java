package com.example.wms;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.monitor.SqlRequestTraceRow;
import com.example.wms.dto.monitor.SqlTraceEntryRow;
import com.example.wms.mapper.monitor.SqlRequestTraceMapper;
import com.example.wms.mapper.monitor.SqlTraceEntryMapper;
import com.example.wms.service.monitor.impl.SqlLatencyMonitorServiceImpl;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlLatencyMonitorServiceTests {
    @Mock
    private SqlRequestTraceMapper sqlRequestTraceMapper;

    @Mock
    private SqlTraceEntryMapper sqlTraceEntryMapper;

    @InjectMocks
    private SqlLatencyMonitorServiceImpl sqlLatencyMonitorService;

    @Test
    void pageFiltersAndReturnsRequestRows() {
        TenantContext.setTenantId(1L);
        try {
            Instant startAt = Instant.parse("2026-05-31T00:00:00Z");
            Instant endAt = Instant.parse("2026-05-31T23:59:59Z");
            when(sqlRequestTraceMapper.count(1L, "/erp/sale-orders", "GET", 200, 1000L, 300L, startAt, endAt))
                .thenReturn(1L);
            when(sqlRequestTraceMapper.page(1L, "/erp/sale-orders", "GET", 200, 1000L, 300L, startAt, endAt, 20L, 0L))
                .thenReturn(List.of(new SqlRequestTraceRow(
                    "req-001", "/erp/sale-orders/approved/86", "GET", 200, 6457L, 2310L, 5, "admin", startAt, endAt
                )));

            PageResponse<SqlRequestTraceRow> result = sqlLatencyMonitorService.page(
                1L, 20L, "/erp/sale-orders", "GET", 200, 1000L, 300L, startAt, endAt
            );

            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).sqlCount()).isEqualTo(5);
            assertThat(result.items().get(0).sqlTotalCostMs()).isEqualTo(2310L);
            assertThat(result.items().get(0).requestCostMs()).isEqualTo(6457L);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void listsEntriesByRequestId() {
        TenantContext.setTenantId(1L);
        try {
            when(sqlTraceEntryMapper.findByRequestId(1L, "req-001"))
                .thenReturn(List.of(new SqlTraceEntryRow(1, "mapper.a", "query", 120L, "SELECT 1", "[disabled]", Instant.now())));

            List<SqlTraceEntryRow> result = sqlLatencyMonitorService.listEntries("req-001");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).mapperId()).isEqualTo("mapper.a");
            verify(sqlTraceEntryMapper).findByRequestId(1L, "req-001");
        } finally {
            TenantContext.clear();
        }
    }
}
