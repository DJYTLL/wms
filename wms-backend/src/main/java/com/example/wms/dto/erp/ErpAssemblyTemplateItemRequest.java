package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**

 * ERP 组装模板用于接收明细项的请求参数。

 */
public record ErpAssemblyTemplateItemRequest(
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
    @NotNull @Positive BigDecimal qty,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
