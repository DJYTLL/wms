package com.example.wms.dto.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 更新采购单请求（ERP进销存）
public record ErpPurchaseOrderUpdateRequest(
    String orderNo,
    String orderAt,
    Long supplierId,
    String settlementMethod,
    String paymentMethodCode,
    java.math.BigDecimal paidAmount,
    java.math.BigDecimal discountAmount,
    @Valid @NotEmpty List<ErpPurchaseOrderItemRequest> items,
    String remark
) {
}
