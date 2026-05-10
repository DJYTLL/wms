package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

// 租户菜单映射（控制租户可见菜单）
@TableName("app_tenant_menu")
public class TenantMenu extends TenantAuditableSoftDeleteEntity {
    @TableField("menu_id")
    private Long menuId;

    @TableField("is_enabled")
    private boolean enabled = true;

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
