package com.example.wms.service.erp.support;

import java.util.List;

public final class ErpCounterpartyGuardRules {
    public static final List<String> BLOCKING_DOCUMENT_STATUSES = List.of("DRAFT", "APPROVED", "OPEN", "SETTLED");
    public static final String RED_FLUSHED_STATUS = "RED_FLUSHED";
    public static final String UNCATEGORIZED_SUPPLIER_TYPE_CODE = "UNCATEGORIZED";
    public static final String UNCATEGORIZED_SUPPLIER_TYPE_NAME = "未分类";

    private ErpCounterpartyGuardRules() {
    }
}
