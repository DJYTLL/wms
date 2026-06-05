package com.example.wms.dto.erp;

import java.math.BigDecimal;

public record ErpProductStockPolicyRequest(
    Long warehouseId,
    BigDecimal safetyStock,
    BigDecimal minStock,
    BigDecimal maxStock
) {
}
