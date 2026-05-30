package com.example.wms.dto.erp;

import java.util.List;

// 初始库存 Excel 导入结果
public record ErpStockInitImportResult(
    Long countId,
    String countNo,
    int totalCount,
    int warningCount,
    List<String> warnings
) {
}
