package com.example.wms.dto.erp;

import java.math.BigDecimal;

// ERP付款单分摊请求
public record ErpPaymentAllocationRequest(
    Long payableId,
    BigDecimal amount,
    BigDecimal discountAmount
) {
}
