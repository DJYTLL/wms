# ERP 主数据本地缓存定向失效 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为所有接入 `erpBaseDataCache` 的 ERP 主数据管理页补齐“写成功后只失效自身资源缓存”的能力。

**Architecture:** 先在缓存层增加按资源类型定向失效的统一 API，再让各主数据管理页在新增、编辑、删除成功后显式调用对应失效方法。测试分两层推进：先锁住缓存层的细粒度失效边界，再验证页面写操作只触发目标资源的缓存失效。

**Tech Stack:** Vue 3、TypeScript、Node `node:test`、现有 `request`/`useApiError`/`erpBaseDataCache` 组合式工具。

---

## File Structure

**Modify**

- `D:/project/auto-parts-wms-vue/src/composables/erpBaseDataCache.ts`
  - 继续承载 ERP 主数据缓存读取逻辑
  - 新增资源类型枚举/映射和按资源类型定向失效 API
- `D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs`
  - 补缓存层定向失效单测
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue`
  - 客户新增/编辑/删除成功后失效 `customers`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
  - 供应商新增/编辑/删除成功后失效 `suppliers`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpCategoryManagement.vue`
  - 商品分类新增/编辑/删除成功后失效 `categories`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerCategoryManagement.vue`
  - 客户分类新增/编辑/删除成功后失效 `customer-categories`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpUnitManagement.vue`
  - 单位新增/编辑/删除成功后失效 `units`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpSettlementMethodManagement.vue`
  - 结算方式新增/编辑/删除成功后失效 `settlement-methods` 与 `settlement-methods-enabled`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpPaymentMethodManagement.vue`
  - 付款方式新增/编辑/删除成功后失效 `payment-methods` 与 `payment-methods-enabled`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpReceiptMethodManagement.vue`
  - 收款方式新增/编辑/删除成功后失效 `receipt-methods` 与 `receipt-methods-enabled`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpDeliveryMethodManagement.vue`
  - 配送方式新增/编辑/删除成功后失效 `delivery-methods` 与 `delivery-methods-enabled`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue`
  - 仓库新增/编辑/删除成功后失效 `warehouses` 与 `warehouses-options`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpLocationManagement.vue`
  - 库位新增/编辑/删除成功后失效 `locations` 与 `locations-options`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue`
  - 商品新增/编辑/删除成功后失效 `products-options`
- `D:/project/auto-parts-wms-vue/src/views/erp/ErpVehicleFitmentManagement.vue`
  - 品牌/车系/车型新增/编辑/删除成功后分别失效 `vehicle-brands`、`vehicle-series`、`vehicle-models`

**Create**

- `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs`
  - 用源码断言方式锁住关键管理页已接入对应缓存失效函数，且不会误用全量失效

## Task 1: 缓存层定向失效单测

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs`

- [ ] **Step 1: 写失败测试，锁住多资源场景下的定向失效边界**

```js
test('tenant scoped cache can invalidate only the targeted resource for one tenant', async () => {
  let customerLoadCount = 0;
  let warehouseLoadCount = 0;
  const cache = createTenantScopedResourceCache();

  const loadCustomers = async () => {
    customerLoadCount += 1;
    return [{ id: customerLoadCount, name: `客户${customerLoadCount}` }];
  };
  const loadWarehouses = async () => {
    warehouseLoadCount += 1;
    return [{ id: warehouseLoadCount, name: `仓库${warehouseLoadCount}` }];
  };

  const firstCustomers = await cache.getOrLoad('customers', 1, loadCustomers);
  const firstWarehouses = await cache.getOrLoad('warehouses', 1, loadWarehouses);

  cache.invalidate('customers', 1);

  const reloadedCustomers = await cache.getOrLoad('customers', 1, loadCustomers);
  const cachedWarehouses = await cache.getOrLoad('warehouses', 1, loadWarehouses);

  assert.deepEqual(firstCustomers, [{ id: 1, name: '客户1' }]);
  assert.deepEqual(reloadedCustomers, [{ id: 2, name: '客户2' }]);
  assert.deepEqual(firstWarehouses, [{ id: 1, name: '仓库1' }]);
  assert.deepEqual(cachedWarehouses, [{ id: 1, name: '仓库1' }]);
  assert.equal(customerLoadCount, 2);
  assert.equal(warehouseLoadCount, 1);
});
```

