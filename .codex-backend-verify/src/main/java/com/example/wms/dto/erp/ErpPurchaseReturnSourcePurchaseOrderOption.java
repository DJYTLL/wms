package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpPurchaseReturnSourcePurchaseOrderOption(
    Long id,
    String orderNo,
    Long supplierId,
    Instant orderAt
) {
}
