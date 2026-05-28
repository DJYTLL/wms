import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');

test('supplier dialog uses grouped redesign structure with optional collapsed sections and sticky footer', () => {
  const componentSource = readView('ErpSupplierManagement.vue');

  assert.match(componentSource, /class="supplier-dialog"/);
  assert.match(componentSource, /class="supplier-dialog__intro"/);
  assert.match(componentSource, /class="supplier-status-panel"/);
  assert.match(componentSource, /supplier-section__title">\s*\{\{\s*\$t\('supplierDialog\.sectionBasic'\)\s*\}\}/);
  assert.match(componentSource, /supplier-section__title">\s*\{\{\s*\$t\('supplierDialog\.sectionContact'\)\s*\}\}/);
  assert.match(componentSource, /supplier-section__title">\s*\{\{\s*\$t\('supplierDialog\.sectionBusiness'\)\s*\}\}/);
  assert.match(componentSource, /:aria-expanded="optionalSectionsExpanded"/);
  assert.match(componentSource, /\{\{\s*optionalToggleLabel\s*\}\}/);
  assert.match(componentSource, /\$t\('supplierDialog\.optionalToggleHint'\)/);
  assert.match(componentSource, /supplier-dialog__footer/);
  assert.match(componentSource, /\$t\('supplierDialog\.counterpartyHint'\)/);
});
