package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.util.List;

// ERP付款单新增请求
public record ErpPaymentCreateRequest(
    String paymentNo,
    Long supplierId,
    Long payableId,
    List<Long> payableIds,
    Long purchaseOrderId,
    BigDecimal amount,
    BigDecimal discountAmount,
    List<ErpPaymentAllocationRequest> allocations,
    String settlementMethod,
    String paymentMethodCode,
    String paidAt,
    String remark
) {
}
