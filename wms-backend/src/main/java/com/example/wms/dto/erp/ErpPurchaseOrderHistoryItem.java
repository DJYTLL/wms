package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

/**

 * ERP 采购单用于返回历史记录条目数据。

 */
public record ErpPurchaseOrderHistoryItem(
    /**
     * 表示order ID。
     */
    Long orderId,
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示单据时间。
     */
    Instant orderAt,
    /**
     * 表示商品 ID。
     */
    Long productId,
    /**
     * 表示数量。
     */
    BigDecimal qty,
    /**
     * 表示价格。
     */
    BigDecimal price,
    /**
     * 表示价格Incl税务。
     */
    BigDecimal priceInclTax,
    /**
     * 表示供应商 ID。
     */
    Long supplierId,
    /**
     * 表示供应商名称。
     */
    String supplierName
) {
}
