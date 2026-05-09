package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// 库存盘点单明细请求（ERP进销存）
public record ErpStockCountItemRequest(
    @NotNull Long productId,
    Long warehouseId,
    Long locationId,
    @NotNull BigDecimal countedQty,
    BigDecimal systemQty,
    String remark
) {
}
