package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 销售单明细成本快照（按审核出库流水汇总）
public record ErpSaleOrderItemCostSnapshot(
    Long bizItemId,
    BigDecimal unitCost
) {
}
