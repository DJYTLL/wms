import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readSaleOrderModule = (relativePath) => readFileSync(join(viewsRoot, 'sale-order', relativePath), 'utf8');

test('sale order form searches products remotely instead of loading all products on first paint', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const detailTableSource = readSaleOrderModule('SaleOrderDetailTable.vue');
  const productSelectionSource = readSaleOrderModule('useSaleOrderProductSelection.ts');
  const mountedBlock = componentSource.match(/onMounted\(\(\) => \{[\s\S]*?\n\}\);/)?.[0] ?? '';

  assert.doesNotMatch(mountedBlock, /\bfetchProducts\(\);/);
  assert.match(componentSource, /:search-products="searchProducts"/);
  assert.match(componentSource, /:product-search-loading="productSearchLoading"/);
  assert.match(detailTableSource, /remote/);
  assert.match(detailTableSource, /:remote-method="searchProducts"/);
  assert.match(detailTableSource, /:loading="productSearchLoading"/);
  assert.match(detailTableSource, /:automatic-dropdown="false"/);
  assert.match(productSelectionSource, /request\.get\('\/erp\/products\/page'/);
  assert.match(productSelectionSource, /size:\s*20/);
});

test('sale order product dropdown waits for input before searching and showing options', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const detailTableSource = readSaleOrderModule('SaleOrderDetailTable.vue');
  const productSelectionSource = readSaleOrderModule('useSaleOrderProductSelection.ts');

  assert.match(productSelectionSource, /const normalizedKeyword = keyword\.trim\(\);/);
  assert.match(productSelectionSource, /if \(!normalizedKeyword\) \{/);
  assert.match(productSelectionSource, /productSearchOptions\.value = \[\];/);
  assert.match(productSelectionSource, /return;/);
  assert.doesNotMatch(detailTableSource, /@visible-change="handleProductDropdownVisibleChange"/);
  assert.doesNotMatch(componentSource, /@visible-change="handleProductDropdownVisibleChange"/);
  assert.doesNotMatch(productSelectionSource, /const warmupProductDropdownOptions = async \(\) => \{/);
  assert.doesNotMatch(productSelectionSource, /const handleProductDropdownVisibleChange = \(visible: boolean\) => \{/);
});

test('sale order form searches customers remotely instead of loading all customers on first paint', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const headerSource = readSaleOrderModule('SaleOrderHeaderForm.vue');
  const baseDataSource = readSaleOrderModule('useSaleOrderBaseData.ts');
  const mountedBlock = componentSource.match(/onMounted\(\(\) => \{[\s\S]*?\n\}\);/)?.[0] ?? '';

  assert.doesNotMatch(mountedBlock, /\bfetchCustomers\(\);/);
  assert.doesNotMatch(componentSource, /getCachedCustomers/);
  assert.doesNotMatch(headerSource, /<FuzzyProductSelect/);
  assert.match(componentSource, /:search-customers="searchCustomers"/);
  assert.match(componentSource, /:customer-search-loading="customerSearchLoading"/);
  assert.match(headerSource, /<el-select[\s\S]*v-model="formData\.customerId"[\s\S]*filterable[\s\S]*remote/);
  assert.match(headerSource, /:remote-method="searchCustomers"/);
  assert.match(headerSource, /:loading="customerSearchLoading"/);
  assert.match(headerSource, /:automatic-dropdown="false"/);
  assert.match(baseDataSource, /request\.get\('\/erp\/customers\/search'/);
  assert.match(baseDataSource, /size:\s*20/);
});

test('sale order customer dropdown waits for input and keeps selected customer available', () => {
  const baseDataSource = readSaleOrderModule('useSaleOrderBaseData.ts');

  assert.match(baseDataSource, /const normalizedKeyword = keyword\.trim\(\);/);
  assert.match(baseDataSource, /if \(!normalizedKeyword\) \{/);
  assert.match(baseDataSource, /customerOptions\.value = formData\.customerId\s*\? keepSelectedCustomerOption\(customerOptions\.value\)\s*:\s*\[\];/);
  assert.match(baseDataSource, /return;/);
  assert.match(baseDataSource, /const ensureCustomerOption = async \(customerId\?: number \| null\) =>/);
  assert.match(baseDataSource, /request\.get\(`\/erp\/customers\/\$\{customerId\}`\)/);
});
