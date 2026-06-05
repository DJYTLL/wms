import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readRouter = () => readFileSync(join(viewsRoot, '..', '..', 'router', 'index.ts'), 'utf8');

test('sale order and sale return list routes mount four real management pages directly', () => {
  const routerSource = readRouter();

  assert.doesNotMatch(routerSource, /import ErpSaleOrderManagement from '\.\.\/views\/erp\/ErpSaleOrderManagement\.vue'/);
  assert.doesNotMatch(routerSource, /import ErpSaleReturnManagement from '\.\.\/views\/erp\/ErpSaleReturnManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleOrderDraftManagement from '\.\.\/views\/erp\/ErpSaleOrderDraftManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleOrderApprovedManagement from '\.\.\/views\/erp\/ErpSaleOrderApprovedManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleReturnDraftManagement from '\.\.\/views\/erp\/ErpSaleReturnDraftManagement\.vue'/);
  assert.match(routerSource, /import ErpSaleReturnApprovedManagement from '\.\.\/views\/erp\/ErpSaleReturnApprovedManagement\.vue'/);

  assert.match(routerSource, /path: 'erp\/sale-orders\/draft'[\s\S]*?component: ErpSaleOrderDraftManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-orders\/approved'[\s\S]*?component: ErpSaleOrderApprovedManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-returns\/draft'[\s\S]*?component: ErpSaleReturnDraftManagement,/);
  assert.match(routerSource, /path: 'erp\/sale-returns\/approved'[\s\S]*?component: ErpSaleReturnApprovedManagement,/);
  assert.doesNotMatch(routerSource, /component: \(\) => import\('\.\.\/views\/erp\/ErpSaleOrderDraft\.vue'\)/);
  assert.doesNotMatch(routerSource, /component: \(\) => import\('\.\.\/views\/erp\/ErpSaleReturnDraft\.vue'\)/);
});

test('legacy sale shell files are not used by router for sale list first paint', () => {
  const routerSource = readRouter();
  const entries = [
    ['ErpSaleOrderDraft.vue', 'ErpSaleOrderDraftManagement.vue'],
    ['ErpSaleOrderApproved.vue', 'ErpSaleOrderApprovedManagement.vue'],
    ['ErpSaleReturnDraft.vue', 'ErpSaleReturnDraftManagement.vue'],
    ['ErpSaleReturnApproved.vue', 'ErpSaleReturnApprovedManagement.vue'],
  ];

  for (const [viewName, loadedPage] of entries) {
    const source = readView(viewName);
    assert.match(source, /<AsyncErpRouteShell/);
    assert.match(source, /:loader="loadView"/);
    assert.match(source, new RegExp(`import\\('\\./${loadedPage}'\\)`));
    assert.doesNotMatch(source, /<ErpSaleListToolbar/);
    assert.doesNotMatch(routerSource, new RegExp(`component: ${viewName.replace('.vue', '')},`));
  }
});

test('sale order approved page is split from draft-only logic', () => {
  const source = readView('ErpSaleOrderApprovedManagement.vue');

  assert.doesNotMatch(source, /ErpSaleOrderManagement/);
  assert.doesNotMatch(source, /openCreatePage/);
  assert.doesNotMatch(source, /openEditPage/);
  assert.doesNotMatch(source, /handleApprove/);
  assert.doesNotMatch(source, /handleDelete/);
  assert.doesNotMatch(source, /erp-sale-draft:edit/);
  assert.doesNotMatch(source, /erp-sale-draft:approve/);
  assert.doesNotMatch(source, /erp-sale-draft:delete/);
  assert.match(source, /\/erp\/sale-orders\/approved\/page/);
  assert.match(source, /erp-sale-approved/);
  assert.match(source, /<SaleOrderApprovedDeferredPanel[\s>]/);
  assert.match(source, /defineAsyncComponent/);
  assert.doesNotMatch(source, /DeferredSaleOrderPanel/);
});

test('sale order draft page is split from approved-only logic', () => {
  const source = readView('ErpSaleOrderDraftManagement.vue');

  assert.doesNotMatch(source, /ErpSaleOrderManagement/);
  assert.doesNotMatch(source, /handleCancel/);
  assert.doesNotMatch(source, /handleRedFlush/);
  assert.doesNotMatch(source, /erp-sale-approved:cancel/);
  assert.doesNotMatch(source, /erp-sale-approved:redflush/);
  assert.doesNotMatch(source, /returnStatus/);
  assert.match(source, /\/erp\/sale-orders\/draft\/page/);
  assert.match(source, /erp-sale-draft/);
  assert.match(source, /handleApprove/);
  assert.match(source, /handleDelete/);
  assert.match(source, /<SaleOrderDraftDeferredPanel[\s>]/);
  assert.match(source, /defineAsyncComponent/);
  assert.doesNotMatch(source, /DeferredSaleOrderPanel/);
});

test('sale return approved page is split from draft-only logic', () => {
  const source = readView('ErpSaleReturnApprovedManagement.vue');

  assert.doesNotMatch(source, /ErpSaleReturnManagement/);
  assert.doesNotMatch(source, /openCreatePage/);
  assert.doesNotMatch(source, /openEditPage/);
  assert.doesNotMatch(source, /handleApprove/);
  assert.doesNotMatch(source, /handleDelete/);
  assert.doesNotMatch(source, /erp-sale-return-draft:edit/);
  assert.doesNotMatch(source, /erp-sale-return-draft:approve/);
  assert.doesNotMatch(source, /erp-sale-return-draft:delete/);
  assert.match(source, /\/erp\/sale-returns\/approved\/page/);
  assert.match(source, /erp-sale-return-approved/);
  assert.match(source, /<SaleReturnApprovedDeferredPanel[\s>]/);
  assert.match(source, /defineAsyncComponent/);
  assert.doesNotMatch(source, /DeferredSaleOrderPanel/);
});

test('sale return draft page is split from approved-only logic', () => {
  const source = readView('ErpSaleReturnDraftManagement.vue');

  assert.doesNotMatch(source, /ErpSaleReturnManagement/);
  assert.doesNotMatch(source, /handleRedFlush/);
  assert.doesNotMatch(source, /erp-sale-return-approved:redflush/);
  assert.match(source, /\/erp\/sale-returns\/draft\/page/);
  assert.match(source, /erp-sale-return-draft/);
  assert.match(source, /handleApprove/);
  assert.match(source, /handleDelete/);
  assert.match(source, /<SaleReturnDraftDeferredPanel[\s>]/);
  assert.match(source, /defineAsyncComponent/);
  assert.doesNotMatch(source, /DeferredSaleOrderPanel/);
});

test('sale order and sale return real pages own first paint structure without deferred placeholder shells', () => {
  const saleOrderSources = [
    readView('ErpSaleOrderDraftManagement.vue'),
    readView('ErpSaleOrderApprovedManagement.vue'),
  ];
  const saleReturnSources = [
    readView('ErpSaleReturnDraftManagement.vue'),
    readView('ErpSaleReturnApprovedManagement.vue'),
  ];

  for (const source of [...saleOrderSources, ...saleReturnSources]) {
    assert.match(source, /<ErpSaleListToolbar/);
    assert.match(source, /(:loading="loading"|v-loading="loading")/);
    assert.match(source, /const pageTitle = computed/);
    assert.match(source, /route\.meta\.titleKey/);
    assert.doesNotMatch(source, /sale-order-deferred-placeholder/);
    assert.doesNotMatch(source, /DeferredErpListRoute/);
    assert.doesNotMatch(source, /AsyncErpRouteShell/);
  }

  assert.match(saleOrderSources[1], /\/erp\/sale-orders\/approved\/page/);
  assert.match(saleReturnSources[1], /\/erp\/sale-returns\/approved\/page/);
});

test('sale order and sale return keep data async while owning the real page table directly', () => {
  const saleOrderSources = [
    readView('ErpSaleOrderDraftManagement.vue'),
    readView('ErpSaleOrderApprovedManagement.vue'),
  ];
  const saleReturnSources = [
    readView('ErpSaleReturnDraftManagement.vue'),
    readView('ErpSaleReturnApprovedManagement.vue'),
  ];

  for (const source of [...saleOrderSources, ...saleReturnSources]) {
    const runRouteRefreshBlock = source.match(/const runRouteRefresh = \(\) => \{[\s\S]*?\n\};/)?.[0] ?? '';

    assert.doesNotMatch(source, /shallowRef<Component \| null>/);
    assert.doesNotMatch(source, /markRaw\(module\.default\)/);
    assert.doesNotMatch(runRouteRefreshBlock, /tableData\.value = \[\];/);
    assert.doesNotMatch(runRouteRefreshBlock, /total\.value = 0;/);
    assert.match(runRouteRefreshBlock, /void columnSettings\.fetchTenantKeys\(\);/);
    assert.match(runRouteRefreshBlock, /handleSearch\(\);/);
  }
});
