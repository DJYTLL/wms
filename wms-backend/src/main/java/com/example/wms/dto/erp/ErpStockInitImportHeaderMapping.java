package com.example.wms.dto.erp;

public record ErpStockInitImportHeaderMapping(
    String excelHeader,
    String fieldKey,
    String fieldLabel,
    String matchType,
    String sampleValue
) {
}
