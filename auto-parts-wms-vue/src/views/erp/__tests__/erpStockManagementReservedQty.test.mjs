import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const componentSource = readFileSync(join(viewsRoot, 'ErpStockManagement.vue'), 'utf8');

test('库存台账展示库存在库、库存占用和可用库存三种数量口径', () => {
  assert.match(componentSource, /canShow\('qtyOnHand'\)/);
  assert.match(componentSource, /prop="qtyOnHand"/);
  assert.match(componentSource, /\$t\('field\.qtyOnHand'\)/);

  assert.match(componentSource, /canShow\('qtyLocked'\)/);
  assert.match(componentSource, /\$t\('field\.qtyLocked'\)/);
  assert.match(componentSource, /row\.qtyLocked/);

  assert.match(componentSource, /canShow\('qtyAvailable'\)/);
  assert.match(componentSource, /prop="qtyAvailable"/);
  assert.match(componentSource, /\$t\('field\.qtyAvailable'\)/);

  assert.match(
    componentSource,
    /const defaultColumns = \['product', 'warehouse', 'location', 'qtyOnHand', 'qtyLocked', 'qtyAvailable', 'updatedAt'\]/,
  );
  assert.doesNotMatch(componentSource, /const defaultColumns = \['product', 'warehouse', 'location', 'qty', 'updatedAt'\]/);
});

test('库存台账占用数量可悬浮查看明细并支持单号新标签页跳转', () => {
  assert.match(componentSource, /el-popover|el-tooltip/);
  assert.match(componentSource, /window\.open\(/);
  assert.match(componentSource, /'_blank'/);
});
