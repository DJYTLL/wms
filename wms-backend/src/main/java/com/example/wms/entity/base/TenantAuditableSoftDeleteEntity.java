package com.example.wms.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;

// 含租户与审计字段的逻辑删除基类
public abstract class TenantAuditableSoftDeleteEntity extends AuditableSoftDeleteEntity {
    @TableField("tenant_id")
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
