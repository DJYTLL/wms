package com.example.wms;

import com.example.wms.config.PermissionSeedProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SqlTimingPermissionAccessTests {

    @Test
    void permissionSeedProviderIncludesSqlTimingManagePermissions() {
        assertThat(PermissionSeedProvider.permissionSeeds())
            .extracting(PermissionSeedProvider.PermissionSeed::code)
            .contains("system-config:sql-timing:view", "system-config:sql-timing:edit");
    }

    @Test
    void systemConfigControllerAllowsDedicatedSqlTimingPermissions() throws Exception {
        assertPreAuthorize(
            com.example.wms.controller.SystemConfigController.class,
            "getByKey",
            new Class<?>[] {String.class},
            "hasRole('super_admin') or @systemConfigPermissionEvaluator.canView(#key)"
        );
        assertPreAuthorize(
            com.example.wms.controller.SystemConfigController.class,
            "update",
            new Class<?>[] {String.class, com.example.wms.dto.SystemConfigRequest.class},
            "hasRole('super_admin') or @systemConfigPermissionEvaluator.canEdit(#key)"
        );
    }

    @Test
    void sqlTimingManagePermissionMigrationSeedsBackfillPermissions() throws Exception {
        String migration = readMigration("db/migration/V136__seed_sql_timing_manage_permissions.sql");

        assertThat(migration)
            .contains("system-config:sql-timing:view")
            .contains("system-config:sql-timing:edit")
            .contains("WHERE role.code = 'admin'")
            .contains("WHERE role.code = 'super_admin'");
    }

    private void assertPreAuthorize(Class<?> type,
                                    String methodName,
                                    Class<?>[] parameterTypes,
                                    String expectedExpression) throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(expectedExpression);
    }

    private String readMigration(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
