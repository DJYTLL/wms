package com.example.wms.dto.erp;

import java.util.List;

public record ErpCounterpartyUnbindCheck(
    boolean allowed,
    List<String> blockingReasons,
    List<ErpCounterpartyPendingDoc> pendingDocs
) {
}
