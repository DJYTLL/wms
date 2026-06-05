import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

const stockTxnSource = readFileSync(join(viewsRoot, 'ErpStockTxnManagement.vue'), 'utf8');
const assemblySource = readFileSync(join(viewsRoot, 'ErpAssemblyOrderManagement.vue'), 'utf8');
const disassembleSource = readFileSync(join(viewsRoot, 'ErpDisassembleOrderManagement.vue'), 'utf8');

test('库存流水列表直接使用分页返回的名称字段', () => {
  assert.match(stockTxnSource, /\{\{ row\.productName \|\| '-' \}\}/);
  assert.match(stockTxnSource, /\{\{ row\.warehouseName \|\| '-' \}\}/);
  assert.match(stockTxnSource, /\{\{ row\.locationName \|\| getLocationName\(row\.locationId\) \}\}/);
  assert.doesNotMatch(stockTxnSource, /const getProductName = \(id\?: number\) =>/);
  assert.doesNotMatch(stockTxnSource, /const getWarehouseName = \(id\?: number\) =>/);
});

test('组装和拆装列表直接使用分页返回的成品名称字段', () => {
  assert.match(assemblySource, /\{\{ row\.finishedProductName \|\| '-' \}\}/);
  assert.match(disassembleSource, /\{\{ row\.finishedProductName \|\| '-' \}\}/);
  assert.doesNotMatch(assemblySource, /request\.get\('\/erp\/products'\)/);
  assert.doesNotMatch(disassembleSource, /request\.get\('\/erp\/products'\)/);
});
