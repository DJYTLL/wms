package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * 用于传输ERP 销售单相关数据。

 */
public record ErpSaleOrderItemCostSnapshot(
    /**
     * 表示biz明细项 ID。
     */
    Long bizItemId,
    /**
     * 表示单位成本。
     */
    BigDecimal unitCost
) {
}
