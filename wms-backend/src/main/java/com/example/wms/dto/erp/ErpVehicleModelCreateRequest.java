package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 新增车型请求（ERP进销存）
public record ErpVehicleModelCreateRequest(
    @NotNull Long seriesId,
    @NotBlank String code,
    @NotBlank String name,
    Integer yearFrom,
    Integer yearTo,
    String displacement,
    String engine,
    Boolean enabled,
    String remark
) {
}
