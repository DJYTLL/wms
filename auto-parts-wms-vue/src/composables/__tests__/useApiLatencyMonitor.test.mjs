import test from 'node:test'
import assert from 'node:assert/strict'
import { isReadonly } from 'vue'

import { createApiLatencyMonitor, useApiLatencyMonitor } from '../useApiLatencyMonitor.ts'

test('createApiLatencyMonitor stores records, exposes filtered rows, and clears state', () => {
  const monitor = createApiLatencyMonitor({ maxRecords: 2, slowThresholdMs: 1000 })

  monitor.addRecord({
    id: '1',
    startedAt: '2026-05-23T10:00:00.000Z',
    finishedAt: '2026-05-23T10:00:01.200Z',
    durationMs: 1200,
    requestMethod: 'GET',
    normalizedPath: '/erp/sale-orders/page',
    routePath: '/erp/sale-orders/approved',
    routeTitle: '销售单（已审核）',
    statusCategory: 'SUCCESS',
    summary: '分页查询已审核销售单列表'
  })

  monitor.addRecord({
    id: '2',
    startedAt: '2026-05-23T10:01:00.000Z',
    finishedAt: '2026-05-23T10:01:00.120Z',
    durationMs: 120,
    requestMethod: 'POST',
    normalizedPath: '/erp/sale-orders/draft',
    routePath: '/erp/sale-orders/draft',
    routeTitle: '销售单（草稿）',
    statusCategory: 'FAIL',
    summary: '保存销售单草稿失败'
  })

  assert.equal(monitor.summary.value.totalCount, 2)
  assert.equal(monitor.summary.value.slowCount, 1)

  monitor.setFilter({ requestMethod: 'GET' })
  assert.deepEqual(monitor.filteredRecords.value.map((item) => item.id), ['1'])

  monitor.setFilter({ requestMethod: 'POST', minDurationMs: 100 })
  assert.deepEqual(monitor.filter.value, { requestMethod: 'POST', minDurationMs: 100 })

  monitor.clear()
  assert.equal(monitor.records.value.length, 0)
  assert.deepEqual(monitor.filter.value, {})
})

test('createApiLatencyMonitor exposes readonly records and filter views', () => {
  const monitor = createApiLatencyMonitor()

  monitor.addRecord({
    id: '1',
    startedAt: '2026-05-23T10:00:00.000Z',
    durationMs: 100
  })
  monitor.setFilter({ requestMethod: 'GET' })

  assert.equal(isReadonly(monitor.records), true)
  assert.equal(isReadonly(monitor.filter), true)
  assert.equal(monitor.records.value.length, 1)
  assert.deepEqual(monitor.filter.value, { requestMethod: 'GET' })

  monitor.resetFilter()
  assert.deepEqual(monitor.filter.value, {})
})

test('useApiLatencyMonitor returns shared state and clear resets records and filter', () => {
  const first = useApiLatencyMonitor()
  const second = useApiLatencyMonitor()

  first.clear()

  assert.strictEqual(first, second)

  first.addRecord({
    id: 'shared-1',
    startedAt: '2026-05-23T10:00:00.000Z',
    durationMs: 1500,
    requestMethod: 'GET',
    routePath: '/erp/sale-orders/approved',
    statusCategory: 'SUCCESS'
  })

  second.setFilter({ requestMethod: 'GET' })

  assert.equal(second.records.value.length, 1)
  assert.deepEqual(first.filteredRecords.value.map((item) => item.id), ['shared-1'])
  assert.deepEqual(first.filter.value, { requestMethod: 'GET' })

  second.clear()

  assert.equal(first.records.value.length, 0)
  assert.equal(second.records.value.length, 0)
  assert.deepEqual(first.filter.value, {})
  assert.deepEqual(second.filter.value, {})
})
