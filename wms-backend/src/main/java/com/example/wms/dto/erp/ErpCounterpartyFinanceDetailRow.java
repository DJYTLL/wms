package com.example.wms.dto.erp;

import java.math.BigDecimal;
import java.time.Instant;

public record ErpCounterpartyFinanceDetailRow(
    String detailType,
    Long bizId,
    String bizNo,
    Long targetId,
    String targetCode,
    String targetName,
    BigDecimal totalAmount,
    BigDecimal unpaidAmount,
    String status,
    Instant createdAt
) {
}
