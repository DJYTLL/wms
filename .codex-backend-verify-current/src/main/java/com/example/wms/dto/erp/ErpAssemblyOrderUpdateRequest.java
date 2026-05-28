package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 组装单用于接收更新操作的请求参数。

 */
public record ErpAssemblyOrderUpdateRequest(
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示order类型。
     */
    String orderType,
    /**
     * 表示单据时间。
     */
    String orderAt,
    /**
     * 表示来源类型。
     */
    String sourceType,
    /**
     * 表示来源销售单 ID。
     */
    Long sourceSaleOrderId,
    /**
     * 表示来源销售单明细 ID。
     */
    Long sourceSaleOrderItemId,
    /**
     * 表示客户 ID。
     */
    Long customerId,
    /**
     * 表示finished商品 ID。
     */
    @NotNull Long finishedProductId,
    /**
     * 表示finished数量。
     */
    @NotNull @Positive BigDecimal finishedQty,
    /**
     * 表示仓库 ID。
     */
    Long warehouseId,
    /**
     * 表示库位 ID。
     */
    Long locationId,
    /**
     * 表示labor成本。
     */
    BigDecimal laborCost,
    /**
     * 表示明细项列表。
     */
    @NotEmpty List<ErpAssemblyOrderItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
