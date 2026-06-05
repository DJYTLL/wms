package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.service.erp.ErpStockService;
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
class ErpStockBalancePageTests {

    @Autowired
    private ErpStockService erpStockService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestAuditContext.clear();
    }

    @Test
    void pageBalanceShouldReturnResolvedProductWarehouseAndLocationNames() {
        long tenantId = 9850L;
        long categoryId = 9851L;
        long unitId = 9852L;
        long warehouseId = 9853L;
        long locationId = 9854L;
        long productId = 9855L;
        long balanceId = 9856L;
        Instant now = Instant.parse("2026-06-03T01:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseId, "华东一号仓", now);
        insertLocation(tenantId, locationId, warehouseId, "A-01-01", now);
        insertProduct(tenantId, productId, categoryId, unitId, "机油滤芯", now);
        insertBalance(balanceId, tenantId, productId, warehouseId, locationId, new BigDecimal("12"), new BigDecimal("3"), now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockBalance> page = erpStockService.pageBalance(1, 20, null, null, null);

        assertThat(page.items()).hasSize(1);
        ErpStockBalance balance = page.items().get(0);
        assertThat(balance.getProductName()).isEqualTo("机油滤芯");
        assertThat(balance.getWarehouseName()).isEqualTo("华东一号仓");
        assertThat(balance.getLocationName()).isEqualTo("A-01-01");
        assertThat(balance.getQtyAvailable()).isEqualByComparingTo("9");
    }

    private void clearTenantData(long tenantId) {
        jdbcTemplate.update("DELETE FROM erp_stock_balance WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_location WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_product WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_warehouse WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_unit WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_category WHERE tenant_id = ?", tenantId);
    }

    private void insertCategory(long tenantId, long categoryId, Instant now) {
        jdbcTemplate.update(
            "INSERT INTO erp_category (id, tenant_id, code, name, is_enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            categoryId, tenantId, "CAT-" + categoryId, "测试分类", true, ts(now), ts(now)
        );
    }

    private void insertUnit(long tenantId, long unitId, Instant now) {
        jdbcTemplate.update(
            "INSERT INTO erp_unit (id, tenant_id, code, name, precision, is_enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            unitId, tenantId, "UNIT-" + unitId, "个", 0, true, ts(now), ts(now)
        );
    }

    private void insertWarehouse(long tenantId, long warehouseId, String name, Instant now) {
        jdbcTemplate.update(
            "INSERT INTO erp_warehouse (id, tenant_id, code, name, is_enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            warehouseId, tenantId, "WH-" + warehouseId, name, true, ts(now), ts(now)
        );
    }

    private void insertLocation(long tenantId, long locationId, long warehouseId, String name, Instant now) {
        jdbcTemplate.update(
            "INSERT INTO erp_location (id, tenant_id, warehouse_id, code, name, is_enabled, is_default, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            locationId, tenantId, warehouseId, "LOC-" + locationId, name, true, false, ts(now), ts(now)
        );
    }

    private void insertProduct(long tenantId, long productId, long categoryId, long unitId, String productName, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_product (
                id, tenant_id, code, name, category_id, unit_id,
                cost_price, sale_price, tax_rate, safety_stock, min_stock, max_stock,
                is_batch, is_enabled, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            productId, tenantId, "P-" + productId, productName, categoryId, unitId,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, false, true, ts(now), ts(now)
        );
    }

    private void insertBalance(long balanceId,
                               long tenantId,
                               long productId,
                               long warehouseId,
                               long locationId,
                               BigDecimal onHand,
                               BigDecimal reserved,
                               Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_stock_balance (
                id, tenant_id, product_id, warehouse_id, location_id, qty_on_hand, qty_reserved, updated_by, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            balanceId, tenantId, productId, warehouseId, locationId, onHand, reserved, "tester", ts(now)
        );
    }

    private Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
