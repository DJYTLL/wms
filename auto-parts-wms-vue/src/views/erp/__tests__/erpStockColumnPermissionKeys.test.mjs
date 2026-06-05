import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = join(__dirname, '..', '..', '..', '..', '..');
const permissionSeedSource = readFileSync(
  join(projectRoot, 'wms-backend', 'src', 'main', 'java', 'com', 'example', 'wms', 'config', 'PermissionSeedProvider.java'),
  'utf8',
);

test('库存台账列权限种子拆分为在库、锁定、可用三列', () => {
  assert.match(permissionSeedSource, /column:erp-stock:qtyOnHand/);
  assert.match(permissionSeedSource, /column:erp-stock:qtyLocked/);
  assert.match(permissionSeedSource, /column:erp-stock:qtyAvailable/);
  assert.doesNotMatch(permissionSeedSource, /column:erp-stock:qty", "ERP库存-数量列"/);
});
