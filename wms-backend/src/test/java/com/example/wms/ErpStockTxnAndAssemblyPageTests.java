package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.service.erp.ErpAssemblyOrderService;
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
class ErpStockTxnAndAssemblyPageTests {

    @Autowired
    private ErpStockService erpStockService;

    @Autowired
    private ErpAssemblyOrderService erpAssemblyOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestAuditContext.clear();
    }

    @Test
    void stockTxnPageShouldReturnResolvedProductWarehouseAndLocationNames() {
        long tenantId = 9860L;
        long categoryId = 9861L;
        long unitId = 9862L;
        long warehouseId = 9863L;
        long locationId = 9864L;
        long productId = 9865L;
        long txnId = 9866L;
        Instant now = Instant.parse("2026-06-03T02:00:00Z");

        clearCommonTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseId, "华南成品仓", now);
        insertLocation(tenantId, locationId, warehouseId, "B-02-03", now);
        insertProduct(tenantId, productId, categoryId, unitId, "火花塞", now);
        insertStockTxn(tenantId, txnId, productId, warehouseId, locationId, now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpStockTxn> page = erpStockService.pageTxn(1, 20, null, null, null);

        assertThat(page.items()).hasSize(1);
        ErpStockTxn txn = page.items().get(0);
        assertThat(txn.getProductName()).isEqualTo("火花塞");
        assertThat(txn.getWarehouseName()).isEqualTo("华南成品仓");
        assertThat(txn.getLocationName()).isEqualTo("B-02-03");
    }

    @Test
    void assemblyOrderPageShouldReturnResolvedFinishedProductName() {
        long tenantId = 9870L;
        long categoryId = 9871L;
        long unitId = 9872L;
        long warehouseId = 9873L;
        long productId = 9874L;
        long orderId = 9875L;
        Instant now = Instant.parse("2026-06-03T03:00:00Z");

        clearAssemblyTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseId, "组装仓", now);
        insertProduct(tenantId, productId, categoryId, unitId, "刹车片套装", now);
        insertAssemblyOrder(tenantId, orderId, productId, warehouseId, now);

        TenantContext.setTenantId(tenantId);

        PageResponse<ErpAssemblyOrder> page = erpAssemblyOrderService.page(1, 20, null, null, "ASSEMBLE", null, null);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).getFinishedProductName()).isEqualTo("刹车片套装");
    }

    private void clearCommonTenantData(long tenantId) {
        jdbcTemplate.update("DELETE FROM erp_stock_txn WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_location WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_product WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_warehouse WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_unit WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_category WHERE tenant_id = ?", tenantId);
    }

    private void clearAssemblyTenantData(long tenantId) {
        jdbcTemplate.update("DELETE FROM erp_assembly_order_item WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_assembly_order WHERE tenant_id = ?", tenantId);
        clearCommonTenantData(tenantId);
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

    private void insertStockTxn(long tenantId, long txnId, long productId, long warehouseId, long locationId, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_stock_txn (
                id, tenant_id, txn_no, biz_type, biz_id, biz_item_id, product_id, warehouse_id, location_id,
                qty_delta, qty_before, qty_after, unit_cost, total_cost, operator, operator_id, remark, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            txnId, tenantId, "TXN-" + txnId, "STOCK_COUNT", 1L, null, productId, warehouseId, locationId,
            new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("20"), "tester", null, "测试流水", ts(now)
        );
    }

    private void insertAssemblyOrder(long tenantId, long orderId, long productId, long warehouseId, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_assembly_order (
                id, tenant_id, order_no, order_type, status, order_at, finished_product_id, finished_qty,
                warehouse_id, location_id, labor_cost, total_cost, unit_cost, inventory_reserved, created_at, created_by, updated_at, updated_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            orderId, tenantId, "AO-" + orderId, "ASSEMBLE", "DRAFT", ts(now), productId, new BigDecimal("6"),
            warehouseId, null, BigDecimal.ZERO, new BigDecimal("80"), new BigDecimal("13.3333"), false, ts(now), "tester", ts(now), "tester"
        );
    }

    private Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
