import assert from 'node:assert/strict';
import { test } from 'node:test';

import zh from '../../../locales/zh.ts';
import en from '../../../locales/en.ts';

test('warehouse default label has i18n entries in zh and en locales', () => {
  assert.equal(zh.field.default, '默认');
  assert.equal(en.field.default, 'Default');
});
