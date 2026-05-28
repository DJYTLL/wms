package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;

import java.util.List;

/**

 * ERP 库存盘点单用于返回详情数据。

 */
public record ErpStockCountDetail(
    /**
     * 表示盘点。
     */
    ErpStockCount count,
    /**
     * 表示明细项列表。
     */
    List<ErpStockCountItem> items
) {
}
