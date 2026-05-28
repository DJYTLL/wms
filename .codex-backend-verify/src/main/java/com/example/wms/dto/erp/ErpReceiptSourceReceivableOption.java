package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpReceiptSourceReceivableOption(
    Long id,
    String orderNo,
    Long customerId,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal unpaidAmount,
    String status,
    Instant createdAt,
    String sourceType,
    Long sourceId
) {
}
