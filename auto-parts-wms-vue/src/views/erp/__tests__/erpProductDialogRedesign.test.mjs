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
  assert.match(componentSource, /<div class="form-section module-card">[\s\S]*\$t\('section\.basicInfo'\)[\s\S]*\$t\('field\.weight'\)[\s\S]*\$t\('field\.volume'\)[\s\S]*<\/div>\s*<div class="form-section module-card inventory-policy-card">[\s\S]*\$t\('section\.inventoryInfo'\)/);
  assert.match(componentSource, /<div class="form-section module-card product-price-card">[\s\S]*\$t\('section\.priceInfo'\)[\s\S]*<el-row :gutter="16">[\s\S]*\$t\('field\.price'\)[\s\S]*\$t\('field\.taxRate'\)[\s\S]*<\/div>\s*<div class="form-section module-card customer-category-price-card">[\s\S]*\$t\('field\.customerCategoryPrice'\)/);
  assert.doesNotMatch(componentSource, /<div class="form-section module-card product-price-card">[\s\S]*\$t\('field\.weight'\)/);
  assert.doesNotMatch(componentSource, /<div class="form-section module-card product-price-card">[\s\S]*\$t\('field\.volume'\)/);
  assert.match(componentSource, /product-price-card__span-offset/);
  assert.match(componentSource, /<el-col v-for="item in priceItems"[\s\S]*<el-form-item :label="item\.categoryName">[\s\S]*<DecimalInput v-model="item\.salePrice"/);
  assert.doesNotMatch(componentSource, /price-list-container/);
  assert.doesNotMatch(componentSource, /price-item/);
  assert.doesNotMatch(componentSource, /price-label/);
  assert.doesNotMatch(componentSource, /price-input/);
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
  assert.match(componentSource, /submitProductImport/);
  assert.match(componentSource, /handleProductImportFile/);
});

test('product management renders inventory strategy as inline rows instead of embedded table', () => {
  const componentSource = readView('ErpProductManagement.vue');
  const tableStyle = readStyle();

  assert.match(componentSource, /inventory-policy-card/);
  assert.match(componentSource, /inventory-default-location-row/);
  assert.match(componentSource, /inventory-default-policy-row/);
  assert.match(componentSource, /inventory-policy-row inventory-policy-row--default/);
  assert.match(componentSource, /默认出入库指向/);
  assert.match(componentSource, /商品默认策略/);
  assert.match(componentSource, /inventory-default-location-row[\s\S]*\$t\('field\.defaultWarehouse'\)[\s\S]*\$t\('field\.defaultLocation'\)/);
  assert.match(componentSource, /inventory-default-policy-row[\s\S]*\$t\('field\.safetyStock'\)[\s\S]*\$t\('field\.minStock'\)[\s\S]*\$t\('field\.maxStock'\)/);
  assert.doesNotMatch(componentSource, /inventory-policy-row inventory-policy-row--default[\s\S]*\$t\('field\.defaultWarehouse'\)[\s\S]*\$t\('field\.safetyStock'\)/);
  assert.match(componentSource, /以下安全库存、最低库存、最高库存属于商品级默认策略，对无单独仓库策略的场景生效，不是上方默认仓库的专属阈值。/);
  assert.match(componentSource, /仓库覆盖策略：为指定仓库单独配置时，将优先覆盖商品默认策略。/);
  assert.match(componentSource, /v-for="\(item, index\) in formData\.stockPolicies"[\s\S]*class="inventory-policy-row"/);
  assert.match(componentSource, /addStockPolicy/);
  assert.match(componentSource, /removeStockPolicy\(index\)/);
  assert.match(componentSource, /新增仓库策略/);
  assert.match(tableStyle, /\.inventory-policy-card/);
  assert.match(tableStyle, /\.inventory-policy-card__section-label/);
  assert.match(tableStyle, /\.inventory-default-location-row/);
  assert.match(tableStyle, /\.inventory-default-policy-row/);
  assert.match(tableStyle, /\.inventory-policy-row/);
  assert.match(tableStyle, /\.inventory-policy-row__actions/);
});
