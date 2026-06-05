package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpStockInitImportBatchSummary(
    Long id,
    String batchNo,
    String sourceName,
    String importMode,
    String strategyMode,
    Integer totalCount,
    Integer successCount,
    Integer failedCount,
    Integer warningCount,
    String status,
    String summary,
    Long countId,
    String countNo,
    String createdBy,
    Instant createdAt
) {
}
