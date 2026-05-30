import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readStyle = () => readFileSync(join(viewsRoot, '..', '..', 'styles', 'table.css'), 'utf8');

test('product management uses supplier-style resizable dialog for create and edit form', () => {
  const componentSource = readView('ErpProductManagement.vue');
  const tableStyle = readStyle();

  assert.match(componentSource, /class="product-dialog"/);
  assert.match(componentSource, /draggable/);
  assert.match(componentSource, /overflow/);
  assert.match(componentSource, /:style="productDialogStyle"/);
  assert.match(componentSource, /product-dialog__header/);
  assert.match(componentSource, /product-dialog__intro/);
  assert.match(componentSource, /product-dialog__content/);
  assert.match(componentSource, /product-dialog__footer/);
  assert.doesNotMatch(componentSource, /<el-drawer[\s\S]*v-model="showModal"/);
  assert.doesNotMatch(componentSource, /layoutMode/);
  assert.doesNotMatch(componentSource, /form-mode-toggle/);
  assert.match(componentSource, /product-dialog__resize-handle product-dialog__resize-handle--nw/);
  assert.match(componentSource, /product-dialog__resize-handle product-dialog__resize-handle--ne/);
  assert.match(componentSource, /product-dialog__resize-handle product-dialog__resize-handle--sw/);
  assert.match(componentSource, /product-dialog__resize-handle product-dialog__resize-handle--se/);
  assert.match(componentSource, /document\.querySelector\('\.el-dialog\.product-dialog'\)/);
  assert.match(componentSource, /const padding = 16;/);
  assert.match(componentSource, /price-list-container/);
  assert.match(componentSource, /price-item/);
  assert.match(componentSource, /price-input/);
  assert.match(componentSource, /grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/);
  assert.match(componentSource, /@container\s*\(max-width:\s*840px\)\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/);
  assert.match(componentSource, /@container\s*\(max-width:\s*560px\)\s*\{[\s\S]*grid-template-columns:\s*1fr/);
  assert.match(componentSource, /\.price-item\s*\{[\s\S]*grid-template-columns:\s*minmax\(72px,\s*auto\)\s+minmax\(120px,\s*1fr\)/);
  assert.match(tableStyle, /\.product-dialog__resize-handle\s*\{[\s\S]*width:\s*18px;[\s\S]*height:\s*18px;/);
  assert.match(tableStyle, /\.product-dialog__resize-handle--ne\s*\{[\s\S]*right:\s*0;/);
  assert.match(tableStyle, /\.el-dialog\.product-dialog\s+\.el-dialog__headerbtn\s*\{[\s\S]*top:\s*12px;[\s\S]*right:\s*12px;[\s\S]*width:\s*24px;[\s\S]*height:\s*24px;[\s\S]*z-index:\s*11;/);
  assert.match(tableStyle, /\.el-dialog\.product-dialog\s*\{/);
  assert.doesNotMatch(tableStyle, /\.product-dialog\s+\.el-dialog\b/);
});

test('product management exposes excel import entry', () => {
  const componentSource = readView('ErpProductManagement.vue');

  assert.match(componentSource, /v-permission="'erp-product:import'"/);
  assert.match(componentSource, /ref="productImportInputRef"/);
  assert.match(componentSource, /accept="\.xls,\s*\.xlsx"/);
  assert.match(componentSource, /request\.post\('\/erp\/products\/import', formData\)/);
  assert.match(componentSource, /triggerProductImport/);
  assert.match(componentSource, /handleProductImportFile/);
});
