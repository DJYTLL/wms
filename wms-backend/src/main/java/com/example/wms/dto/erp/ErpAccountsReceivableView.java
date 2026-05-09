package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// ERP应收列表视图
public record ErpAccountsReceivableView(
    Long id,
    String orderNo,
    Long customerId,
    String customerName,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal unpaidAmount,
    String status,
    Instant createdAt
) {
}
