package com.example.wms.dto.erp;

import java.math.BigDecimal;

// ERP收款单-应收分摊展示
public record ErpReceiptReceivableView(
    Long receivableId,
    String orderNo,
    BigDecimal allocatedAmount,
    BigDecimal allocatedDiscount,
    BigDecimal allocatedTotal
) {
}
