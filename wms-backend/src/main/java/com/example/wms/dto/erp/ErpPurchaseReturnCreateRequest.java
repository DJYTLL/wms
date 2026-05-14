package com.example.wms.dto.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

// 新增采购退货请求（ERP进销存）
public record ErpPurchaseReturnCreateRequest(
    String orderNo,
    String orderAt,
    String returnType,
    Long supplierId,
    Long purchaseOrderId,
    String settlementMethod,
    String paymentMethodCode,
    String refundAction,
    BigDecimal paidAmount,
    BigDecimal discountAmount,
    @Valid @NotEmpty List<ErpPurchaseReturnItemRequest> items,
    String remark
) {
}
