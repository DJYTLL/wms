package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 更新结算方式请求（ERP进销存）
public record ErpSettlementMethodUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Integer sortNo,
    Boolean enabled,
    Boolean isDefault,
    String remark
) {
}
