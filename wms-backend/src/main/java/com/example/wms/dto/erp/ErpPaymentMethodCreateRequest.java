package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 新增付款方式请求（ERP进销存）
public record ErpPaymentMethodCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Integer sortNo,
    Boolean enabled,
    Boolean isDefault,
    String remark
) {
}
