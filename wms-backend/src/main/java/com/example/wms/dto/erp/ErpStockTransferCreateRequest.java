package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ErpStockTransferCreateRequest(
    String transferNo,
    String transferAt,
    @NotEmpty List<ErpStockTransferItemRequest> items,
    String remark
) {
}
