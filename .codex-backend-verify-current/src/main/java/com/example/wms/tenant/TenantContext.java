package com.example.wms.tenant;

// 租户上下文：用于线程内隔离当前租户
public final class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long requireTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalArgumentException("缺少租户信息");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
