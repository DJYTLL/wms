import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('stock warning actions carry stock warning context to purchase order and stock transfer pages', () => {
  const stockWarningSource = readFileSync(join(viewsRoot, 'ErpStockWarningManagement.vue'), 'utf8');
  const purchaseOrderSource = readFileSync(join(viewsRoot, 'ErpPurchaseOrderForm.vue'), 'utf8');
  const stockTransferSource = readFileSync(join(viewsRoot, 'ErpStockTransferManagement.vue'), 'utf8');

  assert.match(
    stockWarningSource,
    /router\.push\(\{\s*path:\s*'\/erp\/purchase-orders\/create',\s*query:\s*\{\s*productId:\s*row\.productId,\s*warehouseId:\s*row\.warehouseId,\s*warningSource:\s*'stock-warning'\s*\}\s*\}\)/
  );
  assert.match(
    stockWarningSource,
    /router\.push\(\{\s*path:\s*'\/erp\/stock-transfers\/create',\s*query:\s*\{\s*productId:\s*row\.productId,\s*warehouseId:\s*row\.warehouseId,\s*warningSource:\s*'stock-warning'\s*\}\s*\}\)/
  );

  assert.match(purchaseOrderSource, /const warningSource = typeof route\.query\.warningSource === 'string' \? route\.query\.warningSource : '';/);
  assert.match(purchaseOrderSource, /const contextProductId = Number\(route\.query\.productId\);/);
  assert.match(purchaseOrderSource, /const contextWarehouseId = Number\(route\.query\.warehouseId\);/);
  assert.match(purchaseOrderSource, /if \(!isEditing\.value\) \{[\s\S]*if \(warningSource === 'stock-warning' && formData\.items\.length\) \{/);
  assert.match(purchaseOrderSource, /formData\.items\[0\]\.productId = Number\.isFinite\(contextProductId\) && contextProductId > 0 \? contextProductId : undefined;/);
  assert.match(purchaseOrderSource, /formData\.items\[0\]\.warehouseId = Number\.isFinite\(contextWarehouseId\) && contextWarehouseId > 0 \? contextWarehouseId : formData\.items\[0\]\.warehouseId;/);

  assert.match(stockTransferSource, /const warningSource = typeof route\.query\.warningSource === 'string' \? route\.query\.warningSource : '';/);
  assert.match(stockTransferSource, /const contextProductId = Number\(route\.query\.productId\);/);
  assert.match(stockTransferSource, /const contextWarehouseId = Number\(route\.query\.warehouseId\);/);
  assert.match(stockTransferSource, /if \(formMode\.value === 'create'\) \{[\s\S]*if \(warningSource === 'stock-warning' && formData\.items\.length\) \{/);
  assert.match(stockTransferSource, /formData\.items\[0\]\.productId = Number\.isFinite\(contextProductId\) && contextProductId > 0 \? contextProductId : null;/);
  assert.match(stockTransferSource, /formData\.items\[0\]\.toWarehouseId = Number\.isFinite\(contextWarehouseId\) && contextWarehouseId > 0 \? contextWarehouseId : null;/);
  assert.match(stockTransferSource, /formData\.items\[0\]\.targetStockKey = buildStockKey\(formData\.items\[0\]\.toWarehouseId, null\);/);
});
