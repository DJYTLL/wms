package com.example.wms.dto.erp;

public record ErpCounterpartySubjectMember(
    Long id,
    String code,
    String name,
    String contact,
    String phone,
    String mobile,
    String roleType,
    ErpCounterpartyUnbindCheck unbindCheck
) {
}
