import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const srcRoot = join(viewsRoot, '..', '..');

const financeToolbarViews = [
  'ErpCustomerDebtManagement.vue',
  'ErpSupplierDebtManagement.vue',
  'ErpCounterpartyFinanceSummary.vue',
  'ErpAccountsReceivableManagement.vue',
  'ErpAccountsPayableManagement.vue'
];

const tableStyleSource = readFileSync(join(srcRoot, 'styles', 'table.css'), 'utf8');

test('finance management toolbars keep fixed right-side actions with refresh button', () => {
  for (const viewName of financeToolbarViews) {
    const componentSource = readFileSync(join(viewsRoot, viewName), 'utf8');

    assert.match(componentSource, /class="table-toolbar finance-toolbar finance-toolbar--fixed-actions"/, `${viewName} should use fixed-action finance toolbar`);
    assert.match(componentSource, /class="table-filters finance-filters/, `${viewName} should keep filter area`);
    assert.match(componentSource, /class="finance-actions"/, `${viewName} should define finance actions area`);
    assert.match(componentSource, /@click="handleRefresh"/, `${viewName} should expose refresh handler`);
    assert.match(componentSource, /\$t\('action\.refresh'\)/, `${viewName} should render refresh button label`);
    assert.match(componentSource, /\$t\('action\.search'\)/, `${viewName} should keep search action`);
  }

  assert.match(tableStyleSource, /\.finance-toolbar--fixed-actions\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto;/);
  assert.match(tableStyleSource, /\.finance-toolbar--fixed-actions\s+\.finance-filters\s*\{[\s\S]*display:\s*grid;[\s\S]*min-width:\s*0;/);
  assert.match(tableStyleSource, /\.finance-toolbar--fixed-actions\s+\.finance-actions\s*\{[\s\S]*justify-content:\s*flex-end;[\s\S]*flex-wrap:\s*nowrap;/);
  assert.match(tableStyleSource, /@media \(max-width:\s*1280px\)\s*\{[\s\S]*\.finance-toolbar--fixed-actions\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto;/);
  assert.match(tableStyleSource, /@media \(max-width:\s*768px\)\s*\{[\s\S]*\.finance-toolbar--fixed-actions\s*\{[\s\S]*grid-template-columns:\s*1fr;/);
});
