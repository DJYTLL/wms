package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 更新采购单请求（ERP进销存）
public record ErpPurchaseOrderUpdateRequest(
    String orderNo,
    Long supplierId,
    String paymentMethodCode,
    java.math.BigDecimal paidAmount,
    java.math.BigDecimal discountAmount,
    @NotEmpty List<ErpPurchaseOrderItemRequest> items,
    String remark
) {
}
