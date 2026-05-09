package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleOrderItem;

import java.util.List;

// 销售单详情响应（ERP进销存）
public record ErpSaleOrderDetail(
    ErpSaleOrder order,
    List<ErpSaleOrderItem> items
) {
}
