package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 付款应付视图用于返回视图展示数据。

 */
public record ErpPaymentPayableView(
    /**
     * 表示应付 ID。
     */
    Long payableId,
    /**
     * 表示单据编号。
     */
    String orderNo,
    /**
     * 表示allocated金额。
     */
    BigDecimal allocatedAmount,
    /**
     * 表示allocatedDiscount。
     */
    BigDecimal allocatedDiscount,
    /**
     * 表示allocated合计。
     */
    BigDecimal allocatedTotal
) {
}
