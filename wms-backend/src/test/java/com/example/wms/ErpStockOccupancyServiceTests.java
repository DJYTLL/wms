package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.erp.ErpStockOccupancyView;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ErpStockOccupancyServiceTests {

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
    void listOccupancyShouldReturnDraftSalePurchaseReturnAndAssemblyRows() {
        long tenantId = 9810L;
        long categoryId = 9811L;
        long unitId = 9812L;
        long warehouseId = 9813L;
        long productId = 9814L;
        long balanceId = 9815L;
        Instant now = Instant.parse("2026-06-03T00:00:00Z");

        clearTenantData(tenantId);
        insertCategory(tenantId, categoryId, now);
        insertUnit(tenantId, unitId, now);
        insertWarehouse(tenantId, warehouseId, "成品仓", now);
        insertProduct(tenantId, productId, categoryId, unitId, now);
        insertBalance(balanceId, tenantId, productId, warehouseId, new BigDecimal("30"), new BigDecimal("9"), now);
        insertSaleOrder(tenantId, 9821L, "SO-TEST-001", now, true);
        insertSaleOrderItem(tenantId, 9822L, 9821L, productId, warehouseId, null, new BigDecimal("3"), now);
        insertPurchaseReturn(tenantId, 9831L, "PR-TEST-001", now, true);
        insertPurchaseReturnItem(tenantId, 9832L, 9831L, productId, warehouseId, null, new BigDecimal("2"), now);
        insertAssemblyOrder(tenantId, 9841L, "ASM-TEST-001", "ASSEMBLE", now, true, productId, warehouseId, null, new BigDecimal("4"));
        insertAssemblyOrderItem(tenantId, 9842L, 9841L, productId, warehouseId, null, new BigDecimal("4"), now);

        TenantContext.setTenantId(tenantId);

        List<ErpStockOccupancyView> items = erpStockService.listOccupancy(balanceId);

        assertThat(items).hasSize(3);
        assertThat(items)
            .extracting(ErpStockOccupancyView::docNo)
            .containsExactly("SO-TEST-001", "PR-TEST-001", "ASM-TEST-001");
        assertThat(items)
            .extracting(ErpStockOccupancyView::qty)
            .containsExactly(new BigDecimal("3.0000"), new BigDecimal("2.0000"), new BigDecimal("4.0000"));
    }

    private void clearTenantData(long tenantId) {
        jdbcTemplate.update("DELETE FROM erp_assembly_order_item WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_assembly_order WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_purchase_return_item WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_purchase_return WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_sale_order_item WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_sale_order WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM erp_stock_balance WHERE tenant_id = ?", tenantId);
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

    private void insertBalance(long balanceId, long tenantId, long productId, long warehouseId, BigDecimal onHand, BigDecimal reserved, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_stock_balance (
                id, tenant_id, product_id, warehouse_id, location_id, qty_on_hand, qty_reserved, updated_by, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            balanceId, tenantId, productId, warehouseId, null, onHand, reserved, "tester", ts(now)
        );
    }

    private void insertSaleOrder(long tenantId, long orderId, String orderNo, Instant now, boolean inventoryReserved) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_sale_order (
                id, tenant_id, order_no, status, order_at, inventory_reserved,
                paid_amount, discount_amount, total_amount, total_amount_excl_tax, total_tax_amount, total_amount_incl_tax,
                version, created_at, created_by, updated_at, updated_by
            ) VALUES (?, ?, ?, 'DRAFT', ?, ?, 0, 0, 0, 0, 0, 0, 0, ?, 'tester', ?, 'tester')
            """,
            orderId, tenantId, orderNo, ts(now), inventoryReserved, ts(now), ts(now)
        );
    }

    private void insertSaleOrderItem(long tenantId, long itemId, long orderId, long productId, long warehouseId, Long locationId, BigDecimal qty, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_sale_order_item (
                id, tenant_id, order_id, product_id, product_code, product_name, warehouse_id, location_id,
                qty, price, price_incl_tax, amount, amount_incl_tax, tax_rate, tax_amount, sort_no, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0, 0, 1, ?, ?)
            """,
            itemId, tenantId, orderId, productId, "P-" + productId, "测试商品", warehouseId, locationId, qty, ts(now), ts(now)
        );
    }

    private void insertPurchaseReturn(long tenantId, long returnId, String orderNo, Instant now, boolean inventoryReserved) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_purchase_return (
                id, tenant_id, order_no, status, return_type, order_at, inventory_reserved,
                paid_amount, discount_amount, total_amount, total_amount_excl_tax, total_tax_amount, total_amount_incl_tax,
                version, created_at, created_by, updated_at, updated_by
            ) VALUES (?, ?, ?, 'DRAFT', 'RETURN', ?, ?, 0, 0, 0, 0, 0, 0, 0, ?, 'tester', ?, 'tester')
            """,
            returnId, tenantId, orderNo, ts(now), inventoryReserved, ts(now), ts(now)
        );
    }

    private void insertPurchaseReturnItem(long tenantId, long itemId, long returnId, long productId, long warehouseId, Long locationId, BigDecimal qty, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_purchase_return_item (
                id, tenant_id, return_id, product_id, product_code, product_name, warehouse_id, location_id,
                qty, price, price_incl_tax, amount, amount_incl_tax, tax_rate, tax_amount, sort_no, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0, 0, 1, ?, ?)
            """,
            itemId, tenantId, returnId, productId, "P-" + productId, "测试商品", warehouseId, locationId, qty, ts(now), ts(now)
        );
    }

    private void insertAssemblyOrder(long tenantId, long orderId, String orderNo, String orderType, Instant now, boolean inventoryReserved, long finishedProductId, long warehouseId, Long locationId, BigDecimal finishedQty) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_assembly_order (
                id, tenant_id, order_no, order_type, status, order_at, finished_product_id, finished_qty,
                warehouse_id, location_id, labor_cost, total_cost, unit_cost, inventory_reserved, created_at, created_by, updated_at, updated_by
            ) VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, 'tester', ?, 'tester')
            """,
            orderId, tenantId, orderNo, orderType, ts(now), finishedProductId, finishedQty, warehouseId, locationId, inventoryReserved, ts(now), ts(now)
        );
    }

    private void insertAssemblyOrderItem(long tenantId, long itemId, long orderId, long productId, long warehouseId, Long locationId, BigDecimal qty, Instant now) {
        jdbcTemplate.update(
            """
            INSERT INTO erp_assembly_order_item (
                id, tenant_id, order_id, line_no, product_id, product_code, product_name,
                warehouse_id, location_id, qty, unit_cost, amount, created_at, updated_at
            ) VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?)
            """,
            itemId, tenantId, orderId, productId, "P-" + productId, "测试商品", warehouseId, locationId, qty, ts(now), ts(now)
        );
    }

    private Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
