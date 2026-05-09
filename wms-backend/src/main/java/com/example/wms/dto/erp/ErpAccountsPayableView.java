package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// ERP应付单列表视图
public record ErpAccountsPayableView(
    Long id,
    String orderNo,
    Long supplierId,
    String supplierName,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal discountAmount,
    BigDecimal unpaidAmount,
    String status,
    Instant createdAt
) {
}
