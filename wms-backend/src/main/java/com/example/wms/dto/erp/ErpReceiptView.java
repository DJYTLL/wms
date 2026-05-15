package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

/**

 * ERP 收款单用于返回视图展示数据。

 */
public record ErpReceiptView(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示收款编号。
     */
    String receiptNo,
    /**
     * 表示客户 ID。
     */
    Long customerId,
    /**
     * 表示客户名称。
     */
    String customerName,
    /**
     * 表示应收 ID。
     */
    Long receivableId,
    /**
     * 表示金额。
     */
    BigDecimal amount,
    /**
     * 表示优惠金额。
     */
    BigDecimal discountAmount,
    /**
     * 表示状态。
     */
    String status,
    /**
     * 表示创建时间。
     */
    Instant createdAt,
    /**
     * 表示备注说明。
     */
    String remark
) {
}
