package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpStockInitImportItemView(
    Long id,
    Integer rowNo,
    String sourceCode,
    String sourceName,
    Long matchedProductId,
    String warehouseName,
    String locationName,
    BigDecimal countedQty,
    BigDecimal initUnitCost,
    BigDecimal initTotalAmount,
    String status,
    String errorField,
    String errorMessage,
    String suggestion,
    String warningMessage,
    String matchedStrategy,
    Instant createdAt
) {
}
