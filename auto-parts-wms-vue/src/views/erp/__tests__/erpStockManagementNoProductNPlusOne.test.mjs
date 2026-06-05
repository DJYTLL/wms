import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewSource = readFileSync(join(__dirname, '..', 'ErpStockManagement.vue'), 'utf8');

test('库存台账直接使用列表返回的名称字段，不再逐条补商品详情请求', () => {
  assert.match(viewSource, /\{\{ row\.productName \|\| '-' \}\}/);
  assert.match(viewSource, /\{\{ row\.warehouseName \|\| '-' \}\}/);
  assert.match(viewSource, /\{\{ row\.locationName \|\| getLocationName\(row\.locationId\) \}\}/);
  assert.doesNotMatch(viewSource, /await Promise\.all\(uniqueProductIds\.map\(id => ensureProductOption\(id\)\)\)/);
  assert.doesNotMatch(viewSource, /request\.get\(`\/erp\/products\/\$\{productId\}`\)/);
});
