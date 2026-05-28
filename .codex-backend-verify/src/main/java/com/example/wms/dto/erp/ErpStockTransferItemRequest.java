package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**

 * ERP 库存调拨单用于接收明细项的请求参数。

 */
public record ErpStockTransferItemRequest(
    /**
     * 表示商品 ID。
     */
    @NotNull Long productId,
    /**
     * 表示起始仓库 ID。
     */
    @NotNull Long fromWarehouseId,
    /**
     * 表示起始库位 ID。
     */
    Long fromLocationId,
    /**
     * 表示结束仓库 ID。
     */
    @NotNull Long toWarehouseId,
    /**
     * 表示结束库位 ID。
     */
    Long toLocationId,
    /**
     * 表示数量。
     */
    @NotNull @Positive BigDecimal qty,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
