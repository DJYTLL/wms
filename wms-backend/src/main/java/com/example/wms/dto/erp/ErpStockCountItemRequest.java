package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// 库存盘点单明细请求（ERP进销存）
public record ErpStockCountItemRequest(
    @NotNull Long productId,
    Long warehouseId,
    Long locationId,
    @NotNull @PositiveOrZero BigDecimal countedQty,
    @PositiveOrZero BigDecimal initUnitCost,
    @PositiveOrZero BigDecimal initTotalAmount,
    BigDecimal systemQty,
    String remark
) {
}
