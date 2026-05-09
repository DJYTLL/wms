package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 更新车型品牌请求（ERP进销存）
public record ErpVehicleBrandUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled,
    String remark
) {
}
