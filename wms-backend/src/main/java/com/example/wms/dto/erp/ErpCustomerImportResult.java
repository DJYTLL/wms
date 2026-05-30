package com.example.wms.dto.erp;

public record ErpCustomerImportResult(
    Long batchId,
    String batchNo,
    String status,
    Integer totalCount,
    Integer successCount,
    Integer failedCount
) {
}
