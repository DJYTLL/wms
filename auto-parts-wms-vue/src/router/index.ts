import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 导入布局组件
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'

// --- 临时占位组件 ---
// 在实际开发中，请删除这里，并在 routes 中引用真实的 .vue 文件
// 这样做的目的是为了让你复制进去后，点击菜单不会报错，能看到效果
const TempComponent = (title: string) => ({
  template: `
    <div style="padding: 20px;">
      <h2>${title}</h2>
      <p>这是一个演示页面，用于测试路由跳转和多标签页功能。</p>
      <div style="margin-top: 20px; padding: 20px; background: #f9f9f9; border-radius: 8px;">
        当前路径: {{ $route.path }}
      </div>
    </div>
  `
})

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '登录' }
  },
  {
    path: '/erp/sale-orders/:id/print',
    name: 'erp-sale-order-print',
    component: () => import('../views/erp/ErpSaleOrderPrint.vue'),
    meta: { title: '销售单打印', permission: 'erp-sale:view' }
  },
  {
    path: '/erp/purchase-orders/:id/print',
    name: 'erp-purchase-order-print',
    component: () => import('../views/erp/ErpPurchaseOrderPrint.vue'),
    meta: { title: '采购单打印', permission: 'erp-purchase:view' }
  },
  {
    path: '/erp/sale-returns/:id/print',
    name: 'erp-sale-return-print',
    component: () => import('../views/erp/ErpSaleReturnPrint.vue'),
    meta: { title: '销售退货单打印', permission: 'erp-sale-return:view' }
  },
  {
    path: '/erp/purchase-returns/:id/print',
    name: 'erp-purchase-return-print',
    component: () => import('../views/erp/ErpPurchaseReturnPrint.vue'),
    meta: { title: '采购退货单打印', permission: 'erp-purchase-return:view' }
  },
  {
    path: '/erp/receipts/:id/print',
    name: 'erp-receipt-print',
    component: () => import('../views/erp/ErpReceiptPrint.vue'),
    meta: { title: '收款单打印', permission: 'erp-receipt:view' }
  },
  {
    path: '/erp/payments/:id/print',
    name: 'erp-payment-print',
    component: () => import('../views/erp/ErpPaymentPrint.vue'),
    meta: { title: '付款单打印', permission: 'erp-payment:view' }
  },
  {
    path: '/erp/ar/:id/print',
    name: 'erp-ar-print',
    component: () => import('../views/erp/ErpAccountsReceivablePrint.vue'),
    meta: { title: '应收单打印', permission: 'erp-ar:view' }
  },
  {
    path: '/erp/ap/:id/print',
    name: 'erp-ap-print',
    component: () => import('../views/erp/ErpAccountsPayablePrint.vue'),
    meta: { title: '应付单打印', permission: 'erp-ap:view' }
  },
  {
    path: '/erp/stock-counts/:id/print',
    name: 'erp-stock-count-print',
    component: () => import('../views/erp/ErpStockCountPrint.vue'),
    meta: { title: '库存调整打印', permission: 'erp-stock-count:view' }
  },
  {
    path: '/erp/stock-transfers/:id/print',
    name: 'erp-stock-transfer-print',
    component: () => import('../views/erp/ErpStockTransferPrint.vue'),
    meta: { title: '库存移库打印', permission: 'erp-stock-transfer:view' }
  },
  {
    path: '/erp/stock-inits/:id/print',
    name: 'erp-stock-init-print',
    component: () => import('../views/erp/ErpStockInitPrint.vue'),
    meta: { title: '初始库存打印', permission: 'erp-stock-init:view' }
  },
  {
    path: '/',
    component: MainLayout,
    // 所有在 MainLayout 内部展示的页面都放在这里
    children: [
      {
        path: '', // 默认首页
        name: 'dashboard',
        component: () => import('../views/HomeView.vue'), // 假设你有 HomeView
        meta: { title: '仪表盘' }
      },
      // --- 仓库管理 (对应 menuData) ---
      {
        path: 'inbound', // 完整路径 /inbound
        name: 'inbound',
        component: () => import('../views/warehouse/InboundManagement.vue'),
        meta: { title: '入库管理', permission: 'inbound:view' }
      },
      // --- 出库管理 (三级菜单) ---
      // 注意：虽然菜单是嵌套的，但路由我们可以把它扁平化处理，或者按照路径层级写
      {
        path: 'outbound/normal', // 完整路径 /outbound/normal
        name: 'outbound-normal',
        component: TempComponent('普通出库'),
        meta: { title: '普通出库', permission: 'outbound:view' }
      },
      {
        path: 'outbound/urgent', // 完整路径 /outbound/urgent
        name: 'outbound-urgent',
        component: TempComponent('加急出库'),
        meta: { title: '加急出库', permission: 'outbound:view' }
      },
      // --- 系统设置 ---
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/system/UserManagement.vue'),
        meta: { title: '用户管理', permission: 'user:view' }
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('../views/system/RoleManagement.vue'),
        meta: { title: '角色权限', permission: 'role:view' }
      },
      {
        path: 'permissions',
        name: 'permissions',
        component: () => import('../views/system/PermissionManagement.vue'),
        meta: { title: '权限管理', permission: 'role:view', role: 'super_admin' } // 权限定义仅超级管理员可用
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: () => import('../views/system/AuditLogManagement.vue'),
        meta: { title: '审计日志', permission: 'audit:view' }
      },
      {
        path: 'column-permissions',
        name: 'column-permissions',
        component: () => import('../views/system/ColumnPermissionManagement.vue'),
        meta: { title: '列权限配置', permission: 'column:role:manage' }
      },
      {
        path: 'menus',
        name: 'menus',
        component: () => import('../views/system/MenuManagement.vue'),
        meta: { title: '菜单管理', permission: 'tenant:view', role: 'super_admin' }
      },
      {
        path: 'system-config',
        name: 'system-config',
        component: () => import('../views/system/SystemConfigManagement.vue'),
        meta: { title: '系统配置', permission: 'system-config:view', role: 'super_admin' }
      },
      {
        path: 'tenants',
        name: 'tenants',
        component: () => import('../views/system/TenantManagement.vue'),
        meta: { title: '租户管理', permission: 'tenant:view' }
      },
      // --- 进销存 ---
      {
        path: 'erp/products',
        name: 'erp-products',
        component: () => import('../views/erp/ErpProductManagement.vue'),
        meta: { title: 'ERP商品管理', permission: 'erp-product:view' }
      },
      {
        path: 'erp/vehicle-fitments',
        name: 'erp-vehicle-fitments',
        component: () => import('../views/erp/ErpVehicleFitmentManagement.vue'),
        meta: { title: 'ERP车型适配管理', permission: 'erp-product-fitment:view' }
      },
      {
        path: 'erp/customers',
        name: 'erp-customers',
        component: () => import('../views/erp/ErpCustomerManagement.vue'),
        meta: { title: 'ERP客户管理', permission: 'erp-customer:view' }
      },
      {
        path: 'erp/customer-categories',
        name: 'erp-customer-categories',
        component: () => import('../views/erp/ErpCustomerCategoryManagement.vue'),
        meta: { title: 'ERP客户类别管理', permission: 'erp-customer-category:view' }
      },
      {
        path: 'erp/suppliers',
        name: 'erp-suppliers',
        component: () => import('../views/erp/ErpSupplierManagement.vue'),
        meta: { title: 'ERP供应商管理', permission: 'erp-supplier:view' }
      },
      {
        path: 'erp/warehouses',
        name: 'erp-warehouses',
        component: () => import('../views/erp/ErpWarehouseManagement.vue'),
        meta: { title: 'ERP仓库管理', permission: 'erp-warehouse:view' }
      },
      {
        path: 'erp/locations',
        name: 'erp-locations',
        component: () => import('../views/erp/ErpLocationManagement.vue'),
        meta: { title: 'ERP库位管理', permission: 'erp-location:view' }
      },
      {
        path: 'erp/categories',
        name: 'erp-categories',
        component: () => import('../views/erp/ErpCategoryManagement.vue'),
        meta: { title: 'ERP分类管理', permission: 'erp-category:view' }
      },
      {
        path: 'erp/units',
        name: 'erp-units',
        component: () => import('../views/erp/ErpUnitManagement.vue'),
        meta: { title: 'ERP单位管理', permission: 'erp-unit:view' }
      },
        {
          path: 'erp/settlement-methods',
          name: 'erp-settlement-methods',
          component: () => import('../views/erp/ErpSettlementMethodManagement.vue'),
          meta: { title: 'ERP结算方式管理', permission: 'erp-settlement-method:view' }
        },
        {
          path: 'erp/payment-methods',
          name: 'erp-payment-methods',
          component: () => import('../views/erp/ErpPaymentMethodManagement.vue'),
          meta: { title: 'ERP付款方式管理', permission: 'erp-payment-method:view' }
        },
        {
          path: 'erp/receipt-methods',
          name: 'erp-receipt-methods',
          component: () => import('../views/erp/ErpReceiptMethodManagement.vue'),
          meta: { title: 'ERP收款方式管理', permission: 'erp-receipt-method:view' }
        },
      {
        path: 'erp/delivery-methods',
        name: 'erp-delivery-methods',
        component: () => import('../views/erp/ErpDeliveryMethodManagement.vue'),
        meta: { title: 'ERP送货方式管理', permission: 'erp-delivery-method:view' }
      },
      {
        path: 'erp/print-templates',
        name: 'erp-print-templates',
        component: () => import('../views/erp/ErpPrintTemplateManagement.vue'),
        meta: { title: '打印模板', permission: 'erp-print-template:view', titleKey: 'page.erpPrintTemplateManagement' }
      },
        {
          path: 'erp/purchase-orders/draft',
          name: 'erp-purchase-draft',
          component: () => import('../views/erp/ErpPurchaseOrderDraft.vue'),
          meta: { title: 'ERP采购单（草稿）', permission: 'erp-purchase:view', titleKey: 'page.erpPurchaseOrderDraft' }
        },
        {
          path: 'erp/purchase-orders/create',
          name: 'erp-purchase-create',
          component: () => import('../views/erp/ErpPurchaseOrderForm.vue'),
          meta: { title: 'ERP采购单新增', permission: 'erp-purchase:add', titleKey: 'page.erpPurchaseOrderCreate' }
        },
        {
          path: 'erp/purchase-orders/:id/edit',
          name: 'erp-purchase-edit',
          component: () => import('../views/erp/ErpPurchaseOrderForm.vue'),
          meta: { title: 'ERP采购单编辑', permission: 'erp-purchase:view', titleKey: 'page.erpPurchaseOrderEdit' }
        },
        {
          path: 'erp/purchase-orders/approved',
          name: 'erp-purchase-approved',
          component: () => import('../views/erp/ErpPurchaseOrderApproved.vue'),
          meta: { title: 'ERP采购单（已审核）', permission: 'erp-purchase:view', titleKey: 'page.erpPurchaseOrderApproved' }
        },
        {
          path: 'erp/purchase-returns',
          redirect: '/erp/purchase-returns/draft'
        },
        {
          path: 'erp/purchase-returns/create',
          name: 'erp-purchase-returns-create',
          component: () => import('../views/erp/ErpPurchaseReturnForm.vue'),
          meta: { title: '新增采购退货单', permission: 'erp-purchase-return:add', titleKey: 'page.erpPurchaseReturnCreate' }
        },
        {
          path: 'erp/purchase-returns/:id/edit',
          name: 'erp-purchase-returns-edit',
          component: () => import('../views/erp/ErpPurchaseReturnForm.vue'),
          meta: { title: '编辑采购退货单', permission: 'erp-purchase-return:view', titleKey: 'page.erpPurchaseReturnEdit' }
        },
        {
          path: 'erp/purchase-returns/draft',
          name: 'erp-purchase-returns-draft',
          component: () => import('../views/erp/ErpPurchaseReturnManagement.vue'),
          meta: { title: '采购退货（草稿）', permission: 'erp-purchase-return:view', titleKey: 'page.erpPurchaseReturnDraft', defaultStatus: 'DRAFT', lockStatus: true }
        },
        {
          path: 'erp/purchase-returns/approved',
          name: 'erp-purchase-returns-approved',
          component: () => import('../views/erp/ErpPurchaseReturnManagement.vue'),
          meta: { title: '采购退货（已审核）', permission: 'erp-purchase-return:view', titleKey: 'page.erpPurchaseReturnApproved', defaultStatus: 'APPROVED', lockStatus: true }
        },
      {
        path: 'erp/sale-orders',
        redirect: '/erp/sale-orders/draft'
      },
      {
        path: 'erp/sale-orders/create',
        name: 'erp-sale-orders-create',
        component: () => import('../views/erp/ErpSaleOrderForm.vue'),
        meta: { title: '新增销售单', permission: 'erp-sale:add', titleKey: 'page.erpSaleOrderCreate' }
      },
      {
        path: 'erp/sale-orders/create-preview',
        name: 'erp-sale-orders-create-preview',
        component: () => import('../views/erp/ErpSaleOrderFormPreview.vue'),
        meta: { title: '新增销售单(预览)', permission: 'erp-sale:add', titleKey: 'page.erpSaleOrderCreatePreview' }
      },
      {
        path: 'erp/sale-orders/create-preview-alt',
        name: 'erp-sale-orders-create-preview-alt',
        component: () => import('../views/erp/ErpSaleOrderFormPreviewAlt.vue'),
        meta: { title: '新增销售单(预览替代)', permission: 'erp-sale:add', titleKey: 'page.erpSaleOrderCreatePreviewAlt' }
      },
      {
        path: 'erp/sale-orders/create-preview-paper',
        name: 'erp-sale-orders-create-preview-paper',
        component: () => import('../views/erp/ErpSaleOrderFormPreviewPaper.vue'),
        meta: { title: '新增销售单(纸质风格)', permission: 'erp-sale:add', titleKey: 'page.erpSaleOrderCreatePreviewPaper' }
      },
      {
        path: 'erp/sale-orders/:id/edit',
        name: 'erp-sale-orders-edit',
        component: () => import('../views/erp/ErpSaleOrderForm.vue'),
        meta: { title: '编辑销售单', permission: 'erp-sale:edit', titleKey: 'page.erpSaleOrderEdit' }
      },
      {
        path: 'erp/sale-orders/draft',
        name: 'erp-sale-orders-draft',
        component: () => import('../views/erp/ErpSaleOrderManagement.vue'),
        meta: { title: '销售单（草稿）', permission: 'erp-sale:view', titleKey: 'page.erpSaleOrderDraft', defaultStatus: 'DRAFT', lockStatus: true }
      },
      {
        path: 'erp/sale-orders/approved',
        name: 'erp-sale-orders-approved',
        component: () => import('../views/erp/ErpSaleOrderManagement.vue'),
        meta: { title: '销售单（已审核）', permission: 'erp-sale:view', titleKey: 'page.erpSaleOrderApproved', defaultStatus: 'APPROVED', lockStatus: true }
      },
      {
        path: 'erp/sale-returns',
        redirect: '/erp/sale-returns/draft'
      },
      {
        path: 'erp/sale-returns/create',
        name: 'erp-sale-returns-create',
        component: () => import('../views/erp/ErpSaleReturnForm.vue'),
        meta: { title: '新增销售退货单', permission: 'erp-sale-return:add', titleKey: 'page.erpSaleReturnCreate' }
      },
      {
        path: 'erp/sale-returns/:id/edit',
        name: 'erp-sale-returns-edit',
        component: () => import('../views/erp/ErpSaleReturnForm.vue'),
        meta: { title: '编辑销售退货单', permission: 'erp-sale-return:view', titleKey: 'page.erpSaleReturnEdit' }
      },
      {
        path: 'erp/sale-returns/draft',
        name: 'erp-sale-returns-draft',
        component: () => import('../views/erp/ErpSaleReturnManagement.vue'),
        meta: { title: '销售退货（草稿）', permission: 'erp-sale-return:view', titleKey: 'page.erpSaleReturnDraft', defaultStatus: 'DRAFT', lockStatus: true }
      },
      {
        path: 'erp/sale-returns/approved',
        name: 'erp-sale-returns-approved',
        component: () => import('../views/erp/ErpSaleReturnManagement.vue'),
        meta: { title: '销售退货（已审核）', permission: 'erp-sale-return:view', titleKey: 'page.erpSaleReturnApproved', defaultStatus: 'APPROVED', lockStatus: true }
      },
      {
        path: 'erp/stocks',
        name: 'erp-stocks',
        component: () => import('../views/erp/ErpStockManagement.vue'),
        meta: { title: 'ERP库存台账', permission: 'erp-stock:view' }
      },
      {
        path: 'erp/stock-txns',
        name: 'erp-stock-txns',
        component: () => import('../views/erp/ErpStockTxnManagement.vue'),
        meta: { title: 'ERP库存流水', permission: 'erp-stock-txn:view' }
      },
      {
        path: 'erp/stock-counts',
        name: 'erp-stock-counts',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '库存调整', permission: 'erp-stock-count:view', titleKey: 'page.erpStockCountManagement', countType: 'COUNT' }
      },
      {
        path: 'erp/stock-counts/create',
        name: 'erp-stock-count-create',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '新增库存调整', permission: 'erp-stock-count:add', titleKey: 'page.erpStockCountManagement', countType: 'COUNT', pageMode: 'form', formMode: 'create' }
      },
      {
        path: 'erp/stock-counts/:id/edit',
        name: 'erp-stock-count-edit',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '编辑库存调整', permission: 'erp-stock-count:edit', titleKey: 'page.erpStockCountManagement', countType: 'COUNT', pageMode: 'form', formMode: 'edit' }
      },
      {
        path: 'erp/stock-counts/:id',
        name: 'erp-stock-count-detail',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '查看库存调整', permission: 'erp-stock-count:view', titleKey: 'page.erpStockCountManagement', countType: 'COUNT', pageMode: 'form', formMode: 'view' }
      },
      {
        path: 'erp/stock-transfers',
        name: 'erp-stock-transfers',
        component: () => import('../views/erp/ErpStockTransferManagement.vue'),
        meta: { title: '库存移库', permission: 'erp-stock-transfer:view', titleKey: 'page.erpStockTransferManagement' }
      },
      {
        path: 'erp/stock-transfers/create',
        name: 'erp-stock-transfer-create',
        component: () => import('../views/erp/ErpStockTransferManagement.vue'),
        meta: { title: '新增库存移库', permission: 'erp-stock-transfer:add', titleKey: 'page.erpStockTransferManagement', pageMode: 'form', formMode: 'create' }
      },
      {
        path: 'erp/stock-transfers/:id/edit',
        name: 'erp-stock-transfer-edit',
        component: () => import('../views/erp/ErpStockTransferManagement.vue'),
        meta: { title: '编辑库存移库', permission: 'erp-stock-transfer:edit', titleKey: 'page.erpStockTransferManagement', pageMode: 'form', formMode: 'edit' }
      },
      {
        path: 'erp/stock-transfers/:id',
        name: 'erp-stock-transfer-detail',
        component: () => import('../views/erp/ErpStockTransferManagement.vue'),
        meta: { title: '查看库存移库', permission: 'erp-stock-transfer:view', titleKey: 'page.erpStockTransferManagement', pageMode: 'form', formMode: 'view' }
      },
      {
        path: 'erp/stock-inits',
        name: 'erp-stock-inits',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '初始库存', permission: 'erp-stock-init:view', titleKey: 'page.erpStockInitManagement', countType: 'INIT' }
      },
      {
        path: 'erp/stock-inits/create',
        name: 'erp-stock-init-create',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '新增初始库存', permission: 'erp-stock-init:add', titleKey: 'page.erpStockInitManagement', countType: 'INIT', pageMode: 'form', formMode: 'create' }
      },
      {
        path: 'erp/stock-inits/:id/edit',
        name: 'erp-stock-init-edit',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '编辑初始库存', permission: 'erp-stock-init:edit', titleKey: 'page.erpStockInitManagement', countType: 'INIT', pageMode: 'form', formMode: 'edit' }
      },
      {
        path: 'erp/stock-inits/:id',
        name: 'erp-stock-init-detail',
        component: () => import('../views/erp/ErpStockCountManagement.vue'),
        meta: { title: '查看初始库存', permission: 'erp-stock-init:view', titleKey: 'page.erpStockInitManagement', countType: 'INIT', pageMode: 'form', formMode: 'view' }
      },
      {
        path: 'erp/stock-warnings',
        name: 'erp-stock-warnings',
        component: () => import('../views/erp/ErpStockWarningManagement.vue'),
        meta: { title: '库存预警', permission: 'erp-stock-warning:view', titleKey: 'page.erpStockWarningManagement' }
      },
      {
        path: 'erp/assemble-orders',
        name: 'erp-assemble-orders',
        component: () => import('../views/erp/ErpAssemblyOrderManagement.vue'),
        meta: { title: '组装单', permission: 'erp-assembly:view', titleKey: 'page.erpAssembleOrderManagement' }
      },
      {
        path: 'erp/assemble-orders/create',
        name: 'erp-assemble-order-create',
        component: () => import('../views/erp/ErpAssemblyOrderForm.vue'),
        meta: { title: '新增组装单', permission: 'erp-assembly:add', titleKey: 'page.erpAssembleOrderCreate' }
      },
      {
        path: 'erp/assemble-orders/:id/edit',
        name: 'erp-assemble-order-edit',
        component: () => import('../views/erp/ErpAssemblyOrderForm.vue'),
        meta: { title: '编辑组装单', permission: 'erp-assembly:edit', titleKey: 'page.erpAssembleOrderEdit' }
      },
      {
        path: 'erp/assemble-orders/:id/view',
        name: 'erp-assemble-order-view',
        component: () => import('../views/erp/ErpAssemblyOrderForm.vue'),
        meta: { title: '查看组装单', permission: 'erp-assembly:view', titleKey: 'page.erpAssembleOrderEdit' }
      },
      {
        path: 'erp/disassemble-orders',
        name: 'erp-disassemble-orders',
        component: () => import('../views/erp/ErpDisassembleOrderManagement.vue'),
        meta: { title: '拆分单', permission: 'erp-assembly:view', titleKey: 'page.erpDisassembleOrderManagement' }
      },
      {
        path: 'erp/disassemble-orders/create',
        name: 'erp-disassemble-order-create',
        component: () => import('../views/erp/ErpDisassembleOrderForm.vue'),
        meta: { title: '新增拆分单', permission: 'erp-assembly:add', titleKey: 'page.erpDisassembleOrderCreate' }
      },
      {
        path: 'erp/disassemble-orders/:id/edit',
        name: 'erp-disassemble-order-edit',
        component: () => import('../views/erp/ErpDisassembleOrderForm.vue'),
        meta: { title: '编辑拆分单', permission: 'erp-assembly:edit', titleKey: 'page.erpDisassembleOrderEdit' }
      },
      {
        path: 'erp/disassemble-orders/:id/view',
        name: 'erp-disassemble-order-view',
        component: () => import('../views/erp/ErpDisassembleOrderForm.vue'),
        meta: { title: '查看拆分单', permission: 'erp-assembly:view', titleKey: 'page.erpDisassembleOrderEdit' }
      },
      {
        path: 'erp/assemblies',
        redirect: 'erp/assemble-orders'
      },
      {
        path: 'erp/assemblies/create',
        redirect: 'erp/assemble-orders/create'
      },
      {
        path: 'erp/assemblies/:id/edit',
        redirect: to => ({
          path: `/erp/assemble-orders/${to.params.id}/edit`,
          query: to.query
        })
      },
      {
        path: 'erp/ar',
        name: 'erp-ar',
        component: () => import('../views/erp/ErpAccountsReceivableManagement.vue'),
        meta: { title: '应收管理', permission: 'erp-ar:view', titleKey: 'page.erpAccountsReceivableManagement' }
      },
      {
        path: 'erp/finance/customer-debts',
        name: 'erp-finance-customer-debt',
        component: () => import('../views/erp/ErpCustomerDebtManagement.vue'),
        meta: { title: '客户欠款', permission: 'erp-finance-customer-debt:view', titleKey: 'page.erpCustomerDebtManagement' }
      },
      {
        path: 'erp/finance/supplier-debts',
        name: 'erp-finance-supplier-debt',
        component: () => import('../views/erp/ErpSupplierDebtManagement.vue'),
        meta: { title: '供应商欠款', permission: 'erp-finance-supplier-debt:view', titleKey: 'page.erpSupplierDebtManagement' }
      },
      {
        path: 'erp/ar/:id',
        name: 'erp-ar-detail',
        component: () => import('../views/erp/ErpAccountsReceivableDetail.vue'),
        meta: { title: '应收详情', permission: 'erp-ar:view', titleKey: 'page.erpAccountsReceivableDetail' }
      },
      {
        path: 'erp/ap',
        name: 'erp-ap',
        component: () => import('../views/erp/ErpAccountsPayableManagement.vue'),
        meta: { title: '应付管理', permission: 'erp-ap:view', titleKey: 'page.erpAccountsPayableManagement' }
      },
      {
        path: 'erp/ap/:id',
        name: 'erp-ap-detail',
        component: () => import('../views/erp/ErpAccountsPayableDetail.vue'),
        meta: { title: '应付详情', permission: 'erp-ap:view', titleKey: 'page.erpAccountsPayableDetail' }
      },
      {
        path: 'erp/receipts',
        name: 'erp-receipts',
        component: () => import('../views/erp/ErpReceiptManagement.vue'),
        meta: { title: '收款单', permission: 'erp-receipt:view', titleKey: 'page.erpReceiptManagement' }
      },
      {
        path: 'erp/receipts/create',
        name: 'erp-receipts-create',
        component: () => import('../views/erp/ErpReceiptForm.vue'),
        meta: { title: '新增收款单', permission: 'erp-receipt:add', titleKey: 'page.erpReceiptCreate' }
      },
      {
        path: 'erp/receipts/:id/edit',
        name: 'erp-receipts-edit',
        component: () => import('../views/erp/ErpReceiptForm.vue'),
        meta: { title: '编辑收款单', permission: 'erp-receipt:add', titleKey: 'page.erpReceiptEdit' }
      },
      {
        path: 'erp/receipts/:id',
        name: 'erp-receipts-detail',
        component: () => import('../views/erp/ErpReceiptDetail.vue'),
        meta: { title: '收款单详情', permission: 'erp-receipt:view', titleKey: 'page.erpReceiptDetail' }
      },
      {
        path: 'erp/payments',
        name: 'erp-payments',
        component: () => import('../views/erp/ErpPaymentManagement.vue'),
        meta: { title: '付款单', permission: 'erp-payment:view', titleKey: 'page.erpPaymentManagement' }
      },
      {
        path: 'erp/payments/create',
        name: 'erp-payments-create',
        component: () => import('../views/erp/ErpPaymentForm.vue'),
        meta: { title: '新增付款单', permission: 'erp-payment:add', titleKey: 'page.erpPaymentCreate' }
      },
      {
        path: 'erp/payments/:id/edit',
        name: 'erp-payments-edit',
        component: () => import('../views/erp/ErpPaymentForm.vue'),
        meta: { title: '编辑付款单', permission: 'erp-payment:add', titleKey: 'page.erpPaymentEdit' }
      },
      {
        path: 'erp/payments/:id',
        name: 'erp-payments-detail',
        component: () => import('../views/erp/ErpPaymentDetail.vue'),
        meta: { title: '付款单详情', permission: 'erp-payment:view', titleKey: 'page.erpPaymentDetail' }
      },
      {
        path: 'about',
        name: 'about',
        component: () => import('../views/AboutView.vue'),
        meta: { title: '关于系统' }
      }
    ]
  },
  // 404 处理
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// --- 路由守卫 ---
/**
 * 全局导航守卫
 * 
 * 在每次路由跳转之前检查认证状态。
 * 如果用户未认证 (store 中没有 token) 并且尝试访问受保护的路由 (除 /login 以外的任何路由)，
 *  mereka 将被重定向到 /login。
 */
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  // 设置网页标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 汽配仓储系统`;
  } else {
    document.title = '汽配仓储系统';
  }

  if (to.name !== 'login' && !authStore.initialized) {
    await authStore.restoreSession();
  }

  // 1. 认证拦截
  if (to.name !== 'login' && !authStore.isAuthenticated) {
    next({ name: 'login' });
    return;
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    next({ path: '/' });
    return;
  }

  // 2. 角色拦截
  if (to.meta.role && !authStore.hasRole(to.meta.role as string)) {
    if (to.path !== '/') {
      console.warn(`Access denied to ${to.path}. Missing role: ${to.meta.role}`);
      next({ path: '/' });
      return;
    }
  }

  // 3. 权限拦截
  if (to.meta.permission && !authStore.hasPermission(to.meta.permission as string)) {
    // 如果没有权限，重定向到首页（Dashboard通常是所有人可见，或者至少是最安全的默认页）
    // 为了防止无限循环（如果用户连 dashboard 权限都没有），这里可以加一个判断
    if (to.path !== '/') {
      console.warn(`Access denied to ${to.path}. Missing permission: ${to.meta.permission}`);
      // 简单提示
      if (typeof window !== 'undefined') {
        // 避免在服务端渲染等情况报错，虽然这里是SPA
        // alert('Access Denied'); // 可选：太打扰用户，通常用 Toast 或直接跳转
      }
      next({ path: '/' });
      return;
    }
  }

  next();
});

export default router
