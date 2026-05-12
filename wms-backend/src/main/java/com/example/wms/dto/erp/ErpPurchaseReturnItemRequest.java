package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// 采购退货明细请求（ERP进销存）
public record ErpPurchaseReturnItemRequest(
    @NotNull Long productId,
    Long warehouseId,
    Long locationId,
    @NotNull @Positive BigDecimal qty,
    BigDecimal price,
    BigDecimal priceInclTax,
    BigDecimal taxRate,
    Integer sortNo,
    String remark
) {
}
