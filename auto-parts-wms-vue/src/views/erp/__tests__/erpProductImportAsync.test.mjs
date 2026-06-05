import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('product management uses async import batches with polling and history drawer', () => {
  const source = readFileSync(join(viewsRoot, 'ErpProductManagement.vue'), 'utf8');

  assert.match(source, /openProductImportDialog/);
  assert.match(source, /openProductImportHistoryDrawer/);
  assert.match(source, /request\.post\('\/erp\/products\/import\/preview', formData\)/);
  assert.match(source, /request\.post\('\/erp\/products\/import', formData\)/);
  assert.match(source, /formData\.append\('fieldMapping', JSON\.stringify\(buildProductImportFieldMapping\(\)\)\)/);
  assert.match(source, /productImportPreview/);
  assert.match(source, /productImportFieldOptions/);
  assert.match(source, /productImportMappings/);
  assert.match(source, /productImportMappingConfirmed/);
  assert.match(source, /confirmProductImportMapping/);
  assert.match(source, /validateProductImportRequiredMappings/);
  assert.match(source, /if \(!productImportMappingConfirmed\.value\)/);
  assert.match(
    source,
    /const confirmProductImportMapping = \(\) => \{[\s\S]*?productImportMappingConfirmed\.value = true;[\s\S]*?\};/
  );
  assert.doesNotMatch(
    source.match(/const confirmProductImportMapping = \(\) => \{[\s\S]*?\n\};/)?.[0] || '',
    /if \(!productImportMappingConfirmed\.value\)/
  );
  assert.match(
    source,
    /const submitProductImport = async \(\) => \{[\s\S]*?if \(!productImportMappingConfirmed\.value\)[\s\S]*?request\.post\('\/erp\/products\/import', formData\)/
  );
  assert.match(source, /@change="handleProductImportMappingChange\(row\)"/);
  assert.match(source, /@click="confirmProductImportMapping"/);
  assert.match(source, /@click="submitProductImport"/);
  assert.match(source, /<el-table :data="productImportMappings"/);
  assert.match(source, /<el-select v-model="row.fieldKey"/);
  assert.match(source, /request\.get\('\/erp\/products\/import-batches'\)/);
  assert.match(source, /request\.get\(`\/erp\/products\/import-batches\/\$\{batch\.id\}\/items`\)/);
  assert.match(source, /startProductImportPolling/);
  assert.match(source, /stopProductImportPolling/);
  assert.match(source, /setTimeout\(\(\) => \{\s*void pollProductImportBatch/);
  assert.match(source, /showProductImportHistoryDrawer/);
  assert.match(source, /selectedProductImportBatch/);
  assert.match(source, /productImportBatchItems/);
  assert.match(source, /status === 'PROCESSING'/);
  assert.match(source, /<el-drawer v-model="showProductImportHistoryDrawer" title="配件导入结果"/);
  assert.match(source, /warningMessage/);
  assert.match(source, /errorMessage/);
});
