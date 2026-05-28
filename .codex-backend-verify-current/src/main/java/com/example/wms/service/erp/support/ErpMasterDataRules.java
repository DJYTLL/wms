package com.example.wms.service.erp.support;

import java.util.Set;
import java.util.regex.Pattern;

// ERP 仓库/库位主数据治理规则
public final class ErpMasterDataRules {
    private static final Pattern MASTER_CODE_PATTERN = Pattern.compile("^[A-Z0-9_/-]+$");

    public static final Set<String> PENDING_ORDER_STATUSES = Set.of("DRAFT");

    private ErpMasterDataRules() {
    }

    public static String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    public static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizeMasterCode(String value, String emptyMessage) {
        String normalized = normalizeRequiredText(value, emptyMessage).toUpperCase();
        if (!MASTER_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("编码只能包含字母、数字、短横线、下划线或斜杠");
        }
        return normalized;
    }

    public static String pendingStatusSqlList() {
        return String.join(", ", PENDING_ORDER_STATUSES.stream().map(status -> "'" + status + "'").toList());
    }
}
