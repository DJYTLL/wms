package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// 采购单商品历史记录（用于商品历史查询）
public record ErpPurchaseOrderHistoryItem(
    Long orderId,
    String orderNo,
    Instant orderAt,
    Long productId,
    BigDecimal qty,
    BigDecimal price,
    BigDecimal priceInclTax,
    Long supplierId,
    String supplierName
) {
}
