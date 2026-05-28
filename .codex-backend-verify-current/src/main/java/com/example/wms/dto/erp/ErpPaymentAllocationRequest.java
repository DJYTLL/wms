package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 付款单用于接收接口请求参数。

 */
public record ErpPaymentAllocationRequest(
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
    BigDecimal discountAmount
) {
}
