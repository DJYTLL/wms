package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// 角色实体：用于权限分组
@TableName("app_role")
public class Role extends TenantAuditableSoftDeleteEntity {
    // 角色编码
    @TableField("code")
    private String code;

    // 角色名称
    @TableField("name")
    private String name;

    // 角色描述
    @TableField("description")
    private String description;

    // 是否启用
    @TableField("is_enabled")
    private boolean enabled = true;

    // 角色拥有的权限（非表字段）
    @TableField(exist = false)
    private Set<Permission> permissions = new HashSet<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
