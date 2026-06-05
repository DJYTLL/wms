package com.example.wms;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ErpStockColumnPermissionMigrationTextTests {

    @Test
    void stockColumnPermissionMigrationShouldSplitLegacyQtyIntoThreeColumns() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V141__erp_stock_column_permissions_split_qty.sql"));

        assertThat(sql).contains("column:erp-stock:qtyOnHand");
        assertThat(sql).contains("column:erp-stock:qtyLocked");
        assertThat(sql).contains("column:erp-stock:qtyAvailable");
        assertThat(sql).contains("regexp_split_to_table");
        assertThat(sql).contains("app_tenant_column_setting");
        assertThat(sql).contains("app_role_column_setting");
        assertThat(sql).contains("app_role_permission");
    }
}
