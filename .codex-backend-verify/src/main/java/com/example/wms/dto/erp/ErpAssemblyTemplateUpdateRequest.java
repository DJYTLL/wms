package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 组装模板用于接收更新操作的请求参数。

 */
public record ErpAssemblyTemplateUpdateRequest(
    /**
     * 表示名称。
     */
    @NotBlank String name,
    /**
     * 表示order类型。
     */
    @NotBlank String orderType,
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
    @NotEmpty List<ErpAssemblyTemplateItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
