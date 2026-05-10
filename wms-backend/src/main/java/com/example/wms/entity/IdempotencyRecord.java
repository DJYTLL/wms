package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.SoftDeleteEntity;

import java.time.Instant;

// 幂等记录实体
@TableName("app_idempotency")
public class IdempotencyRecord extends SoftDeleteEntity {
    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("method")
    private String method;

    @TableField("path")
    private String path;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("username")
    private String username;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("expires_at")
    private Instant expiresAt;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

}
