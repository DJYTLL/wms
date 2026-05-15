package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**

 * ERP 库存盘点单用于接收明细项的请求参数。

 */
public record ErpStockCountItemRequest(
    /**
     * 表示商品 ID。
     */
    @NotNull Long productId,
    /**
     * 表示仓库 ID。
     */
    Long warehouseId,
    /**
     * 表示库位 ID。
     */
    Long locationId,
    /**
     * 表示counted数量。
     */
    @NotNull @PositiveOrZero BigDecimal countedQty,
    /**
     * 表示init单位成本。
     */
    @PositiveOrZero BigDecimal initUnitCost,
    /**
     * 表示init合计金额。
     */
    @PositiveOrZero BigDecimal initTotalAmount,
    /**
     * 表示system数量。
     */
    BigDecimal systemQty,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
