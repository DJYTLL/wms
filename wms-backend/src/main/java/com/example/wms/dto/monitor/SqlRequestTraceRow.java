package com.example.wms.dto.monitor;

import java.time.Instant;

public record SqlRequestTraceRow(
    String requestId,
    String requestPath,
    String requestMethod,
    Integer responseStatus,
    Long requestCostMs,
    Long sqlTotalCostMs,
    Integer sqlCount,
    String username,
    Instant startedAt,
    Instant finishedAt
) {
}
