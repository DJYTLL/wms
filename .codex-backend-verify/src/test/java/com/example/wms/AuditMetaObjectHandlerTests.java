package com.example.wms;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.wms.audit.RequestAuditContext;
import com.example.wms.config.AuditMetaObjectHandler;
import com.example.wms.entity.Role;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMetaObjectHandlerTests {
    private final AuditMetaObjectHandler handler = new AuditMetaObjectHandler();

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), Role.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestAuditContext.clear();
    }

    @Test
    void insertFillPopulatesCreatedAndUpdatedAt() {
        Role role = new Role();
        MetaObject metaObject = SystemMetaObject.forObject(role);

        handler.insertFill(metaObject);

        assertThat(role.getCreatedAt()).isNotNull();
        assertThat(role.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateFillPopulatesDeleteMetadataWhenDeleting() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("tester", "n/a", List.of())
        );
        RequestAuditContext context = new RequestAuditContext();
        context.setDeleteReason("清理重复数据");
        RequestAuditContext.set(context);

        Role role = new Role();
        role.setDeletedAt(Instant.now());
        MetaObject metaObject = SystemMetaObject.forObject(role);

        handler.updateFill(metaObject);

        assertThat(role.getUpdatedAt()).isNotNull();
        assertThat(role.getDeletedBy()).isEqualTo("tester");
        assertThat(role.getDeleteReason()).isEqualTo("清理重复数据");
    }
}
