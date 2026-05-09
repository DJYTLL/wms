package com.example.wms.dto.erp;

import java.math.BigDecimal;

// ERP收款单-应收分摊请求
public record ErpReceiptAllocationRequest(
    Long receivableId,
    BigDecimal amount,
    BigDecimal discountAmount
) {
}
