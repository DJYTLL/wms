package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 新增单位请求（ERP进销存）
public record ErpUnitCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String symbol,
    Integer precision,
    Boolean enabled,
    String remark
) {
}
