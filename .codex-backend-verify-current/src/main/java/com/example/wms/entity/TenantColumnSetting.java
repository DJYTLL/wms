package com.example.wms.entity;

import java.time.Instant;

// 租户列配置实体
public class TenantColumnSetting {
    private Long tenantId;
    private String pageKey;
    private String visibleColumns;
    private String updatedBy;
    private Instant updatedAt;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getVisibleColumns() {
        return visibleColumns;
    }

    public void setVisibleColumns(String visibleColumns) {
        this.visibleColumns = visibleColumns;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
