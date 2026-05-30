package com.example.wms.dto.erp;

import java.util.List;

public record ErpCounterpartySubjectDetail(
    Long subjectId,
    String subjectName,
    String region,
    String unifiedCreditCode,
    Integer customerCount,
    Integer supplierCount,
    List<ErpCounterpartySubjectMember> customers,
    List<ErpCounterpartySubjectMember> suppliers
) {
}
