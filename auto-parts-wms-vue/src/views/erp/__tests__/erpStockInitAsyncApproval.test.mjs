import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');

test('stock init page shows async approval statuses and allows retry after approve failure', () => {
  const source = readFileSync(join(viewsRoot, 'ErpStockCountManagement.vue'), 'utf8');

  assert.match(source, /<el-option label="审核中" value="APPROVING" \/>/);
  assert.match(source, /<el-option label="审核失败" value="APPROVE_FAILED" \/>/);
  assert.match(source, /if \(status === 'APPROVING'\) return '审核中';/);
  assert.match(source, /if \(status === 'APPROVE_FAILED'\) return '审核失败';/);
  assert.match(source, /row\.status === 'APPROVING' \? 'info'/);
  assert.match(source, /row\.status !== 'DRAFT' && row\.status !== 'APPROVE_FAILED'/);
  assert.match(source, /const stockInitApprovalPollingTimer = ref<number \| null>\(null\);/);
  assert.match(source, /const startStockInitApprovalPolling = \(countId: number\) =>/);
  assert.match(source, /const pollStockInitApproval = async \(countId: number\) =>/);
  assert.match(source, /if \(row\.status === 'APPROVING'\) \{/);
  assert.match(source, /startStockInitApprovalPolling\(row\.id\);/);
  assert.match(source, /notifySuccess\('初始库存审核任务已提交，正在后台处理'\)/);
});
