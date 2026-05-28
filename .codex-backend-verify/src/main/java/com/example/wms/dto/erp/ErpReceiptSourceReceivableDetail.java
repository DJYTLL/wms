package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpReceiptSourceReceivableDetail(
    Long id,
    String orderNo,
    Long customerId,
    String customerName,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal unpaidAmount,
    String status,
    String settlementMethod,
    String sourceType,
    Long sourceId,
    String remark,
    Instant createdAt
) {
}
