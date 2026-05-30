import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('stock init page uses excel upload to create opening stock drafts', () => {
  const source = readFileSync(join(viewsRoot, 'ErpStockCountManagement.vue'), 'utf8');

  assert.match(source, /v-if="countType === 'INIT'"\s+v-permission="permAdd"\s+@click="triggerImport">导入期初库存<\/el-button>/);
  assert.match(source, /:accept="countType === 'INIT' \? '\.xls,\.xlsx' : '\.csv,text\/csv'"/);
  assert.match(source, /if \(countType\.value === 'INIT'\) \{\s*importInputRef\.value\?\.click\(\);\s*return;\s*\}/);
  assert.match(source, /if \(countType\.value === 'INIT'\) \{\s*const formData = new FormData\(\)/);
  assert.match(source, /request\.post\('\/erp\/stock-inits\/import', formData\)/);
  assert.match(source, /type StockInitImportResult|interface StockInitImportResult/);
});
