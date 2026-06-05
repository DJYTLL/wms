package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockWarningView;
import com.example.wms.service.erp.ErpStockWarningService;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ErpStockWarningAnomalyServiceTests {

    @Autowired
    private ErpStockWarningService erpStockWarningService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestAuditContext.clear();
    }

    @Test
    void anomalyPageShouldReturnProductFallbackOnlyRecords() {
        long tenantId = 9010L;
        long categoryId = 9401L;
        long unitId = 9402L;
        long fallbackProductId = 9403L;
        long policyProductId = 9404L;
        long warehouseId = 9405L;
        Instant now = Instant.parse("2026-05-31T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseId, "异常仓", now);
        insertProduct(tenantId, fallbackProductId, categoryId, unitId, now);
        insertProduct(tenantId, policyProductId, categoryId, unitId, now);
        updateProductFallbackPolicy(tenantId, fallbackProductId, warehouseId, new BigDecimal("6"), new BigDecimal("4"), BigDecimal.ZERO);
        insertPolicy(tenantId, policyProductId, warehouseId, new BigDecimal("10"), null, now);
        insertStockBalance(tenantId, fallbackProductId, warehouseId, new BigDecimal("1"), now);
        insertStockBalance(tenantId, policyProductId, warehouseId, new BigDecimal("2"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> page = erpStockWarningService.pageAnomalies(
            1, 20, null, null, "PRODUCT_FALLBACK_ONLY"
        );

        assertThat(page.items()).hasSize(1);
        ErpStockWarningView item = page.items().get(0);
        assertThat(item.getProductId()).isEqualTo(fallbackProductId);
        assertThat(item.getWarehouseId()).isNull();
        assertThat(item.getWarehouseName()).isNull();
        assertThat(item.getPolicySource()).isEqualTo("PRODUCT_FALLBACK");
        assertThat(item.getHasPolicyAnomaly()).isTrue();
        assertThat(item.getAnomalyTypes()).containsExactly("PRODUCT_FALLBACK_ONLY");

        PageResponse<ErpStockWarningView> warehouseFilteredPage = erpStockWarningService.pageAnomalies(
            1, 20, null, warehouseId, "PRODUCT_FALLBACK_ONLY"
        );
        assertThat(warehouseFilteredPage.items()).isEmpty();
    }

    @Test
    void anomalyPageShouldKeepProductFallbackWhenWarehousePolicyExists() {
        long tenantId = 9011L;
        long categoryId = 9701L;
        long unitId = 9702L;
        long productId = 9703L;
        long warehouseId = 9704L;
        Instant now = Instant.parse("2026-06-01T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseId, "默认仓库", now);
        insertProduct(tenantId, productId, categoryId, unitId, now);
        updateProductFallbackPolicy(tenantId, productId, warehouseId, new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1000"));
        insertPolicy(tenantId, productId, warehouseId, new BigDecimal("30"), null, now);
        insertStockBalance(tenantId, productId, warehouseId, new BigDecimal("10"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> page = erpStockWarningService.pageAnomalies(
            1, 20, null, null, "PRODUCT_FALLBACK_ONLY"
        );

        assertThat(page.items()).hasSize(1);
        ErpStockWarningView item = page.items().get(0);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getWarehouseId()).isNull();
        assertThat(item.getWarehouseName()).isNull();
        assertThat(item.getPolicySource()).isEqualTo("PRODUCT_FALLBACK");
        assertThat(item.getTotalQty()).isEqualByComparingTo("10.0000");
        assertThat(item.getMinStock()).isEqualByComparingTo("100.0000");
        assertThat(item.getAnomalyTypes()).containsExactly("PRODUCT_FALLBACK_ONLY");
    }

    private void clearTenantData(long tenantId) {
        jdbcTemplate.update("DELETE FROM erp_product_stock_policy WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_stock_balance WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_product WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_warehouse WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_unit WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_category WHERE tenant_id = ?", tenantId);
    }

    private void insertCategory(long tenantId, long categoryId, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_category (id, tenant_id, code, name, is_enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            categoryId, tenantId, "CAT-" + categoryId, "测试分类", true, ts(now), ts(now)
        );
    }

    private void insertUnit(long tenantId, long unitId, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_unit (id, tenant_id, code, name, precision, is_enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            unitId, tenantId, "UNIT-" + unitId, "个", 0, true, ts(now), ts(now)
        );
    }

    private void insertWarehouse(long tenantId, long warehouseId, String name, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_warehouse (id, tenant_id, code, name, is_enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            warehouseId, tenantId, "WH-" + warehouseId, name, true, ts(now), ts(now)
        );
    }

    private void insertProduct(long tenantId, long productId, long categoryId, long unitId, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_product (
                id, tenant_id, code, name, category_id, unit_id,
                cost_price, sale_price, tax_rate, safety_stock, min_stock, max_stock,
                is_batch, is_enabled, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            productId, tenantId, "P-" + productId, "测试商品", categoryId, unitId,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, false, true, ts(now), ts(now)
        );
    }

    private void updateProductFallbackPolicy(long tenantId,
                                             long productId,
                                             Long warehouseId,
                                             BigDecimal safetyStock,
                                             BigDecimal minStock,
                                             BigDecimal maxStock) {
        jdbcTemplate.update(
            """
            UPDATE erp_product
            SET default_warehouse_id = ?,
                safety_stock = ?,
                min_stock = ?,
                max_stock = ?
            WHERE tenant_id = ? AND id = ?
            """,
            warehouseId, safetyStock, minStock, maxStock, tenantId, productId
        );
    }

    private void insertPolicy(long tenantId,
                              long productId,
                              long warehouseId,
                              BigDecimal minStock,
                              BigDecimal maxStock,
                              Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_product_stock_policy (
                tenant_id, product_id, warehouse_id, min_stock, max_stock, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            tenantId, productId, warehouseId, minStock, maxStock, ts(now), ts(now)
        );
    }

    private void insertStockBalance(long tenantId,
                                    long productId,
                                    long warehouseId,
                                    BigDecimal qtyOnHand,
                                    Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_stock_balance (
                tenant_id, product_id, warehouse_id, location_id, qty_on_hand, updated_by, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            tenantId, productId, warehouseId, null, qtyOnHand, "tester", ts(now)
        );
    }

    private Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
