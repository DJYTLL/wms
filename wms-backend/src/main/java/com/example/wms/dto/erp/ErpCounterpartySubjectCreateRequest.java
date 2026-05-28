package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// ERP 往来主体新增请求
public record ErpCounterpartySubjectCreateRequest(
    @NotBlank String name,
    String region,
    String unifiedCreditCode,
    Boolean enabled,
    String remark
) {
}
