import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { test } from 'node:test';

const root = fileURLToPath(new URL('../', import.meta.url));

const readSource = (relativePath) => readFileSync(join(root, relativePath), 'utf8');

const assertContains = (source, expected) => {
  assert.ok(
    source.includes(expected),
    `Expected source to include: ${expected}`,
  );
};

test('role permission tree maps registered system pages to menu nodes', () => {
  const source = readSource('src/views/system/RoleManagement.vue');

  assertContains(source, "{ prefix: 'tenant-setting:', pageKeys: ['tenant-setting'] }");
  assertContains(source, "{ prefix: 'api-latency-monitor:', pageKeys: ['api-latency-monitor'] }");
  assertContains(source, "'tenant-setting': ['tenant-setting']");
  assertContains(source, "'api-latency-monitor': ['api-latency-monitor']");
  assertContains(source, "'/tenant-settings': 'tenant-setting'");
  assertContains(source, "'/api-latency-monitor': 'api-latency-monitor'");
});

test('permission maintenance tree maps registered system pages to menu nodes', () => {
  const source = readSource('src/views/system/PermissionManagement.vue');

  assertContains(source, "'tenant-setting': ['tenant-setting']");
  assertContains(source, "'api-latency-monitor': ['api-latency-monitor']");
});
