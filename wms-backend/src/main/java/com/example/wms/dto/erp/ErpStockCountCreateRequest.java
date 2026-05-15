package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**

 * ERP 库存盘点单用于接收新增操作的请求参数。

 */
public record ErpStockCountCreateRequest(
    /**
     * 表示盘点编号。
     */
    String countNo,
    /**
     * 表示盘点类型。
     */
    String countType,
    /**
     * 表示adjustment原因。
     */
    String adjustmentReason,
    /**
     * 表示仓库 ID。
     */
    Long warehouseId,
    /**
     * 表示库位 ID。
     */
    Long locationId,
    /**
     * 表示盘点时间。
     */
    String countAt,
    /**
     * 表示明细项列表。
     */
    @NotEmpty List<ErpStockCountItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
