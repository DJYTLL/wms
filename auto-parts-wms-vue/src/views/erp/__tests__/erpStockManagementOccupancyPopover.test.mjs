import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewSource = readFileSync(join(__dirname, '..', 'ErpStockManagement.vue'), 'utf8');

test('库存台账占用明细接口只在悬浮时请求且单号新标签打开', () => {
  assert.match(viewSource, /@show="loadOccupancy\(row\)"/);
  assert.match(viewSource, /request\.get\(`\/erp\/stock\/balances\/\$\{row\.id\}\/occupancy`\)/);
  assert.match(viewSource, /window\.open\(route\.href, '_blank'/);
});
