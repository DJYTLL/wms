package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpCustomerImportItemView(
    Long id,
    Integer rowNo,
    String sourceCode,
    String sourceName,
    Long matchedCustomerId,
    String categoryName,
    String settlementMethodName,
    String receiptMethodName,
    String deliveryMethodName,
    String status,
    String errorField,
    String errorMessage,
    String suggestion,
    String warningMessage,
    String matchedStrategy,
    Instant createdAt
) {
}
