package com.example.wms.dto.erp;

public record ErpProductImportResult(
    Long batchId,
    String batchNo,
    String status,
    Integer totalCount,
    Integer successCount,
    Integer failedCount
) {
}
