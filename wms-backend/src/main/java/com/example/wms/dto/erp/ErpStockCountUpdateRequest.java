package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 更新库存盘点单请求（ERP进销存）
public record ErpStockCountUpdateRequest(
    String countAt,
    Long warehouseId,
    Long locationId,
    @NotEmpty List<ErpStockCountItemRequest> items,
    String remark
) {
}
