package com.example.wms.dto.monitor;

import java.time.Instant;

public record SqlTraceEntryRow(
    Integer sequenceNo,
    String mapperId,
    String sqlType,
    Long costMs,
    String sqlText,
    String paramsSummary,
    Instant executedAt
) {
}
