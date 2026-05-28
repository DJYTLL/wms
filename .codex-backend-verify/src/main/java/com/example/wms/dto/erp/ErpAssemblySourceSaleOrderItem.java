package com.example.wms.dto.erp;

import java.math.BigDecimal;

public record ErpAssemblySourceSaleOrderItem(
    Long id,
    Integer sortNo,
    Long productId,
    String productCode,
    String productName,
    Long warehouseId,
    Long locationId,
    BigDecimal qty,
    BigDecimal linkedAssemblyQty,
    BigDecimal approvedAssemblyQty
) {
}
