package com.example.wms.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

import java.time.Instant;

// 通用审计字段基类
public abstract class AuditableEntity extends BaseEntity {
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
