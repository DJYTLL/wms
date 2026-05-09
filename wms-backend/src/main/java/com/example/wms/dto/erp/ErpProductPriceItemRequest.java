package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 商品价格项（按客户类别）
public record ErpProductPriceItemRequest(
    Long customerCategoryId,
    BigDecimal salePrice
) {
}
