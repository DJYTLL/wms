import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const erpViewsDir = path.resolve(currentDir, '..');

const readViewSource = (filename) => (
  fs.readFileSync(path.resolve(erpViewsDir, filename), 'utf8')
);

const assertUsesTargetedInvalidation = (source, resourceType) => {
  const targetedCallPattern = new RegExp(
    `invalidateErpBaseDataResourceCache\\('${resourceType}',\\s*tenantCacheKey\\.value\\)`,
    'g'
  );
  const targetedCalls = source.match(targetedCallPattern) || [];

  assert.ok(
    source.includes('invalidateErpBaseDataResourceCache'),
    `应引入 invalidateErpBaseDataResourceCache，并为 ${resourceType} 做定向失效`
  );
  assert.equal(
    targetedCalls.length,
    2,
    `应在保存成功和删除成功路径各调用一次 ${resourceType} 定向失效`
  );
  assert.doesNotMatch(
    source,
    /invalidateErpBaseDataCache\s*\(/,
    '不应误用 invalidateErpBaseDataCache 做全量失效'
  );
};

test('商品页接入商品选项缓存定向失效', () => {
  const source = readViewSource('ErpProductManagement.vue');
  assertUsesTargetedInvalidation(source, 'productOptions');
});

test('仓库页接入仓库缓存定向失效', () => {
  const source = readViewSource('ErpWarehouseManagement.vue');
  assertUsesTargetedInvalidation(source, 'warehouses');
});

test('库位页接入库位缓存定向失效', () => {
  const source = readViewSource('ErpLocationManagement.vue');
  assertUsesTargetedInvalidation(source, 'locations');
});

test('客户页接入客户缓存定向失效', () => {
  const source = readViewSource('ErpCustomerManagement.vue');
  assertUsesTargetedInvalidation(source, 'customers');
});

test('供应商页接入供应商缓存定向失效', () => {
  const source = readViewSource('ErpSupplierManagement.vue');
  assertUsesTargetedInvalidation(source, 'suppliers');
});

test('分类页接入分类缓存定向失效', () => {
  const source = readViewSource('ErpCategoryManagement.vue');
  assertUsesTargetedInvalidation(source, 'categories');
});

test('客户分类页接入客户分类缓存定向失效', () => {
  const source = readViewSource('ErpCustomerCategoryManagement.vue');
  assertUsesTargetedInvalidation(source, 'customerCategories');
});

test('单位页接入单位缓存定向失效', () => {
  const source = readViewSource('ErpUnitManagement.vue');
  assertUsesTargetedInvalidation(source, 'units');
});

test('结算方式页接入结算方式缓存定向失效', () => {
  const source = readViewSource('ErpSettlementMethodManagement.vue');
  assertUsesTargetedInvalidation(source, 'settlementMethods');
});

test('付款方式页接入付款方式缓存定向失效', () => {
  const source = readViewSource('ErpPaymentMethodManagement.vue');
  assertUsesTargetedInvalidation(source, 'paymentMethods');
});

test('收款方式页接入收款方式缓存定向失效', () => {
  const source = readViewSource('ErpReceiptMethodManagement.vue');
  assertUsesTargetedInvalidation(source, 'receiptMethods');
});

test('交货方式页接入交货方式缓存定向失效', () => {
  const source = readViewSource('ErpDeliveryMethodManagement.vue');
  assertUsesTargetedInvalidation(source, 'deliveryMethods');
});

test('车辆适配页品牌操作接入品牌缓存定向失效', () => {
  const source = readViewSource('ErpVehicleFitmentManagement.vue');
  assertUsesTargetedInvalidation(source, 'vehicleBrands');
});

test('车辆适配页车系操作接入车系缓存定向失效', () => {
  const source = readViewSource('ErpVehicleFitmentManagement.vue');
  assertUsesTargetedInvalidation(source, 'vehicleSeries');
});

test('车辆适配页车型操作接入车型缓存定向失效', () => {
  const source = readViewSource('ErpVehicleFitmentManagement.vue');
  assertUsesTargetedInvalidation(source, 'vehicleModels');
});
