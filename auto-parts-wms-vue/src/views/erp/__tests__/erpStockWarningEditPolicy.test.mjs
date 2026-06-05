import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('stock warning edit dialog distinguishes product-level and warehouse-level editing copy', () => {
  const componentSource = readFileSync(join(viewsRoot, 'ErpStockWarningManagement.vue'), 'utf8');

  assert.match(componentSource, /warehouseName:\s*warehouseScopeLabel\(row\)/);
  assert.match(componentSource, /const matchedPolicy = \(product\.stockPolicies \|\| \[\]\)\.find\(item => item\.warehouseId === row\.warehouseId\)/);
  assert.match(componentSource, /const warehouseId = editingWarningRow\.value\?\.warehouseId \?\? null;/);
  assert.match(componentSource, /const stockPolicies = \[\.\.\.\(editingProduct\.value\.stockPolicies \|\| \[\]\)\];/);
  assert.match(componentSource, /const nextPolicy = policyIndex >= 0[\s\S]*minStock,\s*maxStock/);
  assert.match(componentSource, /stockPolicies/);
  assert.match(componentSource, /minStock: warehouseId == null \? minStock : editingProduct\.value\.minStock \?\? null/);
  assert.match(componentSource, /maxStock: warehouseId == null \? maxStock : editingProduct\.value\.maxStock \?\? null/);
  assert.match(componentSource, /productDialogTitle/);
  assert.match(componentSource, /stockWarningPolicyHint\(editingWarningRow\)/);
});
