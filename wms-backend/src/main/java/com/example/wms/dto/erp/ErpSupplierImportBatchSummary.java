package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpSupplierImportBatchSummary(
    Long id,
    String batchNo,
    String sourceName,
    String importMode,
    Integer totalCount,
    Integer successCount,
    Integer failedCount,
    Integer uncategorizedCount,
    Integer settlementUnmatchedCount,
    Integer pendingSubjectMergeCount,
    String status,
    String summary,
    String createdBy,
    Instant createdAt
) {
}
