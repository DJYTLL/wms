package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// ERP付款单列表视图
public record ErpPaymentView(
    Long id,
    String paymentNo,
    Long supplierId,
    String supplierName,
    Long payableId,
    BigDecimal amount,
    BigDecimal discountAmount,
    String status,
    Instant createdAt,
    String remark
) {
}
