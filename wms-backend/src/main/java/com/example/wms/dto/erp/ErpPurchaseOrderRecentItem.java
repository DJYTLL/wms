package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// 最近采购单明细（用于退货参考）
public record ErpPurchaseOrderRecentItem(
    Long orderId,
    String orderNo,
    Instant orderAt,
    Long productId,
    BigDecimal qty,
    BigDecimal price
) {
}
