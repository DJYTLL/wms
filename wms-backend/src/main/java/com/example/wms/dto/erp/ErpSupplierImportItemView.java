package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpSupplierImportItemView(
    Long id,
    Integer rowNo,
    String sourceCode,
    String sourceName,
    Long matchedSupplierId,
    String supplierTypeName,
    String settlementMethodName,
    String enterpriseMatch,
    String priceLevel,
    String status,
    String errorField,
    String errorMessage,
    String suggestion,
    String warningMessage,
    String matchedStrategy,
    Instant createdAt
) {
}
