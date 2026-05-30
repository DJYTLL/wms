import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('customer management uses async import batches with polling and history drawer', () => {
  const source = readFileSync(join(viewsRoot, 'ErpCustomerManagement.vue'), 'utf8');

  assert.match(source, /v-permission="'erp-customer:import'"/);
  assert.match(source, /openCustomerImportDialog/);
  assert.match(source, /openCustomerImportHistoryDrawer/);
  assert.match(source, /ref="customerImportInputRef"/);
  assert.match(source, /accept="\.xls,\s*\.xlsx"/);
  assert.match(source, /request\.post\('\/erp\/customers\/import', formData\)/);
  assert.match(source, /request\.get\('\/erp\/customers\/import-batches'\)/);
  assert.match(source, /request\.get\(`\/erp\/customers\/import-batches\/\$\{batch\.id\}\/items`\)/);
  assert.match(source, /startCustomerImportPolling/);
  assert.match(source, /stopCustomerImportPolling/);
  assert.match(source, /setTimeout\(\(\) => \{\s*void pollCustomerImportBatch/);
  assert.match(source, /handleCustomerImportFile/);
  assert.match(source, /showCustomerImportHistoryDrawer/);
  assert.match(source, /selectedCustomerImportBatch/);
  assert.match(source, /customerImportBatchItems/);
  assert.match(source, /status === 'PROCESSING'/);
  assert.match(source, /<el-drawer v-model="showCustomerImportHistoryDrawer" title="客户导入结果"/);
});

test('customer management shows aggregated contact info instead of a truncated phone column', () => {
  const source = readFileSync(join(viewsRoot, 'ErpCustomerManagement.vue'), 'utf8');

  assert.match(source, /\$t\('field\.contactInfo'\)/);
  assert.match(source, /formatCustomerContactInfo/);
  assert.match(source, /getCustomerContactTokens/);
  assert.match(source, /isTruncatedMobilePrefix/);
  assert.match(source, /normalizeCustomerContactTokens/);
  assert.match(source, /filterByFuzzyKeyword\(filtered, phoneQuery\.value, row => \[/);
  assert.match(source, /formatCustomerContactInfo\(row\)/);
  assert.doesNotMatch(source, /<ErpDataTableColumn v-if="canShow\('phone'\)" prop="phone" :label="\$t\('field\.phone'\)"/);
});

test('customer management keeps spaced contact text together instead of splitting on spaces', () => {
  const source = readFileSync(join(viewsRoot, 'ErpCustomerManagement.vue'), 'utf8');

  assert.match(source, /const isTruncatedMobilePrefix = \(value: string, values: string\[\]\) =>/);
  assert.match(source, /const normalizeCustomerContactTokens = \(values: Array<string \| undefined>\) =>/);
  assert.doesNotMatch(source, /split.*\\s/);
});
