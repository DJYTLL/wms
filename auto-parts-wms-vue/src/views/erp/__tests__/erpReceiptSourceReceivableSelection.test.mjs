import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const srcRoot = join(viewsRoot, '..', '..');

const receiptFormSource = readFileSync(join(viewsRoot, 'ErpReceiptForm.vue'), 'utf8');
const erpDataTableSource = readFileSync(join(srcRoot, 'components', 'ErpDataTable.vue'), 'utf8');

test('erp receipt source receivable dialog relies on exposed table selection methods', () => {
  assert.match(receiptFormSource, /ref="receivableTableRef"/);
  assert.match(receiptFormSource, /receivableTableRef\.value\.clearSelection\(\)/);
  assert.match(receiptFormSource, /receivableTableRef\.value\.toggleRowSelection\(row,\s*true\)/);

  assert.match(erpDataTableSource, /<ElementTable\s+ref="elementTableRef"/);
  assert.match(erpDataTableSource, /const clearSelection = \(\) => \{\s*elementTableRef\.value\?\.clearSelection\?\.\(\)\s*\}/s);
  assert.match(erpDataTableSource, /const toggleRowSelection = \(row: T, selected\?: boolean, ignoreSelectable = true\) => \{\s*elementTableRef\.value\?\.toggleRowSelection\?\.\(row, selected, ignoreSelectable\)\s*\}/s);
  assert.match(erpDataTableSource, /defineExpose\(\{\s*clearSelection,\s*toggleRowSelection,\s*toggleAllSelection,\s*getElementTableRef\s*\}\)/s);
});
