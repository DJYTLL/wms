package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 销售退货单用于接收新增操作的请求参数。

 */
public record ErpSaleReturnCreateRequest(
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
     * 表示客户 ID。
     */
    Long customerId,
    /**
     * 表示销售Order ID。
     */
    Long saleOrderId,
    /**
     * 表示结算方式。
     */
    String settlementMethod,
    /**
     * 表示收款方式编码。
     */
    String receiptMethodCode,
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
    @NotEmpty List<ErpSaleReturnItemRequest> items,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
