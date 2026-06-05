import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const srcRoot = join(viewsRoot, '..', '..');

const componentSource = readFileSync(join(viewsRoot, 'ErpStockWarningManagement.vue'), 'utf8');
const zhSource = readFileSync(join(srcRoot, 'locales', 'zh.ts'), 'utf8');
const enSource = readFileSync(join(srcRoot, 'locales', 'en.ts'), 'utf8');

test('stock warning page exposes fixed action toolbar with warehouse status and policy source filters', () => {
  assert.match(componentSource, /inventory-toolbar inventory-toolbar--fixed-actions/);
  assert.match(componentSource, /inventory-filters inventory-filters--stock-warning/);
  assert.match(componentSource, /inventory-actions inventory-actions--stock-warning/);
  assert.match(componentSource, /v-model="selectedWarehouseId"/);
  assert.match(componentSource, /v-model="selectedStatus"/);
  assert.match(componentSource, /v-model="selectedPolicySource"/);
  assert.match(componentSource, /\$t\('field\.warehouseScope'\)/);
  assert.match(componentSource, /\$t\('field\.status'\)/);
  assert.match(componentSource, /\$t\('field\.stockWarningLevel'\)/);
  assert.match(componentSource, /\$t\('action\.refresh'\)/);
  assert.match(componentSource, /\$t\('action\.search'\)/);
  assert.match(componentSource, /\$t\('action\.resetDefault'\)/);
  assert.match(componentSource, /\$t\('action\.manageAnomalies'\)/);
  assert.match(componentSource, /\.inventory-toolbar--fixed-actions\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto;/);
  assert.match(componentSource, /\.inventory-filters--stock-warning\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*wrap;/);
  assert.match(componentSource, /\.inventory-filters--stock-warning\s+>\s+\*\s*\{[\s\S]*flex:\s*0 0 180px;[\s\S]*min-width:\s*180px;/);
  assert.match(componentSource, /\.inventory-actions--stock-warning\s*\{[\s\S]*justify-content:\s*flex-end;[\s\S]*flex-wrap:\s*nowrap;/);
});

test('stock warning page renders safety stock policy source and anomaly governance entry', () => {
  assert.match(componentSource, /canShow\('safetyStock'\)/);
  assert.match(componentSource, /prop="safetyStock"/);
  assert.match(componentSource, /\$t\('field\.safetyStock'\)/);
  assert.match(componentSource, /canShow\('stockWarningLevel'\)/);
  assert.match(componentSource, /\$t\('field\.stockWarningLevel'\)/);
  assert.match(componentSource, /stockWarningLevelLabel\(row\)/);
  assert.match(componentSource, /canShow\('warehouseScope'\)/);
  assert.match(componentSource, /\$t\('field\.warehouseScope'\)/);
  assert.match(componentSource, /warehouseScopeLabel\(row\)/);
  assert.match(componentSource, /column-key="anomaly"/);
  assert.match(componentSource, /\$t\('field\.anomaly'\)/);
  assert.match(componentSource, /\$t\('field\.safetyStockReferenceHint'\)/);
});

test('stock warning scope filter includes product level and maps it to fallback policy query', () => {
  assert.match(componentSource, /const PRODUCT_LEVEL_SCOPE_VALUE = 'PRODUCT_LEVEL';/);
  assert.match(componentSource, /label: t\('field\.productLevel'\),\s*value: PRODUCT_LEVEL_SCOPE_VALUE/);
  assert.match(componentSource, /const applyScopeFilters = \(params: Record<string, any>\) =>/);
  assert.match(componentSource, /if \(selectedWarehouseId\.value === PRODUCT_LEVEL_SCOPE_VALUE\) \{\s*params\.policySource = 'PRODUCT_FALLBACK';/);
});

test('stock warning page prepares anomaly governance drawer and anomaly page request', () => {
  assert.match(componentSource, /<el-drawer/);
  assert.match(componentSource, /v-model="anomalyDrawerVisible"/);
  assert.match(componentSource, /\$t\('action\.manageAnomalies'\)/);
  assert.match(componentSource, /request\.get\('\/erp\/stock-warnings\/anomalies\/page'/);
  assert.match(componentSource, /const fetchAnomalyList = async \(\) =>/);
  assert.match(componentSource, /const openAnomalyDrawer = \(\) =>/);
  assert.match(componentSource, /const anomalyTableData = ref/);
});

test('stock warning warehouse filter loads full enabled warehouse options instead of deriving from warning rows', () => {
  assert.match(componentSource, /getCachedWarehouseOptions/);
  assert.match(componentSource, /await getCachedWarehouseOptions\(tenantCacheKey\.value\)/);
  assert.doesNotMatch(componentSource, /const syncWarehouseOptions = \(items: StockWarning\[\]\) =>/);
  assert.doesNotMatch(componentSource, /syncWarehouseOptions\(tableData\.value\)/);
});

test('stock warning locales clarify safety stock reference and anomaly governance copy', () => {
  assert.match(zhSource, /stockWarningLevel:\s*'预警层级'/);
  assert.match(zhSource, /warehouseScope:\s*'适用仓库'/);
  assert.match(zhSource, /productLevel:\s*'商品层级'/);
  assert.match(zhSource, /warehouseLevel:\s*'仓库层级'/);
  assert.match(zhSource, /productLevelScope:\s*'商品默认策略'/);
  assert.match(zhSource, /anomaly:\s*'异常'/);
  assert.match(zhSource, /stockWarningAnomalies:\s*'异常治理'/);
  assert.match(zhSource, /safetyStockReferenceHint:\s*'安全库存仅作为补货参考，不直接触发库存预警。'/);
  assert.match(enSource, /stockWarningLevel:\s*'Warning Level'/);
  assert.match(enSource, /warehouseScope:\s*'Warehouse Scope'/);
  assert.match(enSource, /productLevel:\s*'Product Level'/);
  assert.match(enSource, /warehouseLevel:\s*'Warehouse Level'/);
  assert.match(enSource, /productLevelScope:\s*'Product Default Policy'/);
  assert.match(enSource, /anomaly:\s*'Anomaly'/);
  assert.match(enSource, /stockWarningAnomalies:\s*'Anomaly Governance'/);
  assert.match(enSource, /safetyStockReferenceHint:\s*'Safety stock is only a replenishment reference and does not directly trigger stock warnings\.'/);
});
