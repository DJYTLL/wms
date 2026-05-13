package com.example.wms.config;

import java.util.List;

// 菜单种子提供器
public final class MenuSeedProvider {
    private MenuSeedProvider() {
    }

    public static List<MenuSeed> menuSeeds() {
        return List.of(
            new MenuSeed("dashboard", null, "仪表盘", "dashboard", "/", ICON_DASHBOARD, null, 10),

            new MenuSeed("warehouse", null, "仓库管理", "warehouse", null, ICON_WAREHOUSE, "warehouse:view", 20),
            new MenuSeed("inbound", "warehouse", "入库管理", "inbound", "/inbound", null, "inbound:view", 10),
            new MenuSeed("outbound", "warehouse", "出库管理", "outbound", null, null, "outbound:view", 20),
            new MenuSeed("out-normal", "outbound", "普通出库", "out-normal", "/outbound/normal", null, "outbound:view", 10),
            new MenuSeed("out-urgent", "outbound", "加急出库", "out-urgent", "/outbound/urgent", null, "outbound:view", 20),

            new MenuSeed("erp", null, "进销存", "erp", null, ICON_WAREHOUSE, null, 35),
            new MenuSeed("erp-basic", "erp", "基础资料", "erp-basic", null, ICON_BASIC, null, 10),
            new MenuSeed("erp-product", "erp-basic", "商品管理", "erp-product", "/erp/products", null, "erp-product:view", 10),
            new MenuSeed("erp-vehicle-fitment", "erp-basic", "车型适配管理", "erp-vehicle-fitment", "/erp/vehicle-fitments", null, "erp-product-fitment:view", 15),
            new MenuSeed("erp-customer", "erp-basic", "客户管理", "erp-customer", "/erp/customers", null, "erp-customer:view", 20),
            new MenuSeed("erp-customer-category", "erp-basic", "客户类别", "erp-customer-category", "/erp/customer-categories", null, "erp-customer-category:view", 25),
            new MenuSeed("erp-supplier", "erp-basic", "供应商管理", "erp-supplier", "/erp/suppliers", null, "erp-supplier:view", 30),
            new MenuSeed("erp-warehouse", "erp-basic", "仓库管理", "erp-warehouse", "/erp/warehouses", null, "erp-warehouse:view", 40),
            new MenuSeed("erp-location", "erp-basic", "库位管理", "erp-location", "/erp/locations", null, "erp-location:view", 50),
            new MenuSeed("erp-category", "erp-basic", "分类管理", "erp-category", "/erp/categories", null, "erp-category:view", 60),
            new MenuSeed("erp-unit", "erp-basic", "单位管理", "erp-unit", "/erp/units", null, "erp-unit:view", 70),
            new MenuSeed("erp-settlement-method", "erp-basic", "结算方式", "erp-settlement-method", "/erp/settlement-methods", null, "erp-settlement-method:view", 80),
            new MenuSeed("erp-payment-method", "erp-basic", "付款方式", "erp-payment-method", "/erp/payment-methods", null, "erp-payment-method:view", 85),
            new MenuSeed("erp-delivery-method", "erp-basic", "送货方式", "erp-delivery-method", "/erp/delivery-methods", null, "erp-delivery-method:view", 90),
            new MenuSeed("erp-print-template", "erp-basic", "打印模板", "erp-print-template", "/erp/print-templates", null, "erp-print-template:view", 95),

            new MenuSeed("erp-purchase", "erp", "采购管理", "erp-purchase", null, null, null, 20),
            new MenuSeed("erp-purchase-draft", "erp-purchase", "采购单（草稿）", "erp-purchase-draft", "/erp/purchase-orders/draft", null, "erp-purchase:view", 10),
            new MenuSeed("erp-purchase-approved", "erp-purchase", "采购单（已审核）", "erp-purchase-approved", "/erp/purchase-orders/approved", null, "erp-purchase:view", 20),
            new MenuSeed("erp-purchase-return-draft", "erp-purchase", "采购退货（草稿）", "erp-purchase-return-draft", "/erp/purchase-returns/draft", null, "erp-purchase-return:view", 30),
            new MenuSeed("erp-purchase-return-approved", "erp-purchase", "采购退货（已审核）", "erp-purchase-return-approved", "/erp/purchase-returns/approved", null, "erp-purchase-return:view", 40),

            new MenuSeed("erp-sale", "erp", "销售管理", "erp-sale", null, null, null, 30),
            new MenuSeed("erp-sale-draft", "erp-sale", "销售单（草稿）", "erp-sale-draft", "/erp/sale-orders/draft", null, "erp-sale:view", 10),
            new MenuSeed("erp-sale-approved", "erp-sale", "销售单（已审核）", "erp-sale-approved", "/erp/sale-orders/approved", null, "erp-sale:view", 20),
            new MenuSeed("erp-sale-return-draft", "erp-sale", "销售退货（草稿）", "erp-sale-return-draft", "/erp/sale-returns/draft", null, "erp-sale-return:view", 30),
            new MenuSeed("erp-sale-return-approved", "erp-sale", "销售退货（已审核）", "erp-sale-return-approved", "/erp/sale-returns/approved", null, "erp-sale-return:view", 40),

            new MenuSeed("erp-warehouse-module", "erp", "仓库管理", "erp-warehouse-module", null, null, null, 40),
            new MenuSeed("erp-stock", "erp-warehouse-module", "库存台账", "erp-stock", "/erp/stocks", null, "erp-stock:view", 10),
            new MenuSeed("erp-stock-warning", "erp-warehouse-module", "库存预警", "erp-stock-warning", "/erp/stock-warnings", null, "erp-stock-warning:view", 15),
            new MenuSeed("erp-stock-txn", "erp-warehouse-module", "库存流水", "erp-stock-txn", "/erp/stock-txns", null, "erp-stock-txn:view", 20),
            new MenuSeed("erp-assemble-order", "erp-warehouse-module", "组装单", "erp-assemble-order", "/erp/assemble-orders", null, "erp-assembly:view", 25),
            new MenuSeed("erp-disassemble-order", "erp-warehouse-module", "拆分单", "erp-disassemble-order", "/erp/disassemble-orders", null, "erp-assembly:view", 26),
            new MenuSeed("erp-stock-count", "erp-warehouse-module", "库存调整", "erp-stock-count", "/erp/stock-counts", null, "erp-stock-count:view", 30),
            new MenuSeed("erp-stock-init", "erp-warehouse-module", "初始库存", "erp-stock-init", "/erp/stock-inits", null, "erp-stock-init:view", 40),

            new MenuSeed("erp-finance", "erp", "财务管理", "erp-finance", null, null, null, 50),
            new MenuSeed("erp-finance-summary", "erp-finance", "客户欠款", "erp-finance-customer-debt", "/erp/finance/customer-debts", null, "erp-finance-customer-debt:view", 5),
            new MenuSeed("erp-finance-supplier-debt", "erp-finance", "供应商欠款", "erp-finance-supplier-debt", "/erp/finance/supplier-debts", null, "erp-finance-supplier-debt:view", 10),
            new MenuSeed("erp-ar", "erp-finance", "应收管理", "erp-ar", "/erp/ar", null, "erp-ar:view", 20),
            new MenuSeed("erp-ap", "erp-finance", "应付管理", "erp-ap", "/erp/ap", null, "erp-ap:view", 30),
            new MenuSeed("erp-receipt", "erp-finance", "收款单", "erp-receipt", "/erp/receipts", null, "erp-receipt:view", 40),
            new MenuSeed("erp-payment", "erp-finance", "付款单", "erp-payment", "/erp/payments", null, "erp-payment:view", 50),

            new MenuSeed("system", null, "系统设置", "system", null, ICON_SYSTEM, null, 40),
            new MenuSeed("users", "system", "用户管理", "users", "/users", null, "user:view", 10),
            new MenuSeed("roles", "system", "角色权限", "roles", "/roles", null, "role:view", 20),
            new MenuSeed("permissions", "system", "权限管理", "permissions", "/permissions", null, "role:view", 30),
            new MenuSeed("audit-logs", "system", "审计日志", "audit-logs", "/audit-logs", null, "audit:view", 35),
            new MenuSeed("column-permissions", "system", "列权限配置", "columnPermissions", "/column-permissions", null, "column:role:manage", 36),
            new MenuSeed("menu-management", "system", "菜单管理", "menu-management", "/menus", null, "tenant:view", 36),
            new MenuSeed("system-config", "system", "系统配置", "system-config", "/system-config", null, "tenant:view", 38),
            new MenuSeed("tenants", "system", "租户管理", "tenants", "/tenants", null, "tenant:view", 40)
        );
    }

    public record MenuSeed(String code,
                           String parentCode,
                           String title,
                           String i18nKey,
                           String path,
                           String icon,
                           String permissionCode,
                           int sort) {
    }

    private static final String ICON_DASHBOARD = """
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="7" height="7"></rect>
          <rect x="14" y="3" width="7" height="7"></rect>
          <rect x="14" y="14" width="7" height="7"></rect>
          <rect x="3" y="14" width="7" height="7"></rect>
        </svg>
        """;

    private static final String ICON_WAREHOUSE = """
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
          <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
          <line x1="12" y1="22.08" x2="12" y2="12"></line>
        </svg>
        """;

    private static final String ICON_BASIC = """
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 7V4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3"/>
          <path d="M4 12h16"/>
          <path d="M4 17v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3"/>
          <rect x="4" y="7" width="16" height="10" rx="2"/>
        </svg>
        """;

    private static final String ICON_SYSTEM = """
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
        """;
}
