package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

// 新增销售退货请求（ERP进销存）
public record ErpSaleReturnCreateRequest(
    String orderNo,
    String orderAt,
    String returnType,
    Long customerId,
    Long saleOrderId,
    String settlementMethod,
    String receiptMethodCode,
    String refundAction,
    BigDecimal paidAmount,
    BigDecimal discountAmount,
    @NotEmpty List<ErpSaleReturnItemRequest> items,
    String remark
) {
}
