package com.example.wms.dto.erp;

import java.math.BigDecimal;

// ERP付款单-应付分摊视图
public record ErpPaymentPayableView(
    Long payableId,
    String orderNo,
    BigDecimal allocatedAmount,
    BigDecimal allocatedDiscount,
    BigDecimal allocatedTotal
) {
}
