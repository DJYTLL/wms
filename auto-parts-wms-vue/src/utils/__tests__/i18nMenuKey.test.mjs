import assert from 'node:assert/strict';
import { test } from 'node:test';

import { normalizeMenuKey } from '../i18n.ts';

test('normalizes erp stock transfer menu key to translated nav key', () => {
  assert.equal(normalizeMenuKey('erp-stock-transfer'), 'nav.erpStockTransfer');
});

test('normalizes tenant setting menu key to translated nav key', () => {
  assert.equal(normalizeMenuKey('tenant-setting'), 'nav.tenantSetting');
});
