package com.example.wms.dto.erp;

import java.util.List;
import java.util.Map;

public record ErpProductImportPreview(
    List<String> headers,
    List<ErpProductImportFieldOption> fields,
    List<ErpProductImportHeaderMapping> mappings,
    List<Map<String, String>> sampleRows,
    int totalRows
) {
}
