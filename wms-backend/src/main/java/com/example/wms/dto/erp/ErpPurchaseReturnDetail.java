package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpPurchaseReturnItem;

import java.util.List;

// 采购退货详情（ERP进销存）
public record ErpPurchaseReturnDetail(
    ErpPurchaseReturn order,
    List<ErpPurchaseReturnItem> items
) {
}
