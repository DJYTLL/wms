import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readRouter = () => readFileSync(join(viewsRoot, '..', '..', 'router', 'index.ts'), 'utf8');

test('sale order form delegates heavy dialogs instead of owning all dialog templates', () => {
  const source = readView('ErpSaleOrderForm.vue');

  assert.doesNotMatch(source, /<ProductHistoryDialog/);
  assert.doesNotMatch(source, /<ErpProductEditDrawer/);
  assert.doesNotMatch(source, /<PrintPreviewDialog/);
  assert.doesNotMatch(source, /<el-dialog[\s\S]*?save-success-dialog/);
  assert.doesNotMatch(source, /<el-dialog[\s\S]*?saveErrorDialogVisible/);
  assert.doesNotMatch(source, /<el-dialog[\s\S]*?showCustomerChangeDialog/);
  assert.match(source, /<SaleOrderProductHistoryDialog/);
  assert.match(source, /<SaleOrderProductEditBridge/);
  assert.match(source, /<SaleOrderPrintPreview/);
  assert.match(source, /<SaleOrderSaveFeedbackDialogs/);
  assert.match(source, /<SaleOrderCustomerChangeDialog/);
});

test('sale order quick assembly dialog is split from the primary form template', () => {
  const formSource = readView('ErpSaleOrderForm.vue');
  const dialogSource = readView('SaleOrderQuickAssemblyDialog.vue');

  assert.doesNotMatch(formSource, /class="assembly-quick-dialog"/);
  assert.match(formSource, /<SaleOrderQuickAssemblyDialog/);
  assert.match(dialogSource, /class="assembly-quick-dialog"/);
  assert.match(dialogSource, /ProductStockSelect/);
});

test('sale order form only mounts split dialog components when they are opened', () => {
  const source = readView('ErpSaleOrderForm.vue');

  assert.match(source, /<SaleOrderProductHistoryDialog\s+v-if="historyDialogVisible"/);
  assert.match(source, /<SaleOrderProductEditBridge\s+v-if="productEditDrawerVisible"/);
  assert.match(source, /<SaleOrderCustomerChangeDialog\s+v-if="showCustomerChangeDialog"/);
  assert.match(source, /<SaleOrderSaveFeedbackDialogs\s+v-if="saveErrorDialogVisible \|\| saveSuccessDialogVisible"/);
  assert.match(source, /<SaleOrderQuickAssemblyDialog\s+v-if="assemblyQuickDialogVisible"/);
  assert.match(source, /<SaleOrderPrintPreview\s+v-if="printDialogVisible"/);
});

test('sale order print preview wrapper does not mount inner print dialog while closed', () => {
  const source = readView('SaleOrderPrintPreview.vue');

  assert.match(source, /<PrintPreviewDialog\s+v-if="visible"/);
});

test('sale order experimental preview routes are removed from production router', () => {
  const routerSource = readRouter();

  assert.doesNotMatch(routerSource, /erp\/sale-orders\/create-preview/);
  assert.doesNotMatch(routerSource, /ErpSaleOrderFormPreview/);
});

test('legacy sale order list placeholders no longer load the removed aggregate list page', () => {
  const draftSource = readView('ErpSaleOrderDraft.vue');
  const approvedSource = readView('ErpSaleOrderApproved.vue');

  assert.doesNotMatch(draftSource, /ErpSaleOrderManagement/);
  assert.doesNotMatch(approvedSource, /ErpSaleOrderManagement/);
  assert.match(draftSource, /ErpSaleOrderDraftManagement/);
  assert.match(approvedSource, /ErpSaleOrderApprovedManagement/);
});
