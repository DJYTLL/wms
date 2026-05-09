package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;

import java.util.List;

// 新增销售单请求（ERP进销存）
public record ErpSaleOrderCreateRequest(
    String orderNo,
    String orderAt,
    Long customerId,
    String settlementMethod,
    String deliveryMethod,
    BigDecimal paidAmount,
    BigDecimal discountAmount,
    @NotEmpty List<ErpSaleOrderItemRequest> items,
    String remark
) {
}
