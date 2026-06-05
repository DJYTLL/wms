import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

test('sale order and sale return form route entries are split by business workspace', () => {
  const entries = [
    ['ErpSaleOrderDraftForm.vue', 'ErpSaleOrderDraftFormPage.vue', /workspace="draft"/],
    ['ErpSaleOrderApprovedForm.vue', 'ErpSaleOrderApprovedFormPage.vue', /workspace="approved"/],
    ['ErpSaleReturnDraftForm.vue', 'ErpSaleReturnDraftFormPage.vue', /workspace="draft"/],
    ['ErpSaleReturnApprovedForm.vue', 'ErpSaleReturnApprovedFormPage.vue', /workspace="approved"/]
  ];

  for (const [entryName, pageName, workspacePattern] of entries) {
    const entrySource = readView(entryName);
    assert.match(entrySource, new RegExp(`defineAsyncComponent\\(\\(\\) => import\\('\\./${pageName}'\\)\\)`));
    assert.doesNotMatch(entrySource, /import ErpSale(Order|Return)Form from/);
    assert.equal(existsSync(join(viewsRoot, pageName)), true, `${pageName} should exist`);

    const pageSource = readView(pageName);
    assert.match(pageSource, workspacePattern);
  }
});

test('sale form first paint excludes heavy non-primary business dialogs from the initial chunk', () => {
  const saleOrderSource = readView('ErpSaleOrderForm.vue');
  const saleReturnSource = readView('ErpSaleReturnForm.vue');
  const saleOrderProductEditBridgeSource = readView('SaleOrderProductEditBridge.vue');
  const saleOrderProductHistorySource = readView('SaleOrderProductHistoryDialog.vue');
  const saleOrderQuickAssemblySource = readView('SaleOrderQuickAssemblyDialog.vue');
  const saleOrderPrintPreviewSource = readView('SaleOrderPrintPreview.vue');

  assert.doesNotMatch(saleOrderSource, /import ErpProductEditDrawer from '@\/components\/ErpProductEditDrawer\.vue';/);
  assert.doesNotMatch(saleOrderSource, /import ProductHistoryDialog from '@\/components\/ProductHistoryDialog\.vue';/);
  assert.doesNotMatch(saleOrderSource, /import ProductStockSelect from '@\/components\/ProductStockSelect\.vue';/);
  assert.doesNotMatch(saleOrderSource, /import PrintPreviewDialog from '@\/components\/PrintPreviewDialog\.vue';/);
  assert.match(saleOrderSource, /const SaleOrderProductEditBridge = defineAsyncComponent\(\(\) => import\('\.\/SaleOrderProductEditBridge\.vue'\)\);/);
  assert.match(saleOrderSource, /const SaleOrderProductHistoryDialog = defineAsyncComponent\(\(\) => import\('\.\/SaleOrderProductHistoryDialog\.vue'\)\);/);
  assert.match(saleOrderSource, /const SaleOrderQuickAssemblyDialog = defineAsyncComponent\(\(\) => import\('\.\/SaleOrderQuickAssemblyDialog\.vue'\)\);/);
  assert.match(saleOrderSource, /const SaleOrderPrintPreview = defineAsyncComponent\(\(\) => import\('\.\/SaleOrderPrintPreview\.vue'\)\);/);
  assert.match(saleOrderProductEditBridgeSource, /const ErpProductEditDrawer = defineAsyncComponent\(\(\) => import\('@\/components\/ErpProductEditDrawer\.vue'\)\);/);
  assert.match(saleOrderProductHistorySource, /const ProductHistoryDialog = defineAsyncComponent\(\(\) => import\('@\/components\/ProductHistoryDialog\.vue'\)\);/);
  assert.match(saleOrderQuickAssemblySource, /const ProductStockSelect = defineAsyncComponent\(\(\) => import\('@\/components\/ProductStockSelect\.vue'\)\);/);
  assert.match(saleOrderPrintPreviewSource, /const PrintPreviewDialog = defineAsyncComponent\(\(\) => import\('@\/components\/PrintPreviewDialog\.vue'\)\);/);

  assert.doesNotMatch(saleReturnSource, /import PrintPreviewDialog from '@\/components\/PrintPreviewDialog\.vue';/);
  assert.match(saleReturnSource, /const PrintPreviewDialog = defineAsyncComponent\(\(\) => import\('@\/components\/PrintPreviewDialog\.vue'\)\);/);
});

test('sale return form only mounts hidden dialogs after they are opened', () => {
  const source = readView('ErpSaleReturnForm.vue');

  assert.match(source, /<el-dialog\s+v-if="saleOrderPreviewDialogVisible"[\s\S]*?v-model="saleOrderPreviewDialogVisible"/);
  assert.match(source, /<el-dialog\s+v-if="showSaleOrderDialog"[\s\S]*?v-model="showSaleOrderDialog"/);
  assert.match(source, /<el-dialog\s+v-if="showSaleOrderReturnedDialog"[\s\S]*?v-model="showSaleOrderReturnedDialog"/);
  assert.match(source, /<el-dialog\s+v-if="showRecentSaleDialog"[\s\S]*?v-model="showRecentSaleDialog"/);
  assert.match(source, /<el-dialog\s+v-if="showCustomerChangeDialog"[\s\S]*?v-model="showCustomerChangeDialog"/);
  assert.match(source, /<el-dialog\s+v-if="saveSuccessDialogVisible"[\s\S]*?v-model="saveSuccessDialogVisible"/);
  assert.match(source, /<PrintPreviewDialog\s+v-if="printDialogVisible"[\s\S]*?v-model="printDialogVisible"/);
});

test('old aggregate sale list pages are kept out of route entry chunks', () => {
  const routerSource = readFileSync(join(viewsRoot, '..', '..', 'router', 'index.ts'), 'utf8');
  assert.doesNotMatch(routerSource, /ErpSaleOrderManagement\.vue/);
  assert.doesNotMatch(routerSource, /ErpSaleReturnManagement\.vue/);
});
