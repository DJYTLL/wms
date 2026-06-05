import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

test('sale order list pages render real first paint content and load table panels asynchronously', () => {
  const entries = [
    ['ErpSaleOrderDraftManagement.vue', 'SaleOrderDraftDeferredPanel', 'loadSaleOrderDraftDeferredPanel', true],
    ['ErpSaleOrderApprovedManagement.vue', 'SaleOrderApprovedDeferredPanel', 'loadSaleOrderApprovedDeferredPanel', true],
    ['ErpSaleReturnDraftManagement.vue', 'SaleReturnDraftDeferredPanel', 'loadSaleReturnDraftDeferredPanel', false],
    ['ErpSaleReturnApprovedManagement.vue', 'SaleReturnApprovedDeferredPanel', 'loadSaleReturnApprovedDeferredPanel', false],
  ];

  for (const [entryName, panelName, loaderName, hasSummary] of entries) {
    const entrySource = readView(entryName);

    assert.match(entrySource, /defineAsyncComponent/);
    assert.match(entrySource, new RegExp(`import \\{ ${loaderName} \\} from './saleListDeferredPanelLoaders';`));
    assert.match(entrySource, new RegExp(`const ${panelName} = defineAsyncComponent\\(\\{[\\s\\S]*?loader: ${loaderName}`));
    assert.doesNotMatch(entrySource, new RegExp(`import ${panelName} from './${panelName}\\.vue';`));
    assert.match(entrySource, new RegExp(`<${panelName}\\s+v-if="showDeferredPanel"`));
    assert.match(entrySource, /const showDeferredPanel = ref\(false\);/);
    assert.match(entrySource, /await waitForSaleListFirstPaint\(\);[\s\S]*?showDeferredPanel\.value = true;/);
    assert.match(entrySource, /await waitForSaleListFirstPaint\(\);[\s\S]*?if \(pageSizeSyncReady\.value\) \{[\s\S]*?runRouteRefresh\(\);[\s\S]*?\} else \{[\s\S]*?pendingRouteRefresh\.value = true;[\s\S]*?\}/);
    assert.doesNotMatch(entrySource, /\{\s*flush: 'sync', immediate: true\s*\}/);
    assert.doesNotMatch(entrySource, /const FuzzyProductSelect = defineAsyncComponent/);
    assert.doesNotMatch(entrySource, /import\('@\/components\/FuzzyProductSelect\.vue'\)/);
    assert.doesNotMatch(entrySource, /import FuzzyProductSelect from '@\/components\/FuzzyProductSelect\.vue';/);
    assert.match(entrySource, /<ErpSaleListToolbar/);
    assert.doesNotMatch(entrySource, /DeferredSaleOrderPanel/);
    assert.doesNotMatch(entrySource, /shouldMountDeferredPanel/);
    assert.doesNotMatch(entrySource, /sale-order-deferred-placeholder/);

    if (hasSummary) {
      assert.match(readView(`${panelName}.vue`), /sale-summary-bar/);
    }
  }
});

test('sale list first paint helper waits for browser paint before mounting table panels', () => {
  const source = readView('saleListFirstPaint.ts');

  assert.match(source, /export const waitForSaleListFirstPaint/);
  assert.match(source, /requestAnimationFrame/);
  assert.match(source, /setTimeout/);
});

test('sale order routes mount real management pages directly while table panels stay deferred', () => {
  const routerSource = readFileSync(join(viewsRoot, '..', '..', 'router', 'index.ts'), 'utf8');

  assert.match(routerSource, /import ErpSaleOrderDraftManagement from '\.\.\/views\/erp\/ErpSaleOrderDraftManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleOrderApprovedManagement from '\.\.\/views\/erp\/ErpSaleOrderApprovedManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleReturnDraftManagement from '\.\.\/views\/erp\/ErpSaleReturnDraftManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleReturnApprovedManagement from '\.\.\/views\/erp\/ErpSaleReturnApprovedManagement\.vue'/);
  assert.match(routerSource, /path: 'erp\/sale-orders\/draft'[\s\S]*?component: ErpSaleOrderDraftManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-orders\/approved'[\s\S]*?component: ErpSaleOrderApprovedManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-returns\/draft'[\s\S]*?component: ErpSaleReturnDraftManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-returns\/approved'[\s\S]*?component: ErpSaleReturnApprovedManagement,/);
  assert.doesNotMatch(routerSource, /component: ErpSaleOrderDraft,/);
  assert.doesNotMatch(routerSource, /component: ErpSaleReturnDraft,/);
});

test('sale deferred table panels do not mount print preview until user opens print', () => {
  const panelNames = [
    'SaleOrderDraftDeferredPanel.vue',
    'SaleOrderApprovedDeferredPanel.vue',
    'SaleReturnDraftDeferredPanel.vue',
    'SaleReturnApprovedDeferredPanel.vue',
  ];

  for (const panelName of panelNames) {
    const source = readView(panelName);
    assert.match(source, /const PrintPreviewDialog = defineAsyncComponent/);
    assert.match(source, /<PrintPreviewDialog\s+v-if="printDialogVisible"/);
    assert.match(source, /printDialogVisible\.value = true;/);
  }
});
