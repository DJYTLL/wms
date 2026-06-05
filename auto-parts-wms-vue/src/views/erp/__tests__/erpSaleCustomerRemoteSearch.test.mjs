import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

test('sale list pages search customers remotely instead of loading all customers on first paint', () => {
  const customerSearchSource = readView('useSaleListCustomerSearch.ts');
  const entries = [
    'ErpSaleOrderDraftManagement.vue',
    'ErpSaleOrderApprovedManagement.vue',
    'ErpSaleReturnDraftManagement.vue',
    'ErpSaleReturnApprovedManagement.vue',
  ];

  for (const entryName of entries) {
    const source = readView(entryName);
    const runRouteRefreshBlock = source.match(/const runRouteRefresh = \(\) => \{[\s\S]*?\n\};/)?.[0] ?? '';

    assert.doesNotMatch(source, /getCachedCustomers/);
    assert.doesNotMatch(source, /const FuzzyProductSelect = defineAsyncComponent/);
    assert.doesNotMatch(source, /<FuzzyProductSelect/);
    assert.doesNotMatch(runRouteRefreshBlock, /\bfetchCustomers\(\);/);
    assert.match(source, /<el-select[\s\S]*v-model="customerFilter"[\s\S]*filterable[\s\S]*remote/);
    assert.match(source, /:remote-method="searchCustomers"/);
    assert.match(source, /:loading="customerSearchLoading"/);
    assert.match(source, /:automatic-dropdown="false"/);
    assert.match(source, /useSaleListCustomerSearch\(notifyError\)/);
  }

  assert.match(customerSearchSource, /request\.get\('\/erp\/customers\/search'/);
  assert.match(customerSearchSource, /size:\s*20/);
  assert.doesNotMatch(customerSearchSource, /enabled:\s*true/);
});

test('sale list customer search waits for input and keeps selected label available', () => {
  const customerSearchSource = readView('useSaleListCustomerSearch.ts');

  assert.match(customerSearchSource, /const normalizedKeyword = keyword\.trim\(\);/);
  assert.match(customerSearchSource, /if \(!normalizedKeyword\) \{/);
  assert.match(customerSearchSource, /customerOptions\.value = customerFilter\.value\s*\? keepSelectedCustomerOption\(customerOptions\.value\)\s*:\s*\[\];/);
  assert.match(customerSearchSource, /return;/);
  assert.match(customerSearchSource, /const keepSelectedCustomerOption = \(options: SaleListCustomerOption\[\]\) =>/);
});

test('sale list tables prefer row customer name after removing full customer dictionary', () => {
  const entries = [
    ['ErpSaleOrderDraftManagement.vue', 'SaleOrderDraftDeferredPanel.vue'],
    ['ErpSaleOrderApprovedManagement.vue', 'SaleOrderApprovedDeferredPanel.vue'],
    ['ErpSaleReturnDraftManagement.vue', 'SaleReturnDraftDeferredPanel.vue'],
    ['ErpSaleReturnApprovedManagement.vue', 'SaleReturnApprovedDeferredPanel.vue'],
  ];

  for (const [entryName, panelName] of entries) {
    const entrySource = readView(entryName);
    const panelSource = readView(panelName);

    assert.match(entrySource, /customerName\?: string;/);
    assert.match(entrySource, /const getCustomerName = \(id\?: number, name\?: string\) => name \|\| customerOptions\.value\.find/);
    assert.match(panelSource, /customerName\?: string;/);
    assert.match(panelSource, /getCustomerName\(row\.customerId, row\.customerName\)/);
  }
});
