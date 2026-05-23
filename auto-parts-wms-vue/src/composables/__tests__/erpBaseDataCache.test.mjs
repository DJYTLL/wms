import test from 'node:test';
import assert from 'node:assert/strict';

import {
  createTenantScopedResourceCache
} from '../erpBaseDataCacheCore.ts';

test('tenant scoped cache reuses inflight loader and cached value for same tenant', async () => {
  let loadCount = 0;
  const cache = createTenantScopedResourceCache();
  const loader = async () => {
    loadCount += 1;
    return [{ id: 1, name: '客户A' }];
  };

  const [first, second, third] = await Promise.all([
    cache.getOrLoad('customers', 11, loader),
    cache.getOrLoad('customers', 11, loader),
    cache.getOrLoad('customers', 11, loader)
  ]);

  assert.equal(loadCount, 1);
  assert.deepEqual(first, [{ id: 1, name: '客户A' }]);
  assert.deepEqual(second, first);
  assert.deepEqual(third, first);
});

test('tenant scoped cache isolates records by tenant and supports invalidation', async () => {
  let loadCount = 0;
  const cache = createTenantScopedResourceCache();
  const loader = async () => {
    loadCount += 1;
    return [{ id: loadCount, name: `客户${loadCount}` }];
  };

  const tenantOneFirst = await cache.getOrLoad('customers', 1, loader);
  const tenantTwoFirst = await cache.getOrLoad('customers', 2, loader);
  cache.invalidate('customers', 1);
  const tenantOneReloaded = await cache.getOrLoad('customers', 1, loader);

  assert.equal(loadCount, 3);
  assert.deepEqual(tenantOneFirst, [{ id: 1, name: '客户1' }]);
  assert.deepEqual(tenantTwoFirst, [{ id: 2, name: '客户2' }]);
  assert.deepEqual(tenantOneReloaded, [{ id: 3, name: '客户3' }]);
});
