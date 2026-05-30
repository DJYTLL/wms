package com.example.wms.dto.erp;

public record ErpCounterpartyPendingDoc(
    String docType,
    Long docId,
    String orderNo,
    String status,
    String routeKey
) {
}
