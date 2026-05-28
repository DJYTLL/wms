package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.AuditableSoftDeleteEntity;

// 权限实体：细粒度权限点
@TableName("app_permission")
public class Permission extends AuditableSoftDeleteEntity {
    // 权限编码
    @TableField("code")
    private String code;

    // 权限名称
    @TableField("name")
    private String name;

    // 权限描述
    @TableField("description")
    private String description;

    // 是否启用
    @TableField("is_enabled")
    private boolean enabled = true;

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

}
