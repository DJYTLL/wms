import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readComponent = (relativePath) => readFileSync(join(viewsRoot, '..', '..', 'components', relativePath), 'utf8');
const readStyle = () => readFileSync(join(viewsRoot, '..', '..', 'styles', 'table.css'), 'utf8');

test('supplier dialog uses corner resize handles for freeform resizing plus independent optional toggles', () => {
  const pageSource = readView('ErpSupplierManagement.vue');
  const componentSource = readComponent('ErpSupplierEditDialog.vue');
  const tableStyle = readStyle();

  assert.match(pageSource, /import ErpSupplierEditDialog from '@\/components\/ErpSupplierEditDialog\.vue'/);
  assert.match(pageSource, /<ErpSupplierEditDialog/);
  assert.doesNotMatch(pageSource, /<el-dialog[\s\S]*class="supplier-dialog"/);
  assert.match(componentSource, /class="supplier-dialog"/);
  assert.match(componentSource, /class="supplier-dialog__intro"/);
  assert.match(componentSource, /draggable/);
  assert.match(componentSource, /overflow/);
  assert.match(componentSource, /:style="dialogStyle"/);
  assert.match(componentSource, /supplier-dialog__drag-hint/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--nw/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--ne/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--sw/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--se/);
  assert.match(componentSource, /startResize\('nw', \$event\)/);
  assert.match(componentSource, /startResize\('ne', \$event\)/);
  assert.match(componentSource, /startResize\('sw', \$event\)/);
  assert.match(componentSource, /startResize\('se', \$event\)/);
  assert.match(componentSource, /const startResize = \(/);
  assert.match(componentSource, /const handleResizePointerMove = \(/);
  assert.match(componentSource, /<Teleport :to="resizeHandleTeleportTarget" :disabled="!resizeHandleTeleportTarget">/);
  assert.match(componentSource, /const resizeHandleTeleportTarget = ref<HTMLElement \| null>\(null\)/);
  assert.match(componentSource, /document\.querySelector\('\.el-dialog\.supplier-dialog'\)/);
  assert.match(componentSource, /const padding = 16;/);
  assert.match(tableStyle, /\.supplier-dialog__resize-handle\s*\{[\s\S]*width:\s*18px;[\s\S]*height:\s*18px;/);
  assert.match(tableStyle, /\.supplier-dialog__resize-handle--ne\s*\{[\s\S]*right:\s*0;/);
  assert.match(tableStyle, /\.el-dialog\.supplier-dialog\s+\.el-dialog__headerbtn\s*\{[\s\S]*top:\s*12px;[\s\S]*right:\s*12px;[\s\S]*width:\s*24px;[\s\S]*height:\s*24px;[\s\S]*z-index:\s*11;/);
  assert.match(tableStyle, /\.el-dialog\.supplier-dialog\s*\{/);
  assert.doesNotMatch(tableStyle, /\.supplier-dialog\s+\.el-dialog\b/);
  assert.match(
    componentSource,
    /<\/el-form>\s*<\/div>\s*<Teleport :to="resizeHandleTeleportTarget" :disabled="!resizeHandleTeleportTarget">/
  );
  assert.match(
    componentSource,
    /supplier-dialog__resize-handle supplier-dialog__resize-handle--se"[\s\S]*?<\/Teleport>\s*<template #footer>/
  );
  assert.match(componentSource, /supplier-section__title">\s*\{\{\s*\$t\('supplierDialog\.sectionBasic'\)\s*\}\}/);
  assert.match(componentSource, /supplier-section__title">\s*\{\{\s*\$t\('supplierDialog\.sectionContact'\)\s*\}\}/);
  assert.match(componentSource, /supplier-section__title">\s*\{\{\s*\$t\('supplierDialog\.sectionBusiness'\)\s*\}\}/);
  assert.match(componentSource, /:aria-expanded="financeOptionalExpanded"/);
  assert.match(componentSource, /:aria-expanded="historyOptionalExpanded"/);
  assert.doesNotMatch(componentSource, /optionalSectionsExpanded/);
  assert.doesNotMatch(componentSource, /optionalToggleLabel/);
  assert.match(componentSource, /\$t\('supplierDialog\.financeToggleHint'\)/);
  assert.match(componentSource, /\$t\('supplierDialog\.historyToggleHint'\)/);
  assert.match(componentSource, /supplier-status-field/);
  assert.doesNotMatch(componentSource, /supplier-status-panel/);
  assert.match(componentSource, /\$t\('supplierDialog\.counterpartyHint'\)/);
  assert.match(componentSource, /class="supplier-contact-table"/);
  assert.match(componentSource, /class="supplier-contact-card"/);
  assert.match(componentSource, /class="supplier-contact-card__header"/);
  assert.match(componentSource, /class="supplier-contact-card__method-input"/);
  assert.match(componentSource, /class="supplier-contact-card__tag-list"/);
  assert.match(componentSource, /class="supplier-contact-tag"/);
  assert.match(componentSource, /class="supplier-contact-table__primary"/);
  assert.match(componentSource, /class="supplier-contact-table__actions"/);
  assert.match(componentSource, /const addContact = \(\) =>/);
  assert.match(componentSource, /const removeContact = \(groupIndex: number\) =>/);
  assert.match(componentSource, /const commitContactMethod = \(groupIndex: number\) =>/);
  assert.match(componentSource, /const removeContactMethod = \(groupIndex: number, methodIndex: number\) =>/);
  assert.match(componentSource, /const setPrimaryMethod = \(groupIndex: number, methodIndex: number\) =>/);
  assert.match(componentSource, /const setPrimaryContact = \(index: number\) =>/);
  assert.match(componentSource, /const parseContacts = \(raw\?: unknown\): SupplierContactItem\[\] =>/);
  assert.match(componentSource, /const buildInitialContacts = \(row: SupplierFormValue\): SupplierContactGroup\[\] =>/);
  assert.match(componentSource, /const buildContactsPayload = \(\) =>/);
  assert.match(componentSource, /const buildContactInfoSummary = \(\) =>/);
  assert.match(componentSource, /const isMobileLike = \(value\?: string\) =>/);
  assert.match(componentSource, /placeholder="输入联系方式后回车，或粘贴多个号码"/);
  assert.match(componentSource, /@keyup.enter.prevent="commitContactMethod\(groupIndex\)"/);
  assert.match(componentSource, /@blur="commitContactMethod\(groupIndex\)"/);
  assert.match(componentSource, /设为主号码/);
  assert.match(componentSource, /输入一个联系方式后按回车，或失焦自动生成标签；支持一次粘贴多个号码。/);
  assert.match(componentSource, /保存时会自动用主联系人同步列表页展示与检索字段/);
  assert.match(tableStyle, /\.supplier-contact-table\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-card\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-card__header\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-card__tag-list\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-tag\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-tag__primary\.is-active\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-table__primary\s*\{/);
  assert.match(tableStyle, /\.supplier-contact-table__actions\s*\{/);
});

test('supplier management exposes import entry and batch result workflow on the same page', () => {
  const pageSource = readView('ErpSupplierManagement.vue');

  assert.match(pageSource, /v-permission="'erp-supplier:import'"/);
  assert.match(pageSource, /openImportDialog/);
  assert.match(pageSource, /openImportHistoryDrawer/);
  assert.match(pageSource, /type="file"/);
  assert.match(pageSource, /accept="\.xls,\s*\.xlsx"/);
  assert.match(pageSource, /const importFile = ref<File \| null>\(null\)/);
  assert.match(pageSource, /const formData = new FormData\(\)/);
  assert.match(pageSource, /formData\.append\('file', importFile\.value\)/);
  assert.match(pageSource, /const trimmedSourceName = importSourceName\.value\.trim\(\)/);
  assert.match(pageSource, /if \(trimmedSourceName\) \{\s*formData\.append\('sourceName', trimmedSourceName\);\s*\}/);
  assert.match(pageSource, /request\.post\('\/erp\/suppliers\/import'/);
  assert.match(pageSource, /request\.post\('\/erp\/suppliers\/import',\s*formData/);
  assert.match(pageSource, /request\.get\('\/erp\/suppliers\/import-batches'\)/);
  assert.match(pageSource, /request\.get\(`\/erp\/suppliers\/import-batches\/\$\{batch\.id\}\/items`\)/);
  assert.match(pageSource, /startImportPolling/);
  assert.match(pageSource, /stopImportPolling/);
  assert.match(pageSource, /setTimeout\(\(\) => \{\s*void pollImportBatch/);
  assert.match(pageSource, /status === 'PROCESSING'/);
  assert.match(pageSource, /导入任务已创建：批次/);
  assert.match(pageSource, /<el-dialog v-model="showImportDialog" title="导入供应商历史表"/);
  assert.match(pageSource, /<el-drawer v-model="showImportHistoryDrawer" title="供应商导入结果"/);
  assert.match(pageSource, /uncategorizedCount/);
  assert.match(pageSource, /pendingSubjectMergeCount/);
  assert.match(pageSource, /warningMessage/);
  assert.doesNotMatch(pageSource, /importRawTable/);
  assert.doesNotMatch(pageSource, /rawTable/);
  assert.doesNotMatch(pageSource, /markdown 表格/);
});

test('supplier management shows a single contact info column instead of separate phone and mobile columns', () => {
  const pageSource = readView('ErpSupplierManagement.vue');

  assert.match(pageSource, /prop="contactInfo"/);
  assert.match(pageSource, /\$t\('field\.contactInfo'\)/);
  assert.doesNotMatch(pageSource, /prop="phone"/);
  assert.doesNotMatch(pageSource, /prop="mobile"/);
});

test('supplier management keeps contact info searchable and supplier dialog formats source created time for Shanghai display', () => {
  const pageSource = readView('ErpSupplierManagement.vue');
  const componentSource = readComponent('ErpSupplierEditDialog.vue');

  assert.match(pageSource, /v-model="phoneQuery"/);
  assert.match(pageSource, /row\.contactInfo/);
  assert.match(pageSource, /watch\(\s*\[\s*nameQuery,\s*codeQuery,\s*shortNameQuery,\s*contactQuery,\s*phoneQuery,\s*supplierTypeFilter,\s*businessScopeFilter,\s*statusFilter,\s*allTableData,\s*size\s*\]/);
  assert.match(componentSource, /:model-value="sourceCreatedAtDisplay"/);
  assert.match(componentSource, /const sourceCreatedAtDisplay = computed\(\(\) => formatSourceCreatedAtDisplay\(formData\.sourceCreatedAt\)\)/);
  assert.match(componentSource, /const formatSourceCreatedAtDisplay = \(value\?: string\) =>/);
  assert.match(componentSource, /timeZone:\s*'Asia\/Shanghai'/);
});
