import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('product management uses async import batches with polling and history drawer', () => {
  const source = readFileSync(join(viewsRoot, 'ErpProductManagement.vue'), 'utf8');

  assert.match(source, /openProductImportDialog/);
  assert.match(source, /openProductImportHistoryDrawer/);
  assert.match(source, /request\.post\('\/erp\/products\/import', formData\)/);
  assert.match(source, /request\.get\('\/erp\/products\/import-batches'\)/);
  assert.match(source, /request\.get\(`\/erp\/products\/import-batches\/\$\{batch\.id\}\/items`\)/);
  assert.match(source, /startProductImportPolling/);
  assert.match(source, /stopProductImportPolling/);
  assert.match(source, /setTimeout\(\(\) => \{\s*void pollProductImportBatch/);
  assert.match(source, /showProductImportHistoryDrawer/);
  assert.match(source, /selectedProductImportBatch/);
  assert.match(source, /productImportBatchItems/);
  assert.match(source, /status === 'PROCESSING'/);
  assert.match(source, /<el-drawer v-model="showProductImportHistoryDrawer" title="配件导入结果"/);
  assert.match(source, /warningMessage/);
  assert.match(source, /errorMessage/);
});
