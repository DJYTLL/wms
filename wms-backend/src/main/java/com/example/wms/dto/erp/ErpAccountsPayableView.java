package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

/**

 * ERP 应付账款用于返回视图展示数据。

 */
public record ErpAccountsPayableView(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示供应商 ID。
     */
    Long supplierId,
    /**
     * 表示供应商名称。
     */
    String supplierName,
    /**
     * 表示合计金额。
     */
    BigDecimal totalAmount,
    /**
     * 表示已支付金额。
     */
    BigDecimal paidAmount,
    /**
     * 表示优惠金额。
     */
    BigDecimal discountAmount,
    /**
     * 表示unpaid金额。
     */
    BigDecimal unpaidAmount,
    /**
     * 表示状态。
     */
    String status,
    /**
     * 表示创建时间。
     */
    Instant createdAt
) {
}
