package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

// 更新供应商请求（ERP进销存）
public record ErpSupplierUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String shortName,
    String contact,
    String phone,
    String mobile,
    String email,
    String address,
    String taxNo,
    String bankName,
    String bankAccount,
    String paymentTerms,
    String contacts,
    Boolean enabled,
    Boolean blacklisted,
    String remark
) {
}
