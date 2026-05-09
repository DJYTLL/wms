package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;

import java.util.List;

// 采购单详情响应（ERP进销存）
public record ErpPurchaseOrderDetail(
    ErpPurchaseOrder order,
    List<ErpPurchaseOrderItem> items
) {
}
