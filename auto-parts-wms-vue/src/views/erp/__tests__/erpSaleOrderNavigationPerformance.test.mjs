import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

test('sale order create page renders a draft shell before waiting for next order number', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const loadDetailBlock = componentSource.match(/const loadDetail = async \(\) => \{[\s\S]*?\n\};/)?.[0] ?? '';

  assert.match(loadDetailBlock, /if \(!editing\) \{/);
  assert.match(componentSource, /const initializeDraftShell = \(\) => \{/);
  assert.match(loadDetailBlock, /initializeDraftShell\(\);/);
  assert.match(loadDetailBlock, /void fetchNextOrderNo\(seq\);/);
  assert.doesNotMatch(loadDetailBlock, /await fetchNextOrderNo\(\);/);
});

test('sale order list route refresh keeps existing rows while fetching the latest data', () => {
  const componentSource = readView('ErpSaleOrderDraftManagement.vue');
  const routeRefreshBlock = componentSource.match(/const runRouteRefresh = \(\) => \{[\s\S]*?\n\};/)?.[0] ?? '';

  assert.doesNotMatch(routeRefreshBlock, /tableData\.value = \[\];/);
  assert.doesNotMatch(routeRefreshBlock, /total\.value = 0;/);
  assert.match(routeRefreshBlock, /void fetchCustomers\(\);/);
  assert.match(routeRefreshBlock, /void columnSettings\.fetchTenantKeys\(\);/);
  assert.match(routeRefreshBlock, /handleSearch\(\);/);
});
