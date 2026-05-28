package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// ERP 供应商类型更新请求
public record ErpSupplierTypeUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled,
    Integer sort,
    String remark
) {
}
