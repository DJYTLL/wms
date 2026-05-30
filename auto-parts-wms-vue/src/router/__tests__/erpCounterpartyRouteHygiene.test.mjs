import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { test } from 'node:test';

const root = fileURLToPath(new URL('../../', import.meta.url));

const readSource = (relativePath) => readFileSync(join(root, relativePath), 'utf8');

test('supplier type management route is wired into ERP routes', () => {
  const routerSource = readSource('router/index.ts');

  assert.match(
    routerSource,
    /path: 'erp\/supplier-types'[\s\S]*?name: 'erp-supplier-types'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpSupplierTypeManagement\.vue'\)[\s\S]*?permission: 'erp-supplier-type:view'[\s\S]*?titleKey: 'page\.erpSupplierTypeManagement'/
  );
});

test('supplier type management component exists and calls the expected API', () => {
  const componentPath = join(root, 'views/erp/ErpSupplierTypeManagement.vue');
  assert.equal(existsSync(componentPath), true);

  const componentSource = readSource('views/erp/ErpSupplierTypeManagement.vue');
  assert.match(componentSource, /\$t\('page\.erpSupplierTypeManagement'\)/);
  assert.match(componentSource, /request\.get\('\/erp\/supplier-types'\)/);
  assert.match(componentSource, /request\.post\('\/erp\/supplier-types', payload\)/);
  assert.match(componentSource, /request\.put\(`\/erp\/supplier-types\/\$\{currentId\.value\}`,\s*payload\)/);
  assert.match(componentSource, /request\.delete\(`\/erp\/supplier-types\/\$\{row\.id\}`[\s\S]*?\)/);
  assert.match(componentSource, /v-permission="'erp-supplier-type:add'"/);
  assert.match(componentSource, /v-permission="'erp-supplier-type:edit'"/);
  assert.match(componentSource, /v-permission="'erp-supplier-type:delete'"/);
});

test('supplier type locale keys exist in both zh and en bundles', () => {
  const zhSource = readSource('locales/zh.ts');
  const enSource = readSource('locales/en.ts');

  assert.match(zhSource, /erpSupplierType: '供应商类型'/);
  assert.match(zhSource, /erpSupplierTypeManagement: '供应商类型管理'/);
  assert.match(enSource, /erpSupplierType: 'Supplier Types'/);
  assert.match(enSource, /erpSupplierTypeManagement: 'Supplier Type Management'/);
});

test('counterparty subject routes are wired into ERP routes', () => {
  const routerSource = readSource('router/index.ts');

  assert.match(
    routerSource,
    /path: 'erp\/counterparty-subjects'[\s\S]*?name: 'erp-counterparty-subjects'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpCounterpartySubjectManagement\.vue'\)[\s\S]*?permission: 'erp-counterparty-subject:view'[\s\S]*?titleKey: 'page\.erpCounterpartySubjectManagement'/
  );

  assert.match(
    routerSource,
    /path: 'erp\/finance\/counterparty-subjects'[\s\S]*?name: 'erp-finance-counterparty-subjects'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpCounterpartyFinanceSummary\.vue'\)[\s\S]*?permission: 'erp-finance-summary:view'[\s\S]*?titleKey: 'page\.erpCounterpartyFinanceSummary'/
  );
});

test('counterparty subject components exist and call the expected APIs', () => {
  const subjectComponentPath = join(root, 'views/erp/ErpCounterpartySubjectManagement.vue');
  const financeComponentPath = join(root, 'views/erp/ErpCounterpartyFinanceSummary.vue');
  assert.equal(existsSync(subjectComponentPath), true);
  assert.equal(existsSync(financeComponentPath), true);

  const subjectComponentSource = readSource('views/erp/ErpCounterpartySubjectManagement.vue');
  const financeComponentSource = readSource('views/erp/ErpCounterpartyFinanceSummary.vue');

  assert.match(subjectComponentSource, /\$t\('page\.erpCounterpartySubjectManagement'\)/);
  assert.match(subjectComponentSource, /request\.get\('\/erp\/counterparty-subjects'\)/);
  assert.match(subjectComponentSource, /request\.post\('\/erp\/counterparty-subjects', payload\)/);
  assert.match(subjectComponentSource, /request\.put\(`\/erp\/counterparty-subjects\/\$\{currentId\.value\}`,\s*payload\)/);
  assert.match(subjectComponentSource, /request\.delete\(`\/erp\/counterparty-subjects\/\$\{row\.id\}`/);
  assert.match(subjectComponentSource, /request\.get\(checkUrl\)/);
  assert.match(subjectComponentSource, /\/bind-supplier\/\$\{member\.id\}\/check/);
  assert.match(subjectComponentSource, /\/bind-customer\/\$\{member\.id\}\/check/);
  assert.match(subjectComponentSource, /router\.push\(\{ name: doc\.routeKey, params: \{ id: doc\.docId \} \}\)/);
  assert.match(subjectComponentSource, /v-permission="'erp-counterparty-subject:add'"/);
  assert.match(subjectComponentSource, /v-permission="'erp-counterparty-subject:edit'"/);
  assert.match(subjectComponentSource, /v-permission="'erp-counterparty-subject:delete'"/);

  assert.match(financeComponentSource, /\$t\('page\.erpCounterpartyFinanceSummary'\)/);
  assert.match(financeComponentSource, /request\.get\('\/erp\/finance\/counterparty-subjects\/summary'\)/);
});

test('counterparty subject locale keys exist in both zh and en bundles', () => {
  const zhSource = readSource('locales/zh.ts');
  const enSource = readSource('locales/en.ts');

  assert.match(zhSource, /erpCounterpartySubject: '往来主体'/);
  assert.match(zhSource, /erpCounterpartySubjectManagement: '往来主体管理'/);
  assert.match(zhSource, /erpCounterpartyFinanceSummary: '往来主体财务汇总'/);
  assert.match(enSource, /erpCounterpartySubject: 'Counterparty Subjects'/);
  assert.match(enSource, /erpCounterpartySubjectManagement: 'Counterparty Subject Management'/);
  assert.match(enSource, /erpCounterpartyFinanceSummary: 'Counterparty Subject Finance Summary'/);
});
