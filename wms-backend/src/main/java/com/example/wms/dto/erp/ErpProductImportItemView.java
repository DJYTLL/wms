package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpProductImportItemView(
    Long id,
    Integer rowNo,
    String sourceCode,
    String sourceName,
    Long matchedProductId,
    String categoryName,
    String unitName,
    String warehouseName,
    String supplierName,
    String status,
    String errorField,
    String errorMessage,
    String suggestion,
    String warningMessage,
    String matchedStrategy,
    Instant createdAt
) {
}
