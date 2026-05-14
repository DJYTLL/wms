package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// 销售单最近商品记录（用于退货参考）
public record ErpSaleOrderRecentItem(
    Long orderId,
    String orderNo,
    Instant orderAt,
    Long productId,
    BigDecimal qty,
    BigDecimal remainingQty,
    BigDecimal price
) {
}
