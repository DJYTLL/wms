package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.util.List;

// ERP收款单新增请求
public record ErpReceiptCreateRequest(
    String receiptNo,
    Long customerId,
    Long receivableId,
    List<Long> receivableIds,
    Long saleOrderId,
    BigDecimal amount,
    BigDecimal discountAmount,
    List<ErpReceiptAllocationRequest> allocations,
    String settlementMethod,
    String receiptMethodCode,
    String receivedAt,
    String remark
) {
}
