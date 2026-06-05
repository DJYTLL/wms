import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createJiti } from 'jiti';
import axios from 'axios';

import {
  createTenantScopedResourceCache
} from '../erpBaseDataCacheCore.ts';

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const srcDir = path.resolve(currentDir, '..', '..');
const erpBaseDataCachePath = path.resolve(srcDir, 'composables', 'erpBaseDataCache.ts');
const erpBaseDataCacheSource = fs.readFileSync(erpBaseDataCachePath, 'utf8');
const jiti = createJiti(import.meta.url, {
  alias: {
    '@': srcDir
  },
  interopDefault: true
});
const loadTsModule = (specifier) => jiti.import(specifier);

const erpBaseDataCacheModule = await loadTsModule(erpBaseDataCachePath);
const {
  ERP_BASE_DATA_RESOURCE_KEYS,
  getCachedCustomers,
  getCachedLocations,
  getCachedEnabledSettlementMethods,
  getCachedPaymentMethods,
  getCachedSettlementMethods,
  getCachedWarehouseOptions,
  getCachedWarehouses,
  invalidateErpBaseDataCache,
  invalidateErpBaseDataResourceCache
} = erpBaseDataCacheModule;

test('erp base data cache imports request through alias', () => {
  assert.match(erpBaseDataCacheSource, /import request from ['"]@\/utils\/request['"];?/);
});

test('resource invalidation mapping covers every getter cache key', () => {
  const resourceConstantEntries = Array.from(
    erpBaseDataCacheSource.matchAll(/const (RESOURCE_[A-Z_]+) = '([^']+)';/g),
    ([, constantName, resourceKey]) => [constantName, resourceKey]
  );
  const resourceConstants = Object.fromEntries(resourceConstantEntries);
  const getterResourceKeys = Array.from(
    erpBaseDataCacheSource.matchAll(/export const getCached\w+ =[\s\S]*?cache\.getOrLoad\((RESOURCE_[A-Z_]+)/g),
    ([, constantName]) => resourceConstants[constantName]
  );
  const mappedResourceKeys = new Set(Object.values(ERP_BASE_DATA_RESOURCE_KEYS).flat());

  assert.ok(getterResourceKeys.length > 0);
  assert.deepEqual(
    new Set(getterResourceKeys),
    mappedResourceKeys
  );
});

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

test('resource cache invalidation only clears the targeted resource for the same tenant', async () => {
  const originalRequest = axios.Axios.prototype.request;
  const counts = {
    customers: 0,
    locations: 0
  };

  axios.Axios.prototype.request = async function request(configOrUrl, maybeConfig) {
    const url = typeof configOrUrl === 'string'
      ? configOrUrl
      : (maybeConfig?.url ?? configOrUrl?.url);
    if (url === '/erp/customers') {
      counts.customers += 1;
      return { data: { data: [{ id: counts.customers, name: `客户${counts.customers}` }] } };
    }
    if (url === '/erp/locations') {
      counts.locations += 1;
      return { data: { data: [{ id: counts.locations, name: `库位${counts.locations}` }] } };
    }
    throw new Error(`unexpected url: ${url}`);
  };

  invalidateErpBaseDataCache();

  try {
    const firstCustomers = await getCachedCustomers(1);
    const firstLocations = await getCachedLocations(1);

    invalidateErpBaseDataResourceCache('customers', 1);

    const reloadedCustomers = await getCachedCustomers(1);
    const cachedLocations = await getCachedLocations(1);

    assert.deepEqual(firstCustomers, [{ id: 1, name: '客户1' }]);
    assert.deepEqual(reloadedCustomers, [{ id: 2, name: '客户2' }]);
    assert.deepEqual(firstLocations, [{ id: 1, name: '库位1' }]);
    assert.deepEqual(cachedLocations, [{ id: 1, name: '库位1' }]);
    assert.equal(counts.customers, 2);
    assert.equal(counts.locations, 1);
  } finally {
    axios.Axios.prototype.request = originalRequest;
    invalidateErpBaseDataCache();
  }
});

test('resource cache invalidation clears all mapped keys for the same resource type', async () => {
  const originalRequest = axios.Axios.prototype.request;
  const counts = {
    warehouses: 0,
    warehouseOptions: 0,
    locations: 0
  };

  axios.Axios.prototype.request = async function request(configOrUrl, maybeConfig) {
    const url = typeof configOrUrl === 'string'
      ? configOrUrl
      : (maybeConfig?.url ?? configOrUrl?.url);
    if (url === '/erp/warehouses') {
      counts.warehouses += 1;
      return { data: { data: [{ id: counts.warehouses, name: `仓库${counts.warehouses}` }] } };
    }
    if (url === '/erp/warehouses/options') {
      counts.warehouseOptions += 1;
      return { data: { data: [{ id: counts.warehouseOptions, name: `仓库选项${counts.warehouseOptions}` }] } };
    }
    if (url === '/erp/locations') {
      counts.locations += 1;
      return { data: { data: [{ id: counts.locations, name: `库位${counts.locations}` }] } };
    }
    throw new Error(`unexpected url: ${url}`);
  };

  invalidateErpBaseDataCache();

  try {
    await getCachedWarehouses(8);
    await getCachedWarehouseOptions(8);
    await getCachedLocations(8);

    invalidateErpBaseDataResourceCache('warehouses', 8);

    await getCachedWarehouses(8);
    await getCachedWarehouseOptions(8);
    await getCachedLocations(8);

    assert.equal(counts.warehouses, 2);
    assert.equal(counts.warehouseOptions, 2);
    assert.equal(counts.locations, 1);
  } finally {
    axios.Axios.prototype.request = originalRequest;
    invalidateErpBaseDataCache();
  }
});

test('resource cache invalidation does not clear unrelated resource types', async () => {
  const originalRequest = axios.Axios.prototype.request;
  const counts = {
    settlementMethods: 0,
    settlementMethodsEnabled: 0,
    paymentMethods: 0
  };

  axios.Axios.prototype.request = async function request(configOrUrl, maybeConfig) {
    const url = typeof configOrUrl === 'string'
      ? configOrUrl
      : (maybeConfig?.url ?? configOrUrl?.url);
    if (url === '/erp/settlement-methods') {
      counts.settlementMethods += 1;
      return { data: { data: [{ id: counts.settlementMethods, name: `结算方式${counts.settlementMethods}` }] } };
    }
    if (url === '/erp/settlement-methods?enabled=true') {
      counts.settlementMethodsEnabled += 1;
      return { data: { data: [{ id: counts.settlementMethodsEnabled, name: `启用结算${counts.settlementMethodsEnabled}` }] } };
    }
    if (url === '/erp/payment-methods') {
      counts.paymentMethods += 1;
      return { data: { data: [{ id: counts.paymentMethods, name: `付款方式${counts.paymentMethods}` }] } };
    }
    throw new Error(`unexpected url: ${url}`);
  };

  invalidateErpBaseDataCache();

  try {
    await getCachedSettlementMethods(9);
    await getCachedEnabledSettlementMethods(9);
    await getCachedPaymentMethods(9);

    invalidateErpBaseDataResourceCache('settlementMethods', 9);

    await getCachedSettlementMethods(9);
    await getCachedEnabledSettlementMethods(9);
    await getCachedPaymentMethods(9);

    assert.equal(counts.settlementMethods, 2);
    assert.equal(counts.settlementMethodsEnabled, 2);
    assert.equal(counts.paymentMethods, 1);
  } finally {
    axios.Axios.prototype.request = originalRequest;
    invalidateErpBaseDataCache();
  }
});
