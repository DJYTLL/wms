import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('stock warning page distinguishes product-level and warehouse-level scope in table columns', () => {
  const componentSource = readFileSync(join(viewsRoot, 'ErpStockWarningManagement.vue'), 'utf8');

  assert.match(componentSource, /canShow\('stockWarningLevel'\)/);
  assert.match(componentSource, /stockWarningLevelLabel\(row\)/);
  assert.match(componentSource, /\$t\('field\.stockWarningLevel'\)/);
  assert.match(componentSource, /canShow\('warehouseScope'\)/);
  assert.match(componentSource, /warehouseScopeLabel\(row\)/);
  assert.match(componentSource, /\$t\('field\.warehouseScope'\)/);
  assert.match(componentSource, /canShow\('location'\)/);
  assert.match(componentSource, /prop="locationName"/);
  assert.match(componentSource, /\$t\('field\.location'\)/);
  assert.match(componentSource, /const defaultColumns = \['productCode', 'productName', 'categoryName', 'unitName', 'totalQty', 'safetyStock', 'minStock', 'maxStock', 'status', 'stockWarningLevel', 'warehouseScope', 'location', 'anomaly'\]/);
  assert.doesNotMatch(componentSource, /prop="defaultWarehouseName"/);
  assert.doesNotMatch(componentSource, /prop="defaultLocationName"/);
});
