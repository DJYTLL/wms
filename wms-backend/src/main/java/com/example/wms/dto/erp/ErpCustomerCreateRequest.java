package com.example.wms.dto.erp;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

// 新增客户请求（ERP进销存）
public record ErpCustomerCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Long categoryId,
    String shortName,
    String contact,
    String phone,
    String mobile,
    String email,
    String address,
    String taxNo,
    String bankName,
    String bankAccount,
    String invoiceTitle,
    String paymentTerms,
    String deliveryMethodCode,
    BigDecimal creditLimit,
    String contacts,
    Boolean enabled,
    String remark
) {
}
