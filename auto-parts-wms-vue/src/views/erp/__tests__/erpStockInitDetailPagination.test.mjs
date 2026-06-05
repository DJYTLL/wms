import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('stock init view page loads detail rows through a paged endpoint', () => {
  const source = readFileSync(join(viewsRoot, 'ErpStockCountManagement.vue'), 'utf8');

  assert.match(source, /detailItemsPage/);
  assert.match(source, /detailItemsSize/);
  assert.match(source, /loadDetailItemsPage/);
  assert.match(source, /includeItems:\s*false/);
  assert.match(source, /request\.get\(`\$\{apiPrefix\.value\}\/\$\{id\}\/items`/);
  assert.match(source, /params:\s*\{\s*page:\s*detailItemsPage\.value,\s*size:\s*detailItemsSize\.value\s*\}/);
  assert.match(source, /<el-pagination[\s\S]*?:current-page="detailItemsPage"[\s\S]*?:page-size="detailItemsSize"/);
  assert.match(source, /@size-change="handleDetailItemsSizeChange"/);
  assert.match(source, /@current-change="handleDetailItemsPageChange"/);
  assert.match(source, /ensureDetailItemOptions\(formData\.items,\s*\{\s*includeProducts:\s*false\s*\}\)/);
});
