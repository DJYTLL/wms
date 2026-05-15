package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

/**

 * ERP 付款单用于返回视图展示数据。

 */
public record ErpPaymentView(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示付款编号。
     */
    String paymentNo,
    /**
     * 表示供应商 ID。
     */
    Long supplierId,
    /**
     * 表示供应商名称。
     */
    String supplierName,
    /**
     * 表示应付 ID。
     */
    Long payableId,
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
