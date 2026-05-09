package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

// 销售单商品历史记录（用于商品历史查询）
public record ErpSaleOrderHistoryItem(
    Long orderId,
    String orderNo,
    Instant orderAt,
    Long productId,
    BigDecimal qty,
    BigDecimal price,
    BigDecimal priceInclTax,
    Long customerId,
    String customerName
) {
}
