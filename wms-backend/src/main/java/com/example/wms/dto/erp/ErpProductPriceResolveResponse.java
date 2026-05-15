package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 商品价格用于返回接口响应数据。

 */
public record ErpProductPriceResolveResponse(
    /**
     * 表示销售价格。
     */
    BigDecimal salePrice
) {
}
