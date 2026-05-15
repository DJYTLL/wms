package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpStockTransfer;
import com.example.wms.entity.erp.ErpStockTransferItem;

import java.util.List;

/**

 * ERP 库存调拨单用于返回详情数据。

 */
public record ErpStockTransferDetail(
    /**
     * 表示调拨。
     */
    ErpStockTransfer transfer,
    /**
     * 表示明细项列表。
     */
    List<ErpStockTransferItem> items
) {
}