- [ ] **Step 2: 运行单测，确认新增断言先失败**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs
```

Expected: 至少一个新增断言失败，提示当前只有底层 `invalidate(resource, tenantId)`，还没有业务语义层测试覆盖。

- [ ] **Step 3: 在同一测试文件补第二组失败用例，锁住“同类多 key 失效但不误清他类”的规则**

```js
test('resource invalidation mapping should clear related keys only', async () => {
  const cache = createTenantScopedResourceCache();
  let settlementLoads = 0;
  let settlementEnabledLoads = 0;
  let paymentLoads = 0;

  await cache.getOrLoad('settlement-methods', 9, async () => {
    settlementLoads += 1;
    return [{ id: settlementLoads, name: '现结' }];
  });
  await cache.getOrLoad('settlement-methods-enabled', 9, async () => {
    settlementEnabledLoads += 1;
    return [{ id: settlementEnabledLoads, name: '现结' }];
  });
  await cache.getOrLoad('payment-methods', 9, async () => {
    paymentLoads += 1;
    return [{ id: paymentLoads, name: '现金' }];
  });

  // 这里先占位调用未来在 erpBaseDataCache.ts 暴露的业务失效 API
  assert.equal(typeof invalidateErpBaseDataResourceCache, 'function');
});
```

- [ ] **Step 4: 再次运行单测，确认对未来导出 API 的引用失败**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs
```

Expected: FAIL，报 `invalidateErpBaseDataResourceCache is not defined` 或导入失败。

- [ ] **Step 5: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs
git commit -m "test: 锁定主数据缓存定向失效边界"
```

## Task 2: 实现缓存层定向失效 API

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/composables/erpBaseDataCache.ts`
- Modify: `D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs`

- [ ] **Step 1: 在缓存层补资源类型与资源 key 映射**

```ts
export const ERP_BASE_DATA_RESOURCE_KEYS = {
  customers: [RESOURCE_CUSTOMERS],
  suppliers: [RESOURCE_SUPPLIERS],
  categories: [RESOURCE_CATEGORIES],
  customerCategories: [RESOURCE_CUSTOMER_CATEGORIES],
  units: [RESOURCE_UNITS],
  settlementMethods: [RESOURCE_SETTLEMENT_METHODS, RESOURCE_SETTLEMENT_METHODS_ENABLED],
  paymentMethods: [RESOURCE_PAYMENT_METHODS, RESOURCE_PAYMENT_METHODS_ENABLED],
  receiptMethods: [RESOURCE_RECEIPT_METHODS, RESOURCE_RECEIPT_METHODS_ENABLED],
  deliveryMethods: [RESOURCE_DELIVERY_METHODS, RESOURCE_DELIVERY_METHODS_ENABLED],
  warehouses: [RESOURCE_WAREHOUSES, RESOURCE_WAREHOUSES_OPTIONS],
  locations: [RESOURCE_LOCATIONS, RESOURCE_LOCATIONS_OPTIONS],
  productOptions: [RESOURCE_PRODUCTS_OPTIONS],
  vehicleBrands: [RESOURCE_VEHICLE_BRANDS],
  vehicleSeries: [RESOURCE_VEHICLE_SERIES],
  vehicleModels: [RESOURCE_VEHICLE_MODELS]
} as const;

export type ErpBaseDataResourceType = keyof typeof ERP_BASE_DATA_RESOURCE_KEYS;
```

- [ ] **Step 2: 实现按资源类型定向失效方法，并保留原有全量失效入口**

```ts
export const invalidateErpBaseDataResourceCache = (
  resourceType: ErpBaseDataResourceType,
  tenantId?: number | string
) => {
  const resourceKeys = ERP_BASE_DATA_RESOURCE_KEYS[resourceType];
  resourceKeys.forEach((resourceKey) => {
    cache.invalidate(resourceKey, tenantId);
  });
};

export const invalidateErpBaseDataCache = (tenantId?: number | string) => {
  cache.invalidate(undefined, tenantId);
};
```

- [ ] **Step 3: 更新测试导入并把占位测试改成真实断言**

```js
import {
  createTenantScopedResourceCache
} from '../erpBaseDataCacheCore.ts';
import {
  invalidateErpBaseDataResourceCache
} from '../erpBaseDataCache.ts';
```

```js
test('resource invalidation mapping should clear related keys only', async () => {
  let settlementLoads = 0;
  let settlementEnabledLoads = 0;
  let paymentLoads = 0;

  await getCachedSettlementMethods(9);
  await getCachedEnabledSettlementMethods(9);
  await getCachedPaymentMethods(9);

  invalidateErpBaseDataResourceCache('settlementMethods', 9);

  await getCachedSettlementMethods(9);
  await getCachedEnabledSettlementMethods(9);
  await getCachedPaymentMethods(9);

  assert.equal(settlementLoads, 2);
  assert.equal(settlementEnabledLoads, 2);
  assert.equal(paymentLoads, 1);
});
```

