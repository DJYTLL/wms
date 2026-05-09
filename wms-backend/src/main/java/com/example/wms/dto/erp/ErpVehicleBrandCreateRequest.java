package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 新增车型品牌请求（ERP进销存）
public record ErpVehicleBrandCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled,
    String remark
) {
}
