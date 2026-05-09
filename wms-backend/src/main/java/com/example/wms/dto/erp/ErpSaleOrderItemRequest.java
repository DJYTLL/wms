package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// 销售单明细请求（ERP进销存）
public record ErpSaleOrderItemRequest(
    @NotNull Long productId,
    Long warehouseId,
    Long locationId,
    @NotNull BigDecimal qty,
    BigDecimal price,
    BigDecimal priceInclTax,
    BigDecimal taxRate,
    Integer sortNo,
    String remark
) {
}
