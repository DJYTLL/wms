import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const componentsRoot = join(viewsRoot, '..', '..', 'components');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readComponent = (relativePath) => readFileSync(join(componentsRoot, relativePath), 'utf8');

test('customer management uses dedicated customer dialog component with reusable resize shell', () => {
  const pageSource = readView('ErpCustomerManagement.vue');
  const componentPath = join(componentsRoot, 'ErpCustomerEditDialog.vue');

  assert.equal(existsSync(componentPath), true, 'expected ErpCustomerEditDialog.vue to exist');

  const componentSource = readComponent('ErpCustomerEditDialog.vue');

  assert.match(pageSource, /import ErpCustomerEditDialog from '@\/components\/ErpCustomerEditDialog\.vue'/);
  assert.match(pageSource, /<ErpCustomerEditDialog/);
  assert.doesNotMatch(pageSource, /<el-dialog v-model="showModal"/);
  assert.doesNotMatch(pageSource, /table-key="erp-customer-contacts"/);
  assert.match(componentSource, /class="supplier-dialog customer-dialog"/);
  assert.match(componentSource, /draggable/);
  assert.match(componentSource, /overflow/);
  assert.match(componentSource, /:style="dialogStyle"/);
  assert.match(componentSource, /customer-dialog__drag-hint/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--nw/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--ne/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--sw/);
  assert.match(componentSource, /supplier-dialog__resize-handle supplier-dialog__resize-handle--se/);
  assert.match(componentSource, /<Teleport :to="resizeHandleTeleportTarget" :disabled="!resizeHandleTeleportTarget">/);
  assert.match(componentSource, /const startResize = \(/);
  assert.match(componentSource, /const handleResizePointerMove = \(/);
  assert.match(componentSource, /document\.querySelector\('\.el-dialog\.customer-dialog'\)/);
});

test('customer dialog uses contact cards with tag-based contact methods and primary selection', () => {
  const componentSource = readComponent('ErpCustomerEditDialog.vue');

  assert.match(componentSource, /class="supplier-contact-table"/);
  assert.match(componentSource, /class="supplier-contact-card"/);
  assert.match(componentSource, /class="supplier-contact-card__header"/);
  assert.match(componentSource, /class="supplier-contact-card__method-input"/);
  assert.match(componentSource, /class="supplier-contact-card__tag-list"/);
  assert.match(componentSource, /class="supplier-contact-tag"/);
  assert.match(componentSource, /class="supplier-contact-table__primary"/);
  assert.match(componentSource, /class="supplier-contact-table__actions"/);
  assert.match(componentSource, /主联系人/);
  assert.match(componentSource, /主号码/);
  assert.match(componentSource, /设为主号码/);
  assert.match(componentSource, /const addContact = \(\) =>/);
  assert.match(componentSource, /const removeContact = \(groupIndex: number\) =>/);
  assert.match(componentSource, /const commitContactMethod = \(groupIndex: number\) =>/);
  assert.match(componentSource, /const removeContactMethod = \(groupIndex: number, methodIndex: number\) =>/);
  assert.match(componentSource, /const setPrimaryMethod = \(groupIndex: number, methodIndex: number\) =>/);
  assert.match(componentSource, /const setPrimaryContact = \(index: number\) =>/);
  assert.match(componentSource, /const parseContacts = \(raw\?: unknown\): CustomerContactItem\[\] =>/);
  assert.match(componentSource, /const buildInitialContacts = \(row: CustomerFormValue\): CustomerContactGroup\[\] =>/);
  assert.match(componentSource, /const buildContactsPayload = \(\) =>/);
  assert.match(componentSource, /const buildContactInfoSummary = \(\) =>/);
  assert.match(componentSource, /const getPrimaryContact = \(\) =>/);
  assert.match(componentSource, /const isMobileLike = \(value\?: string\) =>/);
  assert.match(componentSource, /placeholder="输入联系方式后回车，或粘贴多个号码"/);
  assert.match(componentSource, /@keyup.enter.prevent="commitContactMethod\(groupIndex\)"/);
  assert.match(componentSource, /@blur="commitContactMethod\(groupIndex\)"/);
  assert.doesNotMatch(componentSource, /<ErpDataTable/);
});

test('customer management keeps contact search compatible with contacts json after dialog redesign', () => {
  const pageSource = readView('ErpCustomerManagement.vue');

  assert.match(pageSource, /const parseContacts = \(raw\?: unknown\) =>/);
  assert.match(pageSource, /const getCustomerContacts = \(row: ErpCustomer\) => parseContacts\(row\.contacts\);/);
  assert.match(pageSource, /const getCustomerContactTokens = \(row: ErpCustomer\) => uniqueContactTokens\(\[/);
  assert.match(pageSource, /getCustomerContacts\(row\)\.flatMap\(item => \[item\.phone, item\.mobile\]\)/);
  assert.match(pageSource, /const formatCustomerContactInfo = \(row: ErpCustomer\) =>/);
  assert.match(pageSource, /filtered = filterByFuzzyKeyword\(filtered, contactQuery\.value, row => \[/);
  assert.match(pageSource, /getCustomerContacts\(row\)\.map\(item => item\.name\)/);
  assert.match(pageSource, /filtered = filterByFuzzyKeyword\(filtered, phoneQuery\.value, row => \[/);
  assert.match(pageSource, /formatCustomerContactInfo\(row\),/);
  assert.match(pageSource, /getCustomerContactTokens\(row\)/);
});
