package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpCustomerImportBatchSummary(
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
