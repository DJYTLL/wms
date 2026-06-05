package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpStockCountItemView(
    Long id,
    Long tenantId,
    Long countId,
    Integer lineNo,
    Long productId,
    String productCode,
    String productName,
    Long warehouseId,
    String warehouseName,
    Long locationId,
    String locationName,
    BigDecimal systemQty,
    BigDecimal countedQty,
    BigDecimal initUnitCost,
    BigDecimal initTotalAmount,
    BigDecimal diffQty,
    String remark,
    Instant createdAt,
    Instant updatedAt
) {
}
