package com.example.wms.dto.erp;

import java.math.BigDecimal;

public record ErpPurchaseReturnSourcePurchaseOrderItem(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long warehouseId,
    Long locationId,
    BigDecimal qty,
    BigDecimal remainingQty,
    BigDecimal price,
    BigDecimal taxRate
) {
}
