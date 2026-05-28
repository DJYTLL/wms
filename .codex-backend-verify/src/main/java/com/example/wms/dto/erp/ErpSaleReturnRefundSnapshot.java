package com.example.wms.dto.erp;

import java.math.BigDecimal;

public record ErpSaleReturnRefundSnapshot(
    Long returnId,
    String refundStatus,
    BigDecimal refundUnpaidAmount
) {
}
