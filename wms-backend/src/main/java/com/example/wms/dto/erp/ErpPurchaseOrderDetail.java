package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;

import java.util.List;

/**

 * ERP 采购单用于返回详情数据。

 */
public record ErpPurchaseOrderDetail(
    /**
     * 表示主单信息。
     */
    ErpPurchaseOrder order,
    /**
     * 表示明细项列表。
     */
    List<ErpPurchaseOrderItem> items
) {
}
