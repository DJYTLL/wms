package com.example.wms.dto.erp;

import java.time.Instant;
import java.util.List;

public record ErpSaleReturnSourceSaleOrderDetail(
    Long id,
    String orderNo,
    Long customerId,
    Instant orderAt,
    List<ErpSaleReturnSourceSaleOrderItem> items,
    ErpSaleReturnRefundSummary refundSummary,
    List<ErpSaleReturnRelatedOrder> relatedReturns
) {
}
