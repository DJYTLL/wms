import test from 'node:test';
import assert from 'node:assert/strict';

import { resetTabsForTenantSwitch } from '../tenantSwitchTabs.ts';

test('resetTabsForTenantSwitch closes existing tabs and leaves only a fresh dashboard tab', () => {
  const visitedViews = [
    { key: 'erp-products', title: '商品管理', path: '/erp/products' },
    { key: 'erp-sale-orders-draft', title: '销售单草稿', path: '/erp/sale-orders/draft' },
  ];
  const viewKeyVersions = {
    '/erp/products': 2,
  };
  const closedPaths = [];

  resetTabsForTenantSwitch({
    visitedViews,
    viewKeyVersions,
    currentPath: '/erp/sale-orders/draft',
    homeView: { key: 'dashboard', title: '仪表盘', path: '/' },
    onClosePath: (path) => closedPaths.push(path),
  });

  assert.deepEqual(closedPaths, ['/erp/products', '/erp/sale-orders/draft']);
  assert.deepEqual(visitedViews, [{ key: 'dashboard', title: '仪表盘', path: '/' }]);
  assert.equal(viewKeyVersions['/erp/products'], 3);
  assert.equal(viewKeyVersions['/erp/sale-orders/draft'], 1);
  assert.equal(viewKeyVersions['/'], 1);
});
