package com.example.wms.dto.erp;

import java.util.List;

// 初始库存 Excel 导入结果
public record ErpStockInitImportResult(
    Long batchId,
    String batchNo,
    String status,
    Long countId,
    String countNo,
    int totalCount,
    int successCount,
    int failedCount,
    int warningCount,
    List<String> warnings
) {
}
