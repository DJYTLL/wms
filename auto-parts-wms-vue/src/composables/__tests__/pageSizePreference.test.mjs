import test from 'node:test';
import assert from 'node:assert/strict';

import {
  resolveEffectivePageSize,
  sanitizePageSize
} from '../pageSizePreferenceCore.ts';

test('resolveEffectivePageSize prefers user value over tenant and fallback', () => {
  assert.deepEqual(resolveEffectivePageSize({
    userPageSize: 50,
    tenantDefaultPageSize: 20,
    fallbackPageSize: 10
  }), {
    pageSize: 50,
    source: 'USER'
  });
});

test('resolveEffectivePageSize falls back to tenant default when user value is absent', () => {
  assert.deepEqual(resolveEffectivePageSize({
    userPageSize: null,
    tenantDefaultPageSize: 20,
    fallbackPageSize: 10
  }), {
    pageSize: 20,
    source: 'TENANT'
  });
});

test('resolveEffectivePageSize falls back to builtin default when user and tenant values are absent', () => {
  assert.deepEqual(resolveEffectivePageSize({
    userPageSize: null,
    tenantDefaultPageSize: null,
    fallbackPageSize: 10
  }), {
    pageSize: 10,
    source: 'DEFAULT'
  });
});

test('sanitizePageSize only accepts positive integers in allowed range', () => {
  assert.equal(sanitizePageSize(20), 20);
  assert.equal(sanitizePageSize('50'), 50);
  assert.equal(sanitizePageSize(0), null);
  assert.equal(sanitizePageSize(201), null);
  assert.equal(sanitizePageSize('abc'), null);
});
