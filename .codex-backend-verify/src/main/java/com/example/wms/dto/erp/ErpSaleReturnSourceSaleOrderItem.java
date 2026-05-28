package com.example.wms.dto.erp;

import java.math.BigDecimal;

public record ErpSaleReturnSourceSaleOrderItem(
    Long id,
    Integer sortNo,
    Long productId,
    String productCode,
    String productName,
    Long warehouseId,
    Long locationId,
    BigDecimal qty,
    BigDecimal remainingQty,
    BigDecimal approvedReturnedQty,
    BigDecimal draftOccupiedQty,
    BigDecimal price,
    BigDecimal priceInclTax,
    BigDecimal taxRate
) {
}
