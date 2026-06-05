import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const srcRoot = join(__dirname, '..', '..');

const readSource = (relativePath) => readFileSync(join(srcRoot, relativePath), 'utf8');

test('ERP navigation performance trace instruments menu, router, request and sale order first paint stages', () => {
  const traceSource = readSource('utils/erpNavigationPerfTrace.ts');
  const layoutSource = readSource('layouts/MainLayout.vue');
  const routerSource = readSource('router/index.ts');
  const requestSource = readSource('utils/request.ts');
  const saleDraftSource = readSource('views/erp/ErpSaleOrderDraftManagement.vue');
  const saleApprovedSource = readSource('views/erp/ErpSaleOrderApprovedManagement.vue');
  const draftPanelSource = readSource('views/erp/SaleOrderDraftDeferredPanel.vue');
  const approvedPanelSource = readSource('views/erp/SaleOrderApprovedDeferredPanel.vue');

  assert.match(traceSource, /ERP_PERF_TRACE_STORAGE_KEY = 'erpPerfTrace'/);
  assert.match(traceSource, /export const markErpNavigationPerf/);
  assert.match(traceSource, /export const timeErpNavigationPerf/);
  assert.match(traceSource, /console\.table/);

  assert.match(layoutSource, /markErpNavigationPerf\('menu:click'/);
  assert.match(routerSource, /markErpNavigationPerf\('router:beforeEach'/);
  assert.match(routerSource, /markErpNavigationPerf\('router:afterEach'/);
  assert.match(requestSource, /markErpNavigationPerf\('request:start'/);
  assert.match(requestSource, /markErpNavigationPerf\('request:finish'/);

  for (const source of [saleDraftSource, saleApprovedSource]) {
    assert.match(source, /markErpNavigationPerf\('sale-order-list:setup'/);
    assert.match(source, /markErpNavigationPerf\('sale-order-list:mounted'/);
    assert.match(source, /markErpNavigationPerf\('sale-order-list:deferred-panel-request'/);
    assert.match(source, /markErpNavigationPerf\('sale-order-list:list-fetch-start'/);
    assert.match(source, /markErpNavigationPerf\('sale-order-list:list-fetch-end'/);
  }

  for (const source of [draftPanelSource, approvedPanelSource]) {
    assert.match(source, /markErpNavigationPerf\('sale-order-list:deferred-panel-setup'/);
    assert.match(source, /markErpNavigationPerf\('sale-order-list:deferred-panel-mounted'/);
  }
});
