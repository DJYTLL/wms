package com.example.wms.dto.erp;

public record ErpSupplierImportResultItem(
    Integer rowNo,
    String code,
    String name,
    String status,
    String errorField,
    String errorMessage,
    String suggestion,
    String warningMessage,
    String matchedStrategy
) {
}
