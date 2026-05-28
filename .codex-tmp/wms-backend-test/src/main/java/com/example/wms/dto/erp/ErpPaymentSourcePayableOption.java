package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpPaymentSourcePayableOption(
    Long id,
    String orderNo,
    Long supplierId,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal discountAmount,
    BigDecimal unpaidAmount,
    String status,
    Instant createdAt,
    Long purchaseOrderId,
    Long purchaseReturnId
) {
}
