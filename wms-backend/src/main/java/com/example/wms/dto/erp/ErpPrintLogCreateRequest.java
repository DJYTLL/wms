package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 打印日志请求（ERP进销存）
public record ErpPrintLogCreateRequest(
    @NotBlank String docType,
    @NotNull Long docId,
    Long templateId
) {
}
