import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readSaleOrderModule = (relativePath) => readFileSync(join(viewsRoot, 'sale-order', relativePath), 'utf8');

test('sale order create flow offers print after saving a draft order', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const feedbackDialogSource = readView('SaleOrderSaveFeedbackDialogs.vue');

  assert.match(componentSource, /const canPrintSavedOrder = computed\(\(\) => \{/);
  assert.match(componentSource, /Boolean\(saveSuccessOrderId\.value\) && hasPermission\('erp-sale-draft:print'\)/);
  assert.match(
    feedbackDialogSource,
    /<el-button v-if="mode === 'save' && canPrintSavedOrder" type="primary" @click="\$emit\('printSavedOrder'\)">\{\{ \$t\('action\.print'\) \}\}<\/el-button>/
  );
  assert.match(componentSource, /@print-saved-order="handlePrintSavedOrder"/);
  const saveFlowSource = readSaleOrderModule('useSaleOrderSaveFlow.ts');
  assert.match(saveFlowSource, /const handlePrintSavedOrder = \(\) => \{[\s\S]*?pendingPrintDocId\.value = savedId;[\s\S]*?closeSaveSuccessDialog\(\);[\s\S]*?\};/);
});

test('sale order top print button stays scoped to existing orders', () => {
  const componentSource = readView('ErpSaleOrderForm.vue');
  const saveFlowSource = readSaleOrderModule('useSaleOrderSaveFlow.ts');

  assert.match(
    componentSource,
    /const canPrint = computed\(\(\) => \{[\s\S]*?if \(!isEditing\.value\) return !isApprovedWorkspace\.value && hasPermission\('erp-sale-draft:print'\);[\s\S]*?return hasPermission\(isApprovedWorkspace\.value \? 'erp-sale-approved:print' : 'erp-sale-draft:print'\);[\s\S]*?\}\);/
  );
  assert.match(saveFlowSource, /const handlePrint = async \(\) => \{[\s\S]*?if \(!isEditing\.value\) \{[\s\S]*?const savedId = await saveData\(\{ closeOnSuccess: false, showPostSaveDialog: false, silentSuccess: true \}\);[\s\S]*?openPrintPreview\(savedId\);[\s\S]*?return;[\s\S]*?\}[\s\S]*?openPrintPreview\(Number\(id\)\);[\s\S]*?\};/);
  assert.match(saveFlowSource, /const openPrintPreview = \(docId\?: number \| null\) => \{[\s\S]*?printDocId\.value = docId;[\s\S]*?printDialogVisible\.value = true;[\s\S]*?\};/);
});

test('sale order save success dialog follows the approved grouped layout design', () => {
  const componentSource = readView('SaleOrderSaveFeedbackDialogs.vue');

  assert.match(componentSource, /<el-dialog[\s\S]*?class="save-success-dialog"[\s\S]*?width="520px"/);
  assert.doesNotMatch(componentSource, /<el-dialog[\s\S]*?:title="successTitle"/);
  assert.match(componentSource, /<div class="save-success-dialog__header">[\s\S]*?save-success-dialog__icon[\s\S]*?successTitle[\s\S]*?<\/div>/);
  assert.match(componentSource, /<template #footer>[\s\S]*?<div class="save-success-dialog__footer">[\s\S]*?<div class="save-success-dialog__actions save-success-dialog__actions--secondary">[\s\S]*?\$emit\('continueCreate'\)[\s\S]*?\$emit\('stayCurrent'\)[\s\S]*?\$emit\('backToList'\)[\s\S]*?<\/div>[\s\S]*?<div class="save-success-dialog__actions save-success-dialog__actions--primary">[\s\S]*?\$emit\('printSavedOrder'\)[\s\S]*?\$emit\('approveSavedOrder'\)[\s\S]*?<\/div>[\s\S]*?<\/div>[\s\S]*?<\/template>/);
  assert.match(componentSource, /:deep\(\.save-success-dialog \.el-dialog__header\) \{/);
  assert.match(componentSource, /\.save-success-dialog__footer \{/);
  assert.match(componentSource, /\.save-success-dialog__actions \{[\s\S]*?flex-wrap: nowrap;/);
  assert.match(componentSource, /\.save-success-dialog__actions--primary \{/);
});
