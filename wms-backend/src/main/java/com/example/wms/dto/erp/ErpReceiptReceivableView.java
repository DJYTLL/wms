package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 收款应收视图用于返回视图展示数据。

 */
public record ErpReceiptReceivableView(
    /**
     * 表示应收 ID。
     */
    Long receivableId,
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
