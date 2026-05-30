package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpProductImportBatchSummary(
    Long id,
    String batchNo,
    String sourceName,
    String importMode,
    Integer totalCount,
    Integer successCount,
    Integer failedCount,
    String status,
    String summary,
    String createdBy,
    Instant createdAt
) {
}
