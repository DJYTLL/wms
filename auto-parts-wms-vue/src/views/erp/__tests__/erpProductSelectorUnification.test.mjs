import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

const remoteProductPages = [
  'ErpPurchaseOrderForm.vue',
  'ErpSaleReturnForm.vue',
  'ErpPurchaseReturnForm.vue',
  'ErpAssemblyOrderForm.vue',
  'ErpDisassembleOrderForm.vue',
  'ErpStockTransferManagement.vue',
  'ErpStockCountManagement.vue'
];

test('editable ERP product selectors use remote search mode consistently', () => {
  for (const page of remoteProductPages) {
    const source = readView(page);

    assert.match(source, /\bremote\b/, `${page} should enable remote search`);
    assert.match(source, /:remote-method="/, `${page} should define remote-method`);
    assert.match(source, /reserve-keyword/, `${page} should preserve typed keyword`);
  }
});
