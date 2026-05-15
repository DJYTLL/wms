package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**

 * ERP 库存调拨单用于接收新增操作的请求参数。

 */
public record ErpStockTransferCreateRequest(
    /**
     * 表示调拨编号。
     */
    String transferNo,
    /**
     * 表示调拨时间。
     */
    String transferAt,
    /**
     * 表示明细项列表。
     */
    @NotEmpty List<ErpStockTransferItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
