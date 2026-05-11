package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Assembly order item request
public record ErpAssemblyOrderItemRequest(
    @NotNull Long productId,
    Long warehouseId,
    Long locationId,
    @NotNull @Positive BigDecimal qty,
    String remark
) {
}
