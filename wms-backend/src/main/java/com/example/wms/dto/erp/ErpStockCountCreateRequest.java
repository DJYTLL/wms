package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 新增库存盘点单请求（ERP进销存）
public record ErpStockCountCreateRequest(
    String countNo,
    String countType,
    String adjustmentReason,
    Long warehouseId,
    Long locationId,
    String countAt,
    @NotEmpty List<ErpStockCountItemRequest> items,
    String remark
) {
}
