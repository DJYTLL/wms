package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

public record ErpSupplierImportRequest(
    String sourceName,
    @NotBlank String rawTable
) {
}
