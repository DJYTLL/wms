import test from 'node:test';
import assert from 'node:assert/strict';

import { buildMenuUserKey } from '../menuCacheKey.ts';

test('menu cache key changes when auth version changes', () => {
  const before = buildMenuUserKey({
    tenantCode: 'fycdz',
    username: 'admin',
    authVersion: 0,
  });
  const after = buildMenuUserKey({
    tenantCode: 'fycdz',
    username: 'admin',
    authVersion: 1,
  });

  assert.notEqual(before, after);
});
