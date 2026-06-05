import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

test('heavy ERP basic page hidden product dialogs and drawers are not mounted on first paint', () => {
  const source = readView('ErpProductManagement.vue');

  assert.match(source, /<el-dialog\s+v-if="showModal"/);
  assert.match(source, /<Teleport\s+v-if="showModal"/);
  assert.match(source, /<el-dialog\s+v-if="purchaseHistoryDialogVisible"/);
  assert.match(source, /<el-dialog\s+v-if="purchaseOrderDetailDialogVisible"/);
  assert.match(source, /<el-dialog v-if="showProductImportDialog" v-model="showProductImportDialog"/);
  assert.match(source, /<el-drawer v-if="showProductImportHistoryDrawer" v-model="showProductImportHistoryDrawer"/);
});

test('heavy ERP customer and supplier edit/import overlays are not mounted on first paint', () => {
  const customerSource = readView('ErpCustomerManagement.vue');
  const supplierSource = readView('ErpSupplierManagement.vue');

  assert.match(customerSource, /<ErpCustomerEditDialog\s+v-if="showModal"/);
  assert.match(customerSource, /<el-dialog v-if="showCustomerImportDialog" v-model="showCustomerImportDialog"/);
  assert.match(customerSource, /<el-drawer v-if="showCustomerImportHistoryDrawer" v-model="showCustomerImportHistoryDrawer"/);

  assert.match(supplierSource, /<ErpSupplierEditDialog\s+v-if="showModal"/);
  assert.match(supplierSource, /<el-dialog v-if="showImportDialog" v-model="showImportDialog"/);
  assert.match(supplierSource, /<el-drawer v-if="showImportHistoryDrawer" v-model="showImportHistoryDrawer"/);
});
