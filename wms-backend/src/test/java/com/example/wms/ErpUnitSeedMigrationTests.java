package com.example.wms;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErpUnitSeedMigrationTests {
    private static final Path TENANT_6_UNIT_SEED = Path.of(
        "src/main/resources/db/migration/V130__seed_tenant_6_common_units.sql"
    );

    @Test
    void tenant6UnitSeedImportsScreenshotAndCommonUnitsIdempotently() throws IOException {
        String migration = Files.readString(TENANT_6_UNIT_SEED);

        assertThat(migration).contains("tenant_id = 6");
        assertThat(migration).doesNotContain("CROSS JOIN");
        assertThat(migration).contains("NOT EXISTS");
        assertThat(migration).contains("u.tenant_id = 6");
        assertThat(migration).contains("u.deleted_at IS NULL");
        assertThat(migration).contains("u.code = s.code OR u.name = s.name");

        List<String> screenshotUnits = List.of(
            "把", "次", "个", "根", "公斤", "盒", "件", "卷", "颗", "片",
            "瓶", "台", "套", "条", "桶", "箱", "元", "支", "只"
        );
        assertThat(migration).contains(screenshotUnits.toArray(String[]::new));

        List<String> commonUnits = List.of(
            "包", "袋", "罐", "双", "张", "辆", "打", "克", "吨", "升",
            "毫升", "厘米", "毫米", "平方米", "立方米", "米", "斤"
        );
        assertThat(migration).contains(commonUnits.toArray(String[]::new));
    }
}
