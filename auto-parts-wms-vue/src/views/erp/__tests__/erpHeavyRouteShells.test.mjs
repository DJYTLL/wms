import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const routerPath = join(viewsRoot, '..', '..', 'router', 'index.ts');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readRouter = () => readFileSync(routerPath, 'utf8');

const shellViews = [
  'ErpPurchaseReturnDraft.vue',
  'ErpPurchaseReturnApproved.vue',
  'ErpStockManagementRoute.vue',
  'ErpStockTxnManagementRoute.vue',
  'ErpStockCountManagementRoute.vue',
  'ErpStockTransferManagementRoute.vue',
  'ErpStockInitManagementRoute.vue',
  'ErpStockWarningManagementRoute.vue'
];

test('heavy ERP route entries render an immediate async shell', () => {
  for (const viewName of shellViews) {
    const source = readView(viewName);
    assert.match(source, /AsyncErpRouteShell/, `${viewName} should use AsyncErpRouteShell`);
    assert.match(source, /loader=/, `${viewName} should pass an async loader`);
    assert.match(source, /title=/, `${viewName} should provide an immediate title`);
  }
});

test('async ERP route shell gives the placeholder a paint opportunity before loading the heavy page', () => {
  const source = readView('AsyncErpRouteShell.vue');

  assert.match(source, /waitForShellPaint/, 'shell should isolate the paint wait in a named helper');
  assert.match(source, /requestAnimationFrame/, 'browser paint should be scheduled with requestAnimationFrame');
  assert.match(source, /setTimeout/, 'non-browser or unsupported environments should still yield to the event loop');
  assert.match(
    source,
    /await\s+waitForShellPaint\(\)[\s\S]*?await\s+props\.loader\(\)/,
    'heavy page loader should not run until after the shell had a chance to paint'
  );
});

test('async ERP route shell cancels the heavy page load when fast navigation unmounts the shell first', () => {
  const source = readView('AsyncErpRouteShell.vue');

  assert.match(source, /onBeforeUnmount/, 'shell should observe route-entry disposal');
  assert.match(source, /isRouteShellUnmounted/, 'shell should keep an unmounted flag for rapid navigation');
  assert.match(
    source,
    /await\s+waitForShellPaint\(\)[\s\S]*?if\s*\(\s*isRouteShellUnmounted\s*\)[\s\S]*?return\s+waitForRouteShellDisposal\(\)[\s\S]*?await\s+props\.loader\(\)/,
    'old route shell should avoid starting the heavy loader after it has already been unmounted'
  );
});

test('router points heavy ERP list entries at lightweight route shells', () => {
  const routerSource = readRouter();

  [
    'ErpStockManagementRoute',
    'ErpStockTxnManagementRoute',
    'ErpStockCountManagementRoute',
    'ErpStockTransferManagementRoute',
    'ErpStockInitManagementRoute',
    'ErpStockWarningManagementRoute'
  ].forEach((routeView) => {
    assert.match(routerSource, new RegExp(`${routeView}\\.vue`), `${routeView} should be used by router`);
  });

  assert.doesNotMatch(routerSource, /path: 'erp\/stocks'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpStockManagement\.vue'\)/);
  assert.doesNotMatch(routerSource, /path: 'erp\/stock-txns'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpStockTxnManagement\.vue'\)/);
  assert.doesNotMatch(routerSource, /path: 'erp\/stock-warnings'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpStockWarningManagement\.vue'\)/);
});

test('purchase order routes render the real page directly to avoid shell replacement flicker', () => {
  const routerSource = readRouter();

  assert.match(routerSource, /path: 'erp\/purchase-orders\/draft'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpPurchaseOrderDraft\.vue'\)/);
  assert.match(routerSource, /path: 'erp\/purchase-orders\/approved'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpPurchaseOrderApproved\.vue'\)/);
  assert.doesNotMatch(routerSource, /title: 'ERP采购单（草稿）'/);
  assert.doesNotMatch(routerSource, /title: 'ERP采购单（已审核）'/);
  assert.match(routerSource, /title: '采购单（草稿）'[\s\S]*?titleKey: 'page\.erpPurchaseOrderDraft'/);
  assert.match(routerSource, /title: '采购单（已审核）'[\s\S]*?titleKey: 'page\.erpPurchaseOrderApproved'/);
});

test('sale order and sale return routes render real management pages directly', () => {
  const routerSource = readRouter();

  assert.match(routerSource, /import ErpSaleOrderDraftManagement from '\.\.\/views\/erp\/ErpSaleOrderDraftManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleOrderApprovedManagement from '\.\.\/views\/erp\/ErpSaleOrderApprovedManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleReturnDraftManagement from '\.\.\/views\/erp\/ErpSaleReturnDraftManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleReturnApprovedManagement from '\.\.\/views\/erp\/ErpSaleReturnApprovedManagement\.vue'/);
  assert.match(routerSource, /path: 'erp\/sale-orders\/draft'[\s\S]*?component: ErpSaleOrderDraftManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-orders\/approved'[\s\S]*?component: ErpSaleOrderApprovedManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-returns\/draft'[\s\S]*?component: ErpSaleReturnDraftManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-returns\/approved'[\s\S]*?component: ErpSaleReturnApprovedManagement,/);
  assert.doesNotMatch(routerSource, /component: ErpSaleOrderManagement/);
  assert.doesNotMatch(routerSource, /component: ErpSaleReturnManagement/);
});
