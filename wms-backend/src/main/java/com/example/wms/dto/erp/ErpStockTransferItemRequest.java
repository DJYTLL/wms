package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ErpStockTransferItemRequest(
    @NotNull Long productId,
    @NotNull Long fromWarehouseId,
    Long fromLocationId,
    @NotNull Long toWarehouseId,
    Long toLocationId,
    @NotNull @Positive BigDecimal qty,
    String remark
) {
}
