import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { test } from 'node:test';

const root = fileURLToPath(new URL('../../', import.meta.url));

const readSource = (relativePath) => readFileSync(join(root, relativePath), 'utf8');

test('legacy ERP print routes declare frontend permissions', () => {
  const routerSource = readSource('router/index.ts');

  assert.match(
    routerSource,
    /path: '\/erp\/sale-orders\/:id\/print'[\s\S]*?meta: \{ title: '销售单打印', permissionsAny: \['erp-sale-draft:print', 'erp-sale-approved:print'\] \}/
  );
  assert.match(
    routerSource,
    /path: '\/erp\/purchase-orders\/:id\/print'[\s\S]*?meta: \{ title: '采购单打印', permissionsAny: \['erp-purchase-draft:print', 'erp-purchase-approved:print'\] \}/
  );
  assert.match(
    routerSource,
    /path: '\/erp\/sale-returns\/:id\/print'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpSaleReturnLegacyPrintRedirect\.vue'\)[\s\S]*?meta: \{ title: '销售退货单打印', permissionsAny: \['erp-sale-return-draft:print', 'erp-sale-return-approved:print'\] \}/
  );
  assert.match(
    routerSource,
    /path: '\/erp\/purchase-returns\/:id\/print'[\s\S]*?meta: \{ title: '采购退货单打印', permissionsAny: \['erp-purchase-return-draft:print', 'erp-purchase-return-approved:print'\] \}/
  );
  assert.match(routerSource, /const permissionsAny = to\.meta\.permissionsAny as string\[\] \| undefined/);
  assert.match(routerSource, /permissionsAny\.some\(permission => authStore\.hasPermission\(permission\)\)/);
});

test('approved copy buttons require copy and draft-add permissions before rendering', () => {
  const saleOrderSource = readSource('views/erp/ErpSaleOrderApprovedManagement.vue');
  assert.match(saleOrderSource, /const canCopy = computed\(\(\) => hasPermission\('erp-sale-approved:copy'\) && hasPermission\('erp-sale-draft:add'\)/);
  assert.match(saleOrderSource, /v-if="canCopy"[\s\S]*?@click="handleCopy\(row\)"/);

  const purchaseOrderSource = readSource('views/erp/ErpPurchaseOrderApproved.vue');
  assert.match(purchaseOrderSource, /const canCopy = computed\(\(\) =>[\s\S]*?hasPermission\('erp-purchase-approved:copy'\)[\s\S]*?hasPermission\('erp-purchase-draft:add'\)/);
  assert.match(purchaseOrderSource, /v-if="canCopy"[\s\S]*?@click="handleCopy\(row\)"/);

  const saleReturnSource = readSource('views/erp/ErpSaleReturnApprovedManagement.vue');
  assert.match(saleReturnSource, /const canCopy = computed\(\(\) => \([\s\S]*?hasPermission\('erp-sale-return-approved:copy'\)[\s\S]*?hasPermission\('erp-sale-return-draft:add'\)[\s\S]*?\)\);/);
  assert.match(saleReturnSource, /v-if="canCopy"[\s\S]*?@click="handleCopy\(row\)"/);
});

test('legacy sale order print redirect handles missing detail access with permission fallback', () => {
  const redirectSource = readSource('views/erp/ErpSaleOrderPrintRedirect.vue');

  assert.match(redirectSource, /import \{ useApiError \} from '@\/composables\/useApiError'/);
  assert.match(redirectSource, /import \{ useAuthStore \} from '@\/stores\/auth'/);
  assert.match(redirectSource, /const canDraftPrint = hasAnyPermission\(\['erp-sale-draft:print'\]\)/);
  assert.match(redirectSource, /const canApprovedPrint = hasAnyPermission\(\['erp-sale-approved:print'\]\)/);
  assert.match(redirectSource, /try \{[\s\S]*?request\.get\(`\/erp\/sale-orders\/\$\{id\}`\)[\s\S]*?\} catch \(error\) \{/);
  assert.match(redirectSource, /notifyError\(error\)/);
  assert.match(redirectSource, /'\/erp\/sale-orders\/approved'[\s\S]*?'\/erp\/sale-orders\/draft'/);
});

test('sale order split print routes stay outside MainLayout so preview prints only the document body', () => {
  const routerSource = readSource('router/index.ts');

  assert.match(
    routerSource,
    /path: '\/erp\/sale-orders\/draft\/:id\/print'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpSaleOrderDraftPrint\.vue'\)/
  );
  assert.match(
    routerSource,
    /path: '\/erp\/sale-orders\/approved\/:id\/print'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpSaleOrderApprovedPrint\.vue'\)/
  );

  const mainLayoutChildrenMatch = routerSource.match(/path: '\/',[\s\S]*?children: \[([\s\S]*?)\n\s*\]\n\s*\}/);
  assert.ok(mainLayoutChildrenMatch, 'expected MainLayout children block');
  const mainLayoutChildrenSource = mainLayoutChildrenMatch[1];

  assert.doesNotMatch(mainLayoutChildrenSource, /path: 'erp\/sale-orders\/draft\/:id\/print'/);
  assert.doesNotMatch(mainLayoutChildrenSource, /path: 'erp\/sale-orders\/approved\/:id\/print'/);
});

test('legacy sale order edit route redirects by document status instead of forcing draft workspace', () => {
  const routerSource = readSource('router/index.ts');
  const redirectSource = readSource('views/erp/ErpSaleOrderLegacyEditRedirect.vue');

  assert.match(
    routerSource,
    /path: 'erp\/sale-orders\/:id\/edit'[\s\S]*?component: \(\) => import\('\.\.\/views\/erp\/ErpSaleOrderLegacyEditRedirect\.vue'\)/
  );
  assert.match(redirectSource, /const canDraftEdit = hasAnyPermission\(\['erp-sale-draft:edit', 'erp-sale-draft:view'\]\)/);
  assert.match(redirectSource, /const canApprovedView = hasAnyPermission\(\['erp-sale-approved:view'\]\)/);
  assert.match(redirectSource, /request\.get\(`\/erp\/sale-orders\/\$\{id\}`\)/);
  assert.match(redirectSource, /status === 'DRAFT'/);
  assert.match(redirectSource, /buildEditPath\('draft', id\)/);
  assert.match(redirectSource, /buildEditPath\('approved', id\)/);
});

test('legacy sale return print route redirects to split print pages', () => {
  const redirectSource = readSource('views/erp/ErpSaleReturnLegacyPrintRedirect.vue');

  assert.match(redirectSource, /const canDraftPrint = hasAnyPermission\(\['erp-sale-return-draft:print'\]\)/);
  assert.match(redirectSource, /const canApprovedPrint = hasAnyPermission\(\['erp-sale-return-approved:print'\]\)/);
  assert.match(redirectSource, /request\.get\(`\/erp\/sale-returns\/\$\{id\}`\)/);
  assert.match(redirectSource, /buildPrintPath\(workspace, id\)/);
  assert.match(redirectSource, /'\/erp\/sale-returns\/approved'[\s\S]*?'\/erp\/sale-returns\/draft'/);
});

test('temporary router placeholder and login success debug output are removed', () => {
  const routerSource = readSource('router/index.ts');
  const loginSource = readSource('views/LoginView.vue');

  assert.doesNotMatch(routerSource, /TempComponent|演示页面|测试路由/);
  assert.doesNotMatch(loginSource, /console\.log\('Login successful'\)/);
});
