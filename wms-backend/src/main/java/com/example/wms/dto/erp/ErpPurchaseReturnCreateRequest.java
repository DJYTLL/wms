package com.example.wms.dto.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 采购退货单用于接收新增操作的请求参数。

 */
public record ErpPurchaseReturnCreateRequest(
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示单据时间。
     */
    String orderAt,
    /**
     * 表示退货类型。
     */
    String returnType,
    /**
     * 表示供应商 ID。
     */
    Long supplierId,
    /**
     * 表示purchaseOrder ID。
     */
    Long purchaseOrderId,
    /**
     * 表示结算方式。
     */
    String settlementMethod,
    /**
     * 表示付款方式编码。
     */
    String paymentMethodCode,
    /**
     * 表示refund操作动作。
     */
    String refundAction,
    /**
     * 表示已支付金额。
     */
    BigDecimal paidAmount,
    /**
     * 表示优惠金额。
     */
    BigDecimal discountAmount,
    /**
     * 表示明细项列表。
     */
    @Valid @NotEmpty List<ErpPurchaseReturnItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
