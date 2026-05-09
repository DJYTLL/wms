package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 新增打印模板请求（ERP进销存）
public record ErpPrintTemplateCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String docType,
    String headerTitle,
    String subTitle,
    String footerNote,
    String fieldConfig,
    Integer sortNo,
    Boolean enabled,
    Boolean isDefault,
    String remark
) {
}
