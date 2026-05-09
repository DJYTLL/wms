package com.example.wms.dto.erp;

import java.math.BigDecimal;

// Assembly order item request
public record ErpAssemblyOrderItemRequest(
    Long productId,
    Long warehouseId,
    Long locationId,
    BigDecimal qty,
    String remark
) {
}
