package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**

 * ERP 销售退货单用于接收明细项的请求参数。

 */
public record ErpSaleReturnItemRequest(
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
     * 表示数量。
     */
    @NotNull BigDecimal qty,
    /**
     * 表示价格。
     */
    BigDecimal price,
    /**
     * 表示价格Incl税务。
     */
    BigDecimal priceInclTax,
    /**
     * 表示税务Rate。
     */
    BigDecimal taxRate,
    /**
     * 表示排序编号。
     */
    Integer sortNo,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
