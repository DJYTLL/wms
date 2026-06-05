import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

const basicPages = [
  'ErpProductManagement.vue',
  'ErpVehicleFitmentManagement.vue',
  'ErpCustomerManagement.vue',
  'ErpCustomerCategoryManagement.vue',
  'ErpSupplierManagement.vue',
  'ErpSupplierTypeManagement.vue',
  'ErpCounterpartySubjectManagement.vue',
  'ErpWarehouseManagement.vue',
  'ErpLocationManagement.vue',
  'ErpCategoryManagement.vue',
  'ErpUnitManagement.vue',
  'ErpSettlementMethodManagement.vue',
  'ErpPaymentMethodManagement.vue',
  'ErpReceiptMethodManagement.vue',
  'ErpDeliveryMethodManagement.vue',
  'ErpPrintTemplateManagement.vue'
];

test('ERP basic pages share a first-paint wait helper before opening data requests', () => {
  const helperPath = join(viewsRoot, 'erpFirstPaint.ts');

  assert.equal(existsSync(helperPath), true, 'erpFirstPaint.ts should exist');

  const helperSource = readView('erpFirstPaint.ts');
  assert.match(helperSource, /waitForErpFirstPaint/, 'helper should export a named ERP first-paint wait');
  assert.match(helperSource, /requestAnimationFrame/, 'browser first paint should use requestAnimationFrame');
  assert.match(helperSource, /setTimeout/, 'non-browser fallback should yield to the event loop');
});

test('ERP basic pages wait for first paint before mounted data loading', () => {
  for (const page of basicPages) {
    const source = readView(page);
    const mountedBlock = source.match(/onMounted\(async \(\) => \{[\s\S]*?\n\}\);/)?.[0] ?? '';

    assert.match(source, /import \{ waitForErpFirstPaint \} from '\.\/erpFirstPaint';/, `${page} should import waitForErpFirstPaint`);
    assert.match(mountedBlock, /await\s+waitForErpFirstPaint\(\);/, `${page} should wait for first paint in onMounted`);
    assert.match(
      mountedBlock,
      /await\s+waitForErpFirstPaint\(\);[\s\S]*?(fetch|load|init)[A-Z]/,
      `${page} should start initial data loading only after first paint`
    );
    assert.doesNotMatch(
      mountedBlock,
      /onMounted\(async \(\) => \{\s*(fetch|load|init)[A-Z]/,
      `${page} should not start mounted data loading before first paint`
    );
  }
});

test('ERP basic page-size sync callbacks cannot trigger initial list loading before first paint', () => {
  for (const page of basicPages) {
    const source = readView(page);

    assert.match(source, /firstPaintReady\s*=\s*ref\(false\)/, `${page} should track first-paint readiness`);
    assert.match(source, /firstPaintReady\.value\s*=\s*true/, `${page} should mark first paint ready after waiting`);

    const syncCallbackBlocks = source.match(/onInitialSyncComplete:\s*(?:\(\) =>|handlePageSizeSyncReady)[\s\S]*?\n\s*\}/g) ?? [];
    if (syncCallbackBlocks.length === 0 && !source.includes('handlePageSizeSyncReady')) continue;

    assert.match(
      source,
      /pendingInitialLoad\.value\s*&&\s*firstPaintReady\.value|firstPaintReady\.value\s*&&\s*pendingInitialLoad\.value/,
      `${page} should guard pending initial loading by firstPaintReady`
    );
  }
});
