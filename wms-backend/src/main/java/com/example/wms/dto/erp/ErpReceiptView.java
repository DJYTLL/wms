package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// ERP收款单列表视图
public record ErpReceiptView(
    Long id,
    String receiptNo,
    Long customerId,
    String customerName,
    Long receivableId,
    BigDecimal amount,
    BigDecimal discountAmount,
    String status,
    Instant createdAt,
    String remark
) {
}
