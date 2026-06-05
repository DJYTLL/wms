import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewSource = readFileSync(join(__dirname, '..', 'ErpStockManagement.vue'), 'utf8');

test('erp stock management uses remote product search like sale order and editable warehouse/location filters', () => {
  assert.match(viewSource, /<el-select[\s\S]*v-model="productFilter"[\s\S]*filterable[\s\S]*remote[\s\S]*reserve-keyword[\s\S]*:remote-method="searchProducts"[\s\S]*class="inventory-field--narrow"/);
  assert.match(viewSource, /<FuzzyProductSelect[\s\S]*v-model="warehouseFilter"[\s\S]*class="inventory-field--narrow"/);
  assert.match(viewSource, /<FuzzyProductSelect[\s\S]*v-model="locationFilter"[\s\S]*class="inventory-field--narrow"/);
  assert.match(viewSource, /const searchProductsNow = async \(keyword = ''\) => \{/);
  assert.match(viewSource, /const searchProducts = \(keyword = ''\) => \{/);
  assert.match(viewSource, /const locationFilterOptions = computed\(\(\) => \{/);
});
