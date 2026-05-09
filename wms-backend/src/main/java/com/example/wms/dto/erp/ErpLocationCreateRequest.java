package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 新增库位请求（ERP进销存）
public record ErpLocationCreateRequest(
    @NotNull Long warehouseId,
    @NotBlank String code,
    String name,
    String aisle,
    String rack,
    String bin,
    Boolean enabled,
    String remark
) {
}
