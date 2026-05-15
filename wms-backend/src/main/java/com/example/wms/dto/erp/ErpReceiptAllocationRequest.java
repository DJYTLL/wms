package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 收款单用于接收接口请求参数。

 */
public record ErpReceiptAllocationRequest(
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
    BigDecimal discountAmount
) {
}
