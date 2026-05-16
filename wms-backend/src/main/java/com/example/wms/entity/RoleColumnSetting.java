package com.example.wms.entity;

import java.time.Instant;

// 角色列配置实体
public class RoleColumnSetting {
    private Long tenantId;
    private Long roleId;
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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
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
