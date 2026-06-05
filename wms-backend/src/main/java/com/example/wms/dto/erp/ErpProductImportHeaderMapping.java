package com.example.wms.dto.erp;

public record ErpProductImportHeaderMapping(
    String excelHeader,
    String fieldKey,
    String fieldLabel,
    String matchType,
    String sampleValue
) {
}
