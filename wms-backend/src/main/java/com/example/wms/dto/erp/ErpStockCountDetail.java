package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;

import java.util.List;

// 库存盘点单详情响应（ERP进销存）
public record ErpStockCountDetail(
    ErpStockCount count,
    List<ErpStockCountItem> items
) {
}