- [ ] **Step 4: 运行缓存层单测，确认通过**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/composables/erpBaseDataCache.ts D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs
git commit -m "feat: 增加主数据缓存定向失效"
```

## Task 3: 锁住关键页面已接入定向失效

**Files:**
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs`

- [ ] **Step 1: 写失败测试，先锁商品/仓库/库位三个高频页面**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(path, 'utf8');

test('product management invalidates product options cache after save/delete', () => {
  const source = read('D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue');
  assert.match(source, /invalidateErpBaseDataResourceCache\('productOptions'/);
});

test('warehouse management invalidates warehouse cache after save/delete', () => {
  const source = read('D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue');
  assert.match(source, /invalidateErpBaseDataResourceCache\('warehouses'/);
});

test('location management invalidates location cache after save/delete', () => {
  const source = read('D:/project/auto-parts-wms-vue/src/views/erp/ErpLocationManagement.vue');
  assert.match(source, /invalidateErpBaseDataResourceCache\('locations'/);
});
```

- [ ] **Step 2: 增加反例断言，防止误用全量失效**

```js
test('management pages do not use broad master cache invalidation after write operations', () => {
  const productSource = read('D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue');
  const warehouseSource = read('D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue');
  assert.doesNotMatch(productSource, /invalidateErpBaseDataCache\(/);
  assert.doesNotMatch(warehouseSource, /invalidateErpBaseDataCache\(/);
});
```

- [ ] **Step 3: 运行页面接入测试，确认先失败**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
```

Expected: FAIL，页面源码里还没有对应失效调用。

- [ ] **Step 4: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
git commit -m "test: 锁定主数据页面缓存刷新接入"
```

## Task 4: 接入商品、仓库、库位页面

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpLocationManagement.vue`

- [ ] **Step 1: 在三个页面引入定向失效 API**

```ts
import { invalidateErpBaseDataResourceCache } from '@/composables/erpBaseDataCache';
```

- [ ] **Step 2: 商品页面在保存成功与删除成功后只失效商品缓存**

```ts
if (res.data.code === 200) {
  invalidateErpBaseDataResourceCache('productOptions');
  notifySuccess();
  showModal.value = false;
  fetchList();
}
```

```ts
await request.delete(`/erp/products/${row.id}`);
invalidateErpBaseDataResourceCache('productOptions');
notifySuccess();
fetchList();
```

- [ ] **Step 3: 仓库页面在保存成功与删除成功后只失效仓库缓存**

```ts
if (res.data.code === 200) {
  invalidateErpBaseDataResourceCache('warehouses');
  notifySuccess();
  showModal.value = false;
  fetchList();
}
```

```ts
await request.delete(`/erp/warehouses/${row.id}`);
invalidateErpBaseDataResourceCache('warehouses');
notifySuccess();
fetchList();
```

- [ ] **Step 4: 库位页面在保存成功与删除成功后只失效库位缓存**

```ts
if (res.data.code === 200) {
  invalidateErpBaseDataResourceCache('locations');
  notifySuccess();
  showModal.value = false;
  fetchList();
}
```

```ts
await request.delete(`/erp/locations/${row.id}`);
invalidateErpBaseDataResourceCache('locations');
notifySuccess();
fetchList();
```

- [ ] **Step 5: 运行页面接入测试，确认三页通过**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
```

Expected: 与商品/仓库/库位相关的断言 PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpLocationManagement.vue
git commit -m "feat: 刷新商品仓库库位缓存"
```

## Task 5: 接入客户、供应商、分类、单位页面

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpCategoryManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerCategoryManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpUnitManagement.vue`

- [ ] **Step 1: 在五个页面引入定向失效 API**

```ts
import { invalidateErpBaseDataResourceCache } from '@/composables/erpBaseDataCache';
```

- [ ] **Step 2: 客户页面在保存和删除成功后失效 `customers`**

```ts
if (res.data.code === 200) {
  invalidateErpBaseDataResourceCache('customers');
  notifySuccess();
  showModal.value = false;
  fetchList();
}
```

- [ ] **Step 3: 供应商、商品分类、客户分类、单位页面分别接入自身资源失效**

```ts
invalidateErpBaseDataResourceCache('suppliers');
invalidateErpBaseDataResourceCache('categories');
invalidateErpBaseDataResourceCache('customerCategories');
invalidateErpBaseDataResourceCache('units');
```

- [ ] **Step 4: 运行页面接入测试并补充对应源码断言**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
```

Expected: 新增的客户/供应商/分类/单位断言 PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpCategoryManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerCategoryManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpUnitManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
git commit -m "feat: 刷新客户供应商分类单位缓存"
```

## Task 6: 接入方式类主数据页面

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSettlementMethodManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPaymentMethodManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpReceiptMethodManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpDeliveryMethodManagement.vue`

- [ ] **Step 1: 在四个页面引入定向失效 API**

```ts
import { invalidateErpBaseDataResourceCache } from '@/composables/erpBaseDataCache';
```

- [ ] **Step 2: 保存成功后分别失效自身资源类型**

```ts
invalidateErpBaseDataResourceCache('settlementMethods');
invalidateErpBaseDataResourceCache('paymentMethods');
invalidateErpBaseDataResourceCache('receiptMethods');
invalidateErpBaseDataResourceCache('deliveryMethods');
```

- [ ] **Step 3: 删除成功后复用相同资源类型失效，不调用全量失效**

```ts
await request.delete(`/erp/settlement-methods/${row.id}`);
invalidateErpBaseDataResourceCache('settlementMethods');
```

- [ ] **Step 4: 更新页面接入测试，增加“方式类页面不误清其他方式缓存”的源码断言**

```js
assert.match(source, /invalidateErpBaseDataResourceCache\('settlementMethods'/);
assert.doesNotMatch(source, /invalidateErpBaseDataResourceCache\('paymentMethods'/);
```

- [ ] **Step 5: 运行测试**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
```

Expected: 方式类页面源码断言 PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/ErpSettlementMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpPaymentMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpReceiptMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpDeliveryMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
git commit -m "feat: 刷新主数据方式缓存"
```

## Task 7: 接入车型主数据页面

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpVehicleFitmentManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs`

- [ ] **Step 1: 识别品牌、车系、车型三组保存/删除入口并引入定向失效 API**

```ts
import { invalidateErpBaseDataResourceCache } from '@/composables/erpBaseDataCache';
```

- [ ] **Step 2: 品牌保存/删除后只失效 `vehicleBrands`，车系只失效 `vehicleSeries`，车型只失效 `vehicleModels`**

```ts
invalidateErpBaseDataResourceCache('vehicleBrands');
invalidateErpBaseDataResourceCache('vehicleSeries');
invalidateErpBaseDataResourceCache('vehicleModels');
```

- [ ] **Step 3: 补源码断言测试，防止三个入口复用同一个错误资源键**

```js
assert.match(source, /invalidateErpBaseDataResourceCache\('vehicleBrands'/);
assert.match(source, /invalidateErpBaseDataResourceCache\('vehicleSeries'/);
assert.match(source, /invalidateErpBaseDataResourceCache\('vehicleModels'/);
```

- [ ] **Step 4: 运行测试**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/ErpVehicleFitmentManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
git commit -m "feat: 刷新车型主数据缓存"
```

## Task 8: 全量验证与收尾

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs`
  - 如前面任务执行后有遗漏，在这里集中补齐剩余页面断言

- [ ] **Step 1: 运行缓存层与页面接入测试**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
```

Expected: PASS。

- [ ] **Step 2: 运行前端类型检查**

Run:

```bash
npm --prefix D:/project/auto-parts-wms-vue run type-check
```

Expected: PASS。

- [ ] **Step 3: 查看变更范围，确认未误碰无关文件**

Run:

```bash
git -C D:/project status --short
```

Expected: 只新增计划内文件改动，保留用户原有未提交修改不被覆盖。

- [ ] **Step 4: 提交本任务**

```bash
git add D:/project/auto-parts-wms-vue/src/composables/erpBaseDataCache.ts D:/project/auto-parts-wms-vue/src/composables/__tests__/erpBaseDataCache.test.mjs D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpCategoryManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerCategoryManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpUnitManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpSettlementMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpPaymentMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpReceiptMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpDeliveryMethodManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpLocationManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpVehicleFitmentManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpBaseDataCacheInvalidationHooks.test.mjs
git commit -m "feat: 补齐主数据缓存定向刷新"
```

## Self-Review

- Spec coverage:
  - 缓存层资源映射与定向失效：Task 1-2
  - 页面写成功后显式调用：Task 4-7
  - 只清当前资源，不清别的类型：Task 1-3、Task 6-7
  - 测试与类型检查：Task 1、Task 3、Task 8
- Placeholder scan:
  - 已移除 `TODO/TBD` 类占位描述
- Type consistency:
  - 计划统一使用 `invalidateErpBaseDataResourceCache(resourceType, tenantId?)`
  - 资源类型命名统一为 `customers/suppliers/categories/customerCategories/units/settlementMethods/paymentMethods/receiptMethods/deliveryMethods/warehouses/locations/productOptions/vehicleBrands/vehicleSeries/vehicleModels`
