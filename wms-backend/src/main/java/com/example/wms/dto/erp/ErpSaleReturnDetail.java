package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSaleReturnItem;

import java.util.List;

// 销售退货详情（ERP进销存）
public record ErpSaleReturnDetail(
    ErpSaleReturn order,
    List<ErpSaleReturnItem> items
) {
}
