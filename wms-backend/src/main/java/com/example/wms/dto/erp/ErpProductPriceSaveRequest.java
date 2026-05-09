package com.example.wms.dto.erp;

import java.util.List;

// 保存商品价格（按客户类别）
public record ErpProductPriceSaveRequest(
    List<ErpProductPriceItemRequest> items
) {
}
