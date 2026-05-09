package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 更新车型车系请求（ERP进销存）
public record ErpVehicleSeriesUpdateRequest(
    @NotNull Long brandId,
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled,
    String remark
) {
}
