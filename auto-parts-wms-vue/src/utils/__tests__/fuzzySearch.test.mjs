import assert from 'node:assert/strict';
import { test } from 'node:test';

import { filterByFuzzyKeyword, fuzzyIncludes } from '../fuzzySearch.ts';

test('numeric contact queries do not match unrelated rows by unordered digit overlap', () => {
  assert.equal(
    fuzzyIncludes(['电话:13903741733\\03745650899'], '13937468830'),
    false
  );
  assert.equal(
    fuzzyIncludes(['万湖汽配-D4-4 / 电话:1367359998；手机:13888038330'], '13937468830'),
    false
  );
});

test('numeric contact queries still match when the exact number exists in contact info', () => {
  assert.equal(
    fuzzyIncludes(['联系人:张三 / 电话:13937468830；微信:abc'], '13937468830'),
    true
  );
});

test('sorts fuzzy search results by matched keyword count descending', () => {
  const rows = [
    { name: 'ABS继电器', code: 'A-001' },
    { name: '刹车感应器', code: 'S-001' },
    { name: 'ABS刹车感应器总成', code: 'AS-001' },
  ];

  const result = filterByFuzzyKeyword(rows, 'ABS 感应器', row => [row.name, row.code]);

  assert.deepEqual(result.map(row => row.code), ['AS-001', 'A-001', 'S-001']);
});

test('treats compact digit and pinyin initial queries as multiple searchable terms', () => {
  const rows = [
    { name: '140中心支架总成<不含外壳>YC', code: '2663170-0001-001' },
    { name: '45X120.4万向节J', code: 'W-0027' },
    { name: '140转-NB万向节J(24X63)', code: 'A-9001' },
  ];

  const result = filterByFuzzyKeyword(rows, '140wxj', row => [row.code, row.name]);

  assert.deepEqual(result.map(row => row.code), ['A-9001', '2663170-0001-001', 'W-0027']);
});
