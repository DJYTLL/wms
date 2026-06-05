package com.example.wms.service.monitor;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.monitor.SqlRequestTraceRow;
import com.example.wms.dto.monitor.SqlTraceEntryRow;

import java.time.Instant;
import java.util.List;

public interface SqlLatencyMonitorService {
    PageResponse<SqlRequestTraceRow> page(long page,
                                          long size,
                                          String requestPath,
                                          String requestMethod,
                                          Integer responseStatus,
                                          Long minRequestCostMs,
                                          Long minSqlCostMs,
                                          Instant startAt,
                                          Instant endAt);

    List<SqlTraceEntryRow> listEntries(String requestId);
}
