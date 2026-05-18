package com.example.wms.dto.erp;

import java.time.Instant;
import java.util.List;

public record ErpPurchaseReturnSourcePurchaseOrderDetail(
    Long id,
    String orderNo,
    Long supplierId,
    Instant orderAt,
    List<ErpPurchaseReturnSourcePurchaseOrderItem> items,
    ErpPurchaseReturnRefundSummary refundSummary
) {
}
