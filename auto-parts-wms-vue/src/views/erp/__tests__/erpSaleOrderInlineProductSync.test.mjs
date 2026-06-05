import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readSaleOrderModule = (relativePath) => readFileSync(join(viewsRoot, 'sale-order', relativePath), 'utf8');

test('sale order form refreshes every matching row after inline product edit is saved', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const productSelectionSource = readSaleOrderModule('useSaleOrderProductSelection.ts');

  assert.match(componentSource, /const \{[\s\S]*?handleInlineProductSaved[\s\S]*?\} = useSaleOrderProductSelection\(/);
  assert.match(productSelectionSource, /const syncEditedProductToItems = async \(product: ErpProductDetail\) => \{/);
  assert.match(productSelectionSource, /const matchedItems = formData\.items\.filter\(item => item\.productId === product\.id\);/);
  assert.match(productSelectionSource, /for \(const item of matchedItems\) \{/);
  assert.match(productSelectionSource, /item\.productName = product\.name;/);
  assert.match(productSelectionSource, /applyProductDefaults\(item, true\);/);
  assert.match(productSelectionSource, /await fetchStockOptions\(product\.id, true\);/);
  assert.match(productSelectionSource, /syncStockKey\(item\);/);
  assert.match(productSelectionSource, /await applyPriceForRow\(item, false\);/);
  assert.match(productSelectionSource, /await syncEditedProductToItems\(product\);/);
});

test('sale order form overwrites row warehouse and location with edited product defaults', () => {
  const productSelectionSource = readSaleOrderModule('useSaleOrderProductSelection.ts');

  assert.match(productSelectionSource, /for \(const item of matchedItems\) \{[\s\S]*applyProductDefaults\(item, true\);/);
});

test('sale order form clears row warehouse and location when edited product defaults are removed', () => {
  const stockSelectionSource = readSaleOrderModule('useSaleOrderStockSelection.ts');

  assert.match(stockSelectionSource, /if \(force\) \{\s*row\.warehouseId = product\.defaultWarehouseId \?\? undefined;\s*row\.locationId = product\.defaultLocationId \?\? undefined;\s*\}/);
});
