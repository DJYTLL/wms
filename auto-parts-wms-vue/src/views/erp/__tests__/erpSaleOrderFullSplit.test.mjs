import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const saleOrderRoot = join(viewsRoot, 'sale-order');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readSaleOrderModule = (relativePath) => readFileSync(join(saleOrderRoot, relativePath), 'utf8');

test('sale order form is wired through the full split modules', () => {
  const formSource = readView('ErpSaleOrderForm.vue');
  const expectedModules = [
    'useSaleOrderHistory',
    'useSaleOrderProductSelection',
    'useSaleOrderStockSelection',
    'useSaleOrderPricing',
    'useSaleOrderSaveFlow',
    'useSaleOrderApprovedActions',
    'useSaleOrderQuickAssembly',
    'useSaleOrderBaseData'
  ];

  for (const moduleName of expectedModules) {
    assert.match(formSource, new RegExp(`import \\{ ${moduleName} \\} from './sale-order/${moduleName}'`));
    assert.match(formSource, new RegExp(`const \\{[\\s\\S]*?\\} = ${moduleName}\\(`));
    assert.equal(existsSync(join(saleOrderRoot, `${moduleName}.ts`)), true, `${moduleName}.ts should exist`);
  }

  assert.match(formSource, /import SaleOrderHeaderForm from '\.\/sale-order\/SaleOrderHeaderForm\.vue';/);
  assert.match(formSource, /import SaleOrderDetailTable from '\.\/sale-order\/SaleOrderDetailTable\.vue';/);
  assert.match(formSource, /<SaleOrderHeaderForm/);
  assert.match(formSource, /<SaleOrderDetailTable/);
  assert.equal(existsSync(join(saleOrderRoot, 'SaleOrderHeaderForm.vue')), true);
  assert.equal(existsSync(join(saleOrderRoot, 'SaleOrderDetailTable.vue')), true);
});

test('sale order split modules own their business responsibilities', () => {
  const expectedContent = [
    ['useSaleOrderHistory.ts', [/fetchPurchaseHistory/, /fetchSaleHistory/, /fetchCustomerSaleHistory/, /fetchProductPrices/]],
    ['useSaleOrderProductSelection.ts', [/searchProductsNow/, /ensureProductOption/, /handleInlineProductSaved/]],
    ['useSaleOrderStockSelection.ts', [/fetchStockOptions/, /syncStockKey/, /getStockOptionsForRow/]],
    ['useSaleOrderPricing.ts', [/calcLineAmount/, /totalSummary/, /applyPriceForRow/]],
    ['useSaleOrderSaveFlow.ts', [/saveData/, /openSaveSuccessDialog/, /handlePrintSavedOrder/]],
    ['useSaleOrderApprovedActions.ts', [/handleRedFlush/, /handleCancel/, /handleCopy/]],
    ['useSaleOrderQuickAssembly.ts', [/openAssemblyForRow/, /saveAssemblyQuickOrder/, /applyAssemblyTemplateDetail/]],
    ['useSaleOrderBaseData.ts', [/fetchCustomers/, /fetchWarehouses/, /applyDefaultMethods/]],
    ['saleOrderTypes.ts', [/interface SaleOrderItem/, /interface ProductOption/, /interface AssemblyQuickItem/]]
  ];

  for (const [fileName, patterns] of expectedContent) {
    const source = readSaleOrderModule(fileName);
    for (const pattern of patterns) {
      assert.match(source, pattern, `${fileName} should contain ${pattern}`);
    }
  }
});

test('obsolete sale order aggregate and preview files are no longer present', () => {
  const obsoleteFiles = [
    'ErpSaleOrderManagement.vue',
    'ErpSaleOrderFormPreview.vue',
    'ErpSaleOrderFormPreviewAlt.vue',
    'ErpSaleOrderFormPreviewPaper.vue'
  ];

  for (const fileName of obsoleteFiles) {
    assert.equal(existsSync(join(viewsRoot, fileName)), false, `${fileName} should be removed after split`);
  }
});
