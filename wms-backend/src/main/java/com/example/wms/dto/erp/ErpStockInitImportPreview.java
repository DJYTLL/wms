package com.example.wms.dto.erp;

import java.util.List;
import java.util.Map;

public record ErpStockInitImportPreview(
    List<String> headers,
    List<ErpStockInitImportFieldOption> fields,
    List<ErpStockInitImportHeaderMapping> mappings,
    List<Map<String, String>> sampleRows,
    int totalRows
) {
}
