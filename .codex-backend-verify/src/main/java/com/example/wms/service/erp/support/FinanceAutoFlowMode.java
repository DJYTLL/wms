package com.example.wms.service.erp.support;

import java.util.Locale;

// 审核后财务自动联动模式。
public enum FinanceAutoFlowMode {
    AR_AP_ONLY,
    AR_AP_WITH_DRAFT_PAYMENT,
    AR_AP_WITH_APPROVED_PAYMENT;

    public static FinanceAutoFlowMode fromValue(String value) {
        return FinanceAutoFlowMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
