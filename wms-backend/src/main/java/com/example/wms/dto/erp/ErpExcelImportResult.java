package com.example.wms.dto.erp;

import java.util.List;

// 通用 Excel 导入结果
public record ErpExcelImportResult(
    int totalCount,
    int successCount,
    int warningCount,
    List<String> warnings
) {
}
