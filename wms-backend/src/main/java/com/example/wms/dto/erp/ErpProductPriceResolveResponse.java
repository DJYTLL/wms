package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 商品价格解析响应
public record ErpProductPriceResolveResponse(
    BigDecimal salePrice
) {
}
