package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// ERP 往来主体更新请求
public record ErpCounterpartySubjectUpdateRequest(
    @NotBlank String name,
    String region,
    String unifiedCreditCode,
    Boolean enabled,
    String remark
) {
}
