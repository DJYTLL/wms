package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpStockOccupancyView(
    String docType,
    String docNo,
    Long docId,
    BigDecimal qty,
    Instant orderAt,
    String routeName
) {
}
