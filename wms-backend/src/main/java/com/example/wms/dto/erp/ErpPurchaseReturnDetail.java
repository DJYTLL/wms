package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpPurchaseReturnItem;

import java.util.List;

/**

 * ERP 采购退货单用于返回详情数据。

 */
public record ErpPurchaseReturnDetail(
    /**
     * 表示主单信息。
     */
    ErpPurchaseReturn order,
    /**
     * 表示明细项列表。
     */
    List<ErpPurchaseReturnItem> items
) {
}
