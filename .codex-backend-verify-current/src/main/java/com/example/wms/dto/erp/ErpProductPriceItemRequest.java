package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 商品价格用于接收明细项的请求参数。

 */
public record ErpProductPriceItemRequest(
    /**
     * 表示客户分类 ID。
     */
    Long customerCategoryId,
    /**
     * 表示销售价格。
     */
    BigDecimal salePrice
) {
}
