package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpPaymentSourcePayableDetail(
    Long id,
    String orderNo,
    Long supplierId,
    String supplierName,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal discountAmount,
    BigDecimal unpaidAmount,
    String status,
    String settlementMethod,
    Long purchaseOrderId,
    Long purchaseReturnId,
    String remark,
    Instant createdAt
) {
}
