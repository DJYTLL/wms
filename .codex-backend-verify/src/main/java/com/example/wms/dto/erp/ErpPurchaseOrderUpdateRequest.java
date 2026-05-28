package com.example.wms.dto.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**

 * ERP 采购单用于接收更新操作的请求参数。

 */
public record ErpPurchaseOrderUpdateRequest(
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示单据时间。
     */
    String orderAt,
    /**
     * 表示供应商 ID。
     */
    Long supplierId,
    /**
     * 表示结算方式。
     */
    String settlementMethod,
    /**
     * 表示付款方式编码。
     */
    String paymentMethodCode,
    /**
     * 表示已支付金额。
     */
    java.math.BigDecimal paidAmount,
    /**
     * 表示优惠金额。
     */
    java.math.BigDecimal discountAmount,
    /**
     * 表示明细项列表。
     */
    @Valid @NotEmpty List<ErpPurchaseOrderItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
