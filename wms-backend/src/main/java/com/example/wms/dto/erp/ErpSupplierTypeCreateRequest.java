package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// ERP 供应商类型新增请求
public record ErpSupplierTypeCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled,
    Integer sort,
    String remark
) {
}
