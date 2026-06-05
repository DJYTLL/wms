package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpProductStockPolicyRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.dto.erp.ErpStockWarningView;
import com.example.wms.service.erp.ErpProductService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ErpStockWarningServiceTests {

    @Autowired
    private ErpStockWarningService erpStockWarningService;

    @Autowired
    private ErpProductService erpProductService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestAuditContext.clear();
    }

    @Test
    void pageShouldUseWarehousePolicyInsteadOfSummedProductInventory() {
        long tenantId = 9001L;
        long categoryId = 9101L;
        long unitId = 9102L;
        long productId = 9103L;
        long warehouseAId = 9104L;
        long warehouseBId = 9105L;
        Instant now = Instant.parse("2026-05-31T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseAId, "A仓", now);
        insertWarehouse(tenantId, warehouseBId, "B仓", now);
        insertProduct(tenantId, productId, categoryId, unitId, now);
        insertPolicy(tenantId, productId, warehouseAId, new BigDecimal("10"), null, now);
        insertPolicy(tenantId, productId, warehouseBId, new BigDecimal("10"), null, now);
        insertStockBalance(tenantId, productId, warehouseAId, new BigDecimal("2"), now);
        insertStockBalance(tenantId, productId, warehouseBId, new BigDecimal("20"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> page = erpStockWarningService.page(1, 20, null, null, null, null, null);

        assertThat(page.items()).hasSize(1);
        ErpStockWarningView warning = page.items().get(0);
        assertThat(warning.getProductId()).isEqualTo(productId);
        assertThat(warning.getWarehouseId()).isEqualTo(warehouseAId);
        assertThat(warning.getWarehouseName()).isEqualTo("A仓");
        assertThat(warning.getTotalQty()).isEqualByComparingTo("2.0000");
        assertThat(warning.getMinStock()).isEqualByComparingTo("10.0000");
        assertThat(warning.getSafetyStock()).isEqualByComparingTo("0.0000");
        assertThat(warning.getPolicySource()).isEqualTo("WAREHOUSE_POLICY");
        assertThat(warning.getStatus()).isEqualTo("LOW");
    }

    @Test
    void pageShouldReflectUpdatedWarehousePolicyImmediately() {
        long tenantId = 9002L;
        long categoryId = 9201L;
        long unitId = 9202L;
        long productId = 9203L;
        long warehouseAId = 9204L;
        long warehouseBId = 9205L;
        Instant now = Instant.parse("2026-05-31T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseAId, "A仓", now);
        insertWarehouse(tenantId, warehouseBId, "B仓", now);
        insertProduct(tenantId, productId, categoryId, unitId, now);
        insertPolicy(tenantId, productId, warehouseAId, new BigDecimal("10"), null, now);
        insertPolicy(tenantId, productId, warehouseBId, new BigDecimal("10"), null, now);
        insertStockBalance(tenantId, productId, warehouseAId, new BigDecimal("2"), now);
        insertStockBalance(tenantId, productId, warehouseBId, new BigDecimal("20"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> beforeUpdate = erpStockWarningService.page(1, 20, null, null, null, null, null);
        assertThat(beforeUpdate.items()).hasSize(1);
        assertThat(beforeUpdate.items().get(0).getWarehouseId()).isEqualTo(warehouseAId);
        assertThat(beforeUpdate.items().get(0).getStatus()).isEqualTo("LOW");

        RequestAuditContext auditContext = new RequestAuditContext();
        auditContext.setMethod("PUT");
        auditContext.setPath("/api/erp/products/" + productId);
        auditContext.setRequestId("test-stock-warning-update");
        auditContext.setClientIp("127.0.0.1");
        auditContext.setUserAgent("JUnit");
        auditContext.setAuthTenantId(tenantId);
        auditContext.setAuthTenantCode("tenant-" + tenantId);
        auditContext.setCrossTenant(false);
        RequestAuditContext.set(auditContext);

        ErpProductUpdateRequest request = new ErpProductUpdateRequest(
            "P-" + productId,
            "测试商品",
            null,
            null,
            null,
            null,
            Long.valueOf(categoryId),
            Long.valueOf(unitId),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            false,
            null,
            true,
            null,
            null,
            List.of(
                new ErpProductStockPolicyRequest(warehouseAId, null, new BigDecimal("1"), null),
                new ErpProductStockPolicyRequest(warehouseBId, null, new BigDecimal("10"), null)
            ),
            List.of()
        );

        erpProductService.update(productId, request);

        PageResponse<ErpStockWarningView> afterUpdate = erpStockWarningService.page(1, 20, null, null, null, null, null);
        assertThat(afterUpdate.items()).isEmpty();
    }

    @Test
    void pageShouldFilterByWarehouseAndPolicySource() {
        long tenantId = 9003L;
        long categoryId = 9301L;
        long unitId = 9302L;
        long productWithPolicyId = 9303L;
        long productFallbackId = 9304L;
        long warehouseAId = 9305L;
        long warehouseBId = 9306L;
        Instant now = Instant.parse("2026-05-31T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseAId, "A仓", now);
        insertWarehouse(tenantId, warehouseBId, "B仓", now);
        insertProduct(tenantId, productWithPolicyId, categoryId, unitId, now);
        insertProduct(tenantId, productFallbackId, categoryId, unitId, now);
        updateProductFallbackPolicy(tenantId, productFallbackId, warehouseBId, new BigDecimal("8"), new BigDecimal("5"), BigDecimal.ZERO);
        insertPolicy(tenantId, productWithPolicyId, warehouseAId, new BigDecimal("10"), null, now);
        insertStockBalance(tenantId, productWithPolicyId, warehouseAId, new BigDecimal("2"), now);
        insertStockBalance(tenantId, productFallbackId, warehouseBId, new BigDecimal("1"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> warehouseFiltered = erpStockWarningService.page(
            1, 20, null, warehouseAId, null, null, null
        );
        assertThat(warehouseFiltered.items()).hasSize(1);
        assertThat(warehouseFiltered.items().get(0).getProductId()).isEqualTo(productWithPolicyId);
        assertThat(warehouseFiltered.items().get(0).getWarehouseId()).isEqualTo(warehouseAId);

        PageResponse<ErpStockWarningView> fallbackOnly = erpStockWarningService.page(
            1, 20, null, null, null, "PRODUCT_FALLBACK", null
        );
        assertThat(fallbackOnly.items()).hasSize(1);
        ErpStockWarningView fallbackWarning = fallbackOnly.items().get(0);
        assertThat(fallbackWarning.getProductId()).isEqualTo(productFallbackId);
        assertThat(fallbackWarning.getWarehouseId()).isNull();
        assertThat(fallbackWarning.getWarehouseName()).isNull();
        assertThat(fallbackWarning.getPolicySource()).isEqualTo("PRODUCT_FALLBACK");
        assertThat(fallbackWarning.getSafetyStock()).isEqualByComparingTo("8.0000");

        PageResponse<ErpStockWarningView> fallbackWarehouseFiltered = erpStockWarningService.page(
            1, 20, null, warehouseBId, null, null, null
        );
        assertThat(fallbackWarehouseFiltered.items()).isEmpty();
    }

    @Test
    void pageShouldUseProductFallbackForWarehousesWithoutExplicitPolicy() {
        long tenantId = 9004L;
        long categoryId = 9501L;
        long unitId = 9502L;
        long productId = 9503L;
        long warehouseWithPolicyId = 9504L;
        long warehouseFallbackId = 9505L;
        Instant now = Instant.parse("2026-06-01T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseWithPolicyId, "策略仓", now);
        insertWarehouse(tenantId, warehouseFallbackId, "兜底仓", now);
        insertProduct(tenantId, productId, categoryId, unitId, now);
        updateProductFallbackPolicy(tenantId, productId, warehouseFallbackId, new BigDecimal("5"), new BigDecimal("10"), BigDecimal.ZERO);
        insertPolicy(tenantId, productId, warehouseWithPolicyId, new BigDecimal("2"), null, now);
        insertStockBalance(tenantId, productId, warehouseWithPolicyId, new BigDecimal("5"), now);
        insertStockBalance(tenantId, productId, warehouseFallbackId, new BigDecimal("3"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> page = erpStockWarningService.page(1, 20, null, null, null, null, null);

        assertThat(page.items()).hasSize(1);
        ErpStockWarningView warning = page.items().get(0);
        assertThat(warning.getProductId()).isEqualTo(productId);
        assertThat(warning.getWarehouseId()).isNull();
        assertThat(warning.getWarehouseName()).isNull();
        assertThat(warning.getLocationId()).isNull();
        assertThat(warning.getLocationName()).isNull();
        assertThat(warning.getPolicySource()).isEqualTo("PRODUCT_FALLBACK");
        assertThat(warning.getTotalQty()).isEqualByComparingTo("8.0000");
        assertThat(warning.getMinStock()).isEqualByComparingTo("10.0000");
        assertThat(warning.getHasPolicyAnomaly()).isTrue();
    }

    @Test
    void pageShouldKeepProductLevelWarningWhenWarehousePoliciesAlsoWarn() {
        long tenantId = 9005L;
        long categoryId = 9601L;
        long unitId = 9602L;
        long productId = 9603L;
        long warehouseAId = 9604L;
        long warehouseBId = 9605L;
        Instant now = Instant.parse("2026-06-01T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseAId, "默认仓库", now);
        insertWarehouse(tenantId, warehouseBId, "副仓", now);
        insertProduct(tenantId, productId, categoryId, unitId, now);
        updateProductFallbackPolicy(tenantId, productId, warehouseAId, new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1000"));
        insertPolicy(tenantId, productId, warehouseAId, new BigDecimal("30"), null, now);
        insertPolicy(tenantId, productId, warehouseBId, new BigDecimal("30"), null, now);
        insertStockBalance(tenantId, productId, warehouseAId, new BigDecimal("10"), now);
        insertStockBalance(tenantId, productId, warehouseBId, new BigDecimal("20"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockWarningView> page = erpStockWarningService.page(1, 20, null, null, null, null, null);

        assertThat(page.items()).hasSize(3);
        assertThat(page.items())
            .filteredOn(item -> "PRODUCT_FALLBACK".equals(item.getPolicySource()))
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getProductId()).isEqualTo(productId);
                assertThat(item.getWarehouseId()).isNull();
                assertThat(item.getWarehouseName()).isNull();
                assertThat(item.getTotalQty()).isEqualByComparingTo("30.0000");
                assertThat(item.getMinStock()).isEqualByComparingTo("100.0000");
                assertThat(item.getMaxStock()).isEqualByComparingTo("1000.0000");
            });
        assertThat(page.items())
            .filteredOn(item -> "WAREHOUSE_POLICY".equals(item.getPolicySource()))
            .extracting(ErpStockWarningView::getWarehouseId)
            .containsExactlyInAnyOrder(warehouseAId, warehouseBId);
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
