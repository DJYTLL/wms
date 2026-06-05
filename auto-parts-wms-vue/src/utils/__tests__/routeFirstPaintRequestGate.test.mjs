import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const srcRoot = join(__dirname, '..', '..');

const readSource = (relativePath) => readFileSync(join(srcRoot, relativePath), 'utf8');

test('router opens a first-paint request gate for every non-print route transition', () => {
  const routerSource = readSource('router/index.ts');
  const gateSource = readSource('utils/routeFirstPaintRequestGate.ts');

  assert.match(gateSource, /export const openRouteFirstPaintRequestGate/);
  assert.match(gateSource, /requestAnimationFrame/);
  assert.match(gateSource, /setTimeout/);
  assert.match(routerSource, /openRouteFirstPaintRequestGate\(to\.path\)/);
  assert.match(routerSource, /!to\.path\.includes\('\/print'\)/);
});

test('request interceptor yields opening GET requests until route first paint', () => {
  const requestSource = readSource('utils/request.ts');

  assert.match(requestSource, /import \{ waitForRouteFirstPaintRequestGate \} from '\.\/routeFirstPaintRequestGate'/);
  assert.match(requestSource, /const shouldWaitForRouteFirstPaint = \(config: LatencyAwareRequestConfig\) => \{/);
  assert.match(requestSource, /method === 'get'/);
  assert.match(requestSource, /!isAuthEndpoint\(config\.url\)/);
  assert.match(requestSource, /await waitForRouteFirstPaintRequestGate\(\)/);
  assert.match(
    requestSource,
    /if \(shouldWaitForRouteFirstPaint\(config\)\) \{[\s\S]*?await waitForRouteFirstPaintRequestGate\(\)[\s\S]*?\}[\s\S]*?config\._latencyMeta = buildLatencyMeta\(config\)/,
    'request latency and network start should happen after the route first-paint gate'
  );
});
