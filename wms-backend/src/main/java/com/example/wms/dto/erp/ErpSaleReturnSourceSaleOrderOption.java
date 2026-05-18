package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpSaleReturnSourceSaleOrderOption(
    Long id,
    String orderNo,
    Long customerId,
    Instant orderAt
) {
}
