package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 新增客户类别请求（ERP进销存）
public record ErpCustomerCategoryCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    Integer sortNo,
    Boolean enabled,
    Boolean isDefault,
    String remark
) {
}
