import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const utilsRoot = join(__dirname, '..');
const requestSource = () => readFileSync(join(utilsRoot, 'request.ts'), 'utf8');

test('request interceptor builds latency metadata synchronously before request start', () => {
  const source = requestSource();
  const requestInterceptorBlock = source.match(/request\.interceptors\.request\.use\([\s\S]*?\n\)/)?.[0] ?? '';

  assert.match(source, /const readCurrentRouteContext = \(\) => \{/);
  assert.match(source, /const buildLatencyMeta = \(config: LatencyAwareRequestConfig\): LatencyRequestMeta => \{/);
  assert.doesNotMatch(source, /import\('\.\.\/router'\)/);
  assert.doesNotMatch(source, /routerModulePromise/);
  assert.doesNotMatch(requestInterceptorBlock, /await buildLatencyMeta/);
});
