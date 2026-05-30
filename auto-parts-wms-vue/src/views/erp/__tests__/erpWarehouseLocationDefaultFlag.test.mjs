import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (name) => readFileSync(join(viewsRoot, name), 'utf8');

test('warehouse and location management expose default column and switch', () => {
  const warehouseSource = readView('ErpWarehouseManagement.vue');
  const locationSource = readView('ErpLocationManagement.vue');

  assert.match(warehouseSource, /defaultColumns = \[[^\]]*'default'/);
  assert.match(warehouseSource, /canShow\('default'\)/);
  assert.match(warehouseSource, /row\.isDefault/);
  assert.match(warehouseSource, /v-model="formData\.isDefault"/);

  assert.match(locationSource, /defaultColumns = \[[^\]]*'default'/);
  assert.match(locationSource, /canShow\('default'\)/);
  assert.match(locationSource, /row\.isDefault/);
  assert.match(locationSource, /v-model="formData\.isDefault"/);
});

test('product create form applies default warehouse and location from options', () => {
  const componentSource = readView('ErpProductManagement.vue');

  assert.match(componentSource, /interface OptionItem[\s\S]*isDefault\?: boolean/);
  assert.match(componentSource, /const applyDefaultWarehouseAndLocation = \(\) =>/);
  assert.match(componentSource, /warehouseOptions\.value\.find\(item => item\.isDefault\)/);
  assert.match(componentSource, /locationOptions\.value\.find\(item => item\.isDefault && item\.warehouseId === formData\.defaultWarehouseId\)/);
  assert.match(componentSource, /resetForm\(\);[\s\S]*applyDefaultWarehouseAndLocation\(\);[\s\S]*fetchNextCode\(\);/);
});
