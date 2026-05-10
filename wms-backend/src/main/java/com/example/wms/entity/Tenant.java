package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.AuditableSoftDeleteEntity;

// 租户实体
@TableName("app_tenant")
public class Tenant extends AuditableSoftDeleteEntity {
    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
