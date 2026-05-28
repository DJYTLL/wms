package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

/**

 * ERP 采购单用于返回最近记录条目数据。

 */
public record ErpPurchaseOrderRecentItem(
    /**
     * 表示order ID。
     */
    Long orderId,
    /**
     * 表示采购单明细 ID。
     */
    Long orderItemId,
    /**
     * 表示采购单明细行号。
     */
    Integer orderItemSortNo,
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
     * 表示仓库 ID。
     */
    Long warehouseId,
    /**
     * 表示库位 ID。
     */
    Long locationId,
    /**
     * 表示数量。
     */
    BigDecimal qty,
    /**
     * 表示remaining数量。
     */
    BigDecimal remainingQty,
    /**
     * 表示已审核退货数量。
     */
    BigDecimal approvedReturnedQty,
    /**
     * 表示其他草稿占用数量。
     */
    BigDecimal draftOccupiedQty,
    /**
     * 表示价格。
     */
    BigDecimal price,
    /**
     * 表示含税价格。
     */
    BigDecimal priceInclTax,
    /**
     * 表示税率。
     */
    BigDecimal taxRate
) {
}
