package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 付款单用于接收新增操作的请求参数。

 */
public record ErpPaymentCreateRequest(
    /**
     * 表示付款编号。
     */
    String paymentNo,
    /**
     * 表示供应商 ID。
     */
    Long supplierId,
    /**
     * 表示应付 ID。
     */
    Long payableId,
    /**
     * 表示应付 ID 列表。
     */
    List<Long> payableIds,
    /**
     * 表示purchaseOrder ID。
     */
    Long purchaseOrderId,
    /**
     * 表示金额。
     */
    BigDecimal amount,
    /**
     * 表示优惠金额。
     */
    BigDecimal discountAmount,
    /**
     * 表示allocations。
     */
    List<ErpPaymentAllocationRequest> allocations,
    /**
     * 表示结算方式。
     */
    String settlementMethod,
    /**
     * 表示付款方式编码。
     */
    String paymentMethodCode,
    /**
     * 表示paid时间。
     */
    String paidAt,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
