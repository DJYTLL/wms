package com.example.wms.dto.erp;

import java.math.BigDecimal;

public record ErpSaleOrderFlowSnapshot(
    Long saleOrderId,
    String receivableStatus,
    BigDecimal receivableUnpaidAmount,
    Long approvedReturnCount,
    BigDecimal cumulativeReturnAmount,
    BigDecimal saleCost,
    BigDecimal cumulativeReturnCost
) {
}
