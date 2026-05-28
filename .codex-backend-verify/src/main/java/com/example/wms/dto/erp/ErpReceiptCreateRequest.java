package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.util.List;

/**

 * ERP 收款单用于接收新增操作的请求参数。

 */
public record ErpReceiptCreateRequest(
    /**
     * 表示收款编号。
     */
    String receiptNo,
    /**
     * 表示客户 ID。
     */
    Long customerId,
    /**
     * 表示应收 ID。
     */
    Long receivableId,
    /**
     * 表示应收 ID 列表。
     */
    List<Long> receivableIds,
    /**
     * 表示销售Order ID。
     */
    Long saleOrderId,
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
    List<ErpReceiptAllocationRequest> allocations,
    /**
     * 表示结算方式。
     */
    String settlementMethod,
    /**
     * 表示收款方式编码。
     */
    String receiptMethodCode,
    /**
     * 表示received时间。
     */
    String receivedAt,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
