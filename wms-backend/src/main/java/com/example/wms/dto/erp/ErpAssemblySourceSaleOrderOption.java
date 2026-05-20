package com.example.wms.dto.erp;

import java.time.Instant;

public record ErpAssemblySourceSaleOrderOption(
    Long id,
    String orderNo,
    String status,
    Long customerId,
    String customerName,
    Instant orderAt
) {
}
