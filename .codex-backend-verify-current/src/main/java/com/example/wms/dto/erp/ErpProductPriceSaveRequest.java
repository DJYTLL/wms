package com.example.wms.dto.erp;

import java.util.List;

/**

 * ERP 商品价格用于接收接口请求参数。

 */
public record ErpProductPriceSaveRequest(
    /**
     * 表示明细项列表。
     */
    List<ErpProductPriceItemRequest> items
) {
}
