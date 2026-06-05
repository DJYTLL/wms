import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const layoutsRoot = join(__dirname, '..');
const routerFile = join(layoutsRoot, '..', 'router', 'index.ts');

const readLayout = (relativePath) => readFileSync(join(layoutsRoot, relativePath), 'utf8');
const readRouter = () => readFileSync(routerFile, 'utf8');

test('menu click layout prefetches heavy ERP list chunks before navigation', () => {
  const layoutSource = readLayout('MainLayout.vue');
  const routerSource = readRouter();

  assert.match(routerSource, /const preloadableRouteComponents: Record<string, Array<\(\) => Promise<unknown>>> = \{/);
  assert.match(routerSource, /const resolveRouteComponentLoaders = \(path: string\) => \{/);
  assert.match(routerSource, /router\.resolve\(path\)/);
  assert.match(routerSource, /record\.components/);
  assert.match(routerSource, /loaders = resolveRouteComponentLoaders\(path\)/);
  assert.match(routerSource, /'\/erp\/products': \[[\s\S]*?ErpProductManagement\.vue/);
  assert.match(routerSource, /'\/erp\/vehicle-fitments': \[[\s\S]*?ErpVehicleFitmentManagement\.vue/);
  assert.match(routerSource, /'\/erp\/customers': \[[\s\S]*?ErpCustomerManagement\.vue/);
  assert.match(routerSource, /'\/erp\/suppliers': \[[\s\S]*?ErpSupplierManagement\.vue/);
  assert.match(routerSource, /'\/erp\/print-templates': \[[\s\S]*?ErpPrintTemplateManagement\.vue/);
  assert.match(routerSource, /'\/erp\/purchase-orders\/draft': \[/);
  assert.match(routerSource, /'\/erp\/purchase-orders\/approved': \[/);
  assert.doesNotMatch(routerSource, /'\/erp\/sale-orders\/draft': \[/);
  assert.doesNotMatch(routerSource, /'\/erp\/sale-orders\/approved': \[/);
  assert.doesNotMatch(routerSource, /'\/erp\/sale-returns\/draft': \[/);
  assert.doesNotMatch(routerSource, /'\/erp\/sale-returns\/approved': \[/);
  assert.match(routerSource, /component: ErpSaleOrderDraftManagement,/);
  assert.match(routerSource, /component: ErpSaleOrderApprovedManagement,/);
  assert.match(routerSource, /component: ErpSaleReturnDraftManagement,/);
  assert.match(routerSource, /component: ErpSaleReturnApprovedManagement,/);
  assert.doesNotMatch(routerSource, /'\/erp\/sale-orders\/draft': \[[\s\S]*?ErpSaleOrderDraftManagement\.vue/);
  assert.doesNotMatch(routerSource, /loadSaleOrderDraftDeferredPanel/);
  assert.match(routerSource, /'\/erp\/stocks': \[/);
  assert.match(routerSource, /'\/erp\/stock-txns': \[/);
  assert.match(routerSource, /'\/erp\/stock-counts': \[/);
  assert.match(routerSource, /'\/erp\/stock-transfers': \[/);
  assert.match(routerSource, /'\/erp\/stock-inits': \[/);
  assert.match(routerSource, /'\/erp\/stock-warnings': \[/);
  assert.match(routerSource, /const preloadedRouteLoaders = new WeakSet<\(\) => Promise<unknown>>\(\)/);
  assert.match(routerSource, /preloadedRouteLoaders\.has\(load\)/);
  assert.match(routerSource, /preloadedRouteLoaders\.add\(load\)/);
  assert.doesNotMatch(routerSource, /const preloadedRoutePaths = new Set<string>\(\)/);
  assert.match(routerSource, /export const preloadRouteComponents = \(path: string\)/);

  assert.match(layoutSource, /import \{ preloadRouteComponents \} from '\.\.\/router'/);
  assert.doesNotMatch(layoutSource, /warmupSaleRouteData/);
  assert.match(layoutSource, /const prefetchMenuTarget = \(item: MenuItem\) => \{/);
  assert.match(layoutSource, /void preloadRouteComponents\(targetPath\);/);
  assert.match(layoutSource, /const warmupPrimarySaleRoutes = \(\) => \{/);
  assert.match(layoutSource, /for \(const path of primarySaleRouteWarmupPaths\) \{/);
  assert.match(layoutSource, /void preloadRouteComponents\(path\);/);
  assert.match(layoutSource, /const warmupHeavyBasicRoutes = \(\) => \{/);
  assert.match(layoutSource, /scheduleIdleWork\(\(\) => \{/);
  assert.match(layoutSource, /for \(const path of heavyBasicRouteWarmupPaths\) \{/);
  assert.match(layoutSource, /warmupHeavyBasicRoutes\(\);/);
  assert.match(layoutSource, /warmupPrimarySaleRoutes\(\);/);
  assert.match(layoutSource, /if \(primarySaleRouteWarmupPaths\.includes\(targetPath\)\) \{/);
  assert.match(layoutSource, /warmupPrimarySaleRoutes\(\);\s*return;/);
  assert.match(layoutSource, /prefetchMenuTarget\(item\);[\s\S]*?router\.push\(targetPath\);/);
  assert.match(layoutSource, /@mouseenter="prefetchMenuTarget\(subItem\)"/);
  assert.match(layoutSource, /@focusin="prefetchMenuTarget\(subItem\)"/);
  assert.match(layoutSource, /@mouseenter="prefetchMenuTarget\(leaf\)"/);
  assert.match(layoutSource, /@focusin="prefetchMenuTarget\(leaf\)"/);
});

test('main layout resolves menu paths once instead of during every active render', () => {
  const layoutSource = readLayout('MainLayout.vue');
  const resolveMenuPathBlock = layoutSource.match(/const resolveMenuPath = \(item: MenuItem\): string \| undefined => \{[\s\S]*?\n\};/)?.[0] ?? '';

  assert.match(layoutSource, /resolvedPath\?: string;/);
  assert.match(layoutSource, /const normalizeMenuPath = \(item: MenuItem\): string \| undefined => \{/);
  assert.match(layoutSource, /resolvedPath: normalizeMenuPath\(item\),/);
  assert.match(resolveMenuPathBlock, /return item\.resolvedPath \|\| undefined;/);
  assert.doesNotMatch(resolveMenuPathBlock, /router\.resolve/);
});
