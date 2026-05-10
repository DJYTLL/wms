package com.example.wms.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.wms.audit.RequestAuditContext;
import com.example.wms.security.CurrentActor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

// MyBatis 自动填充：统一补审计与逻辑删除元数据
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        strictInsertFill(metaObject, "createdAt", Instant.class, now);
        strictInsertFill(metaObject, "updatedAt", Instant.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
        Object deletedAt = getFieldValByName("deletedAt", metaObject);
        if (deletedAt != null) {
            strictUpdateFill(metaObject, "deletedBy", String.class, CurrentActor.username());
            RequestAuditContext context = RequestAuditContext.get();
            String deleteReason = context == null ? null : context.getDeleteReason();
            if (deleteReason != null && !deleteReason.isBlank()) {
                strictUpdateFill(metaObject, "deleteReason", String.class, deleteReason.trim());
            }
        }
    }
}
