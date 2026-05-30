package com.example.wms.dto.erp;

import java.util.List;

public record ErpSupplierImportResult(
    Long batchId,
    String batchNo,
    String status,
    Integer totalCount,
    Integer successCount,
    Integer failedCount,
    List<ErpSupplierImportResultItem> items
) {
}
