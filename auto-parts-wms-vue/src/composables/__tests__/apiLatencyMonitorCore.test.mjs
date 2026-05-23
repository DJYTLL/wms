import test from 'node:test'
import assert from 'node:assert/strict'

import {
  appendLatencyRecord,
  buildLatencySummaryText,
  buildLatencySummary,
  estimatePayloadBytes,
  filterLatencyRecords,
  normalizeLatencyStatus,
  sortLatencyRecords
} from '../apiLatencyMonitorCore.ts'

test('appendLatencyRecord prepends new records and trims to the max size', () => {
  const records = [
    { id: 'old-1', durationMs: 120, startedAt: '2026-05-23T10:00:00.000Z' },
    { id: 'old-2', durationMs: 90, startedAt: '2026-05-23T09:59:00.000Z' }
  ]

  const next = appendLatencyRecord(records, {
    id: 'new-1',
    durationMs: 450,
    startedAt: '2026-05-23T10:01:00.000Z'
  }, 2)

  assert.deepEqual(next.map((item) => item.id), ['new-1', 'old-1'])
})

test('appendLatencyRecord treats zero and invalid maxRecords values explicitly', () => {
  const records = [
    { id: 'old-1', durationMs: 120, startedAt: '2026-05-23T10:00:00.000Z' }
  ]
  const record = {
    id: 'new-1',
    durationMs: 450,
    startedAt: '2026-05-23T10:01:00.000Z'
  }

  assert.deepEqual(appendLatencyRecord(records, record, 0), [])
  assert.deepEqual(appendLatencyRecord(records, record, -1), [])
  assert.deepEqual(appendLatencyRecord(records, record, Number.NaN), [])
  assert.deepEqual(appendLatencyRecord(records, record, Number.POSITIVE_INFINITY), [])
  assert.deepEqual(appendLatencyRecord(records, record, 1.9).map((item) => item.id), ['new-1'])
})

test('sortLatencyRecords orders by duration descending by default', () => {
  const sorted = sortLatencyRecords([
    { id: 'a', durationMs: 120, startedAt: '2026-05-23T10:00:00.000Z' },
    { id: 'b', durationMs: 620, startedAt: '2026-05-23T10:01:00.000Z' },
    { id: 'c', durationMs: 240, startedAt: '2026-05-23T10:02:00.000Z' }
  ])

  assert.deepEqual(sorted.map((item) => item.id), ['b', 'c', 'a'])
})

test('filterLatencyRecords applies keyword, method, status, route, and duration bounds together', () => {
  const filtered = filterLatencyRecords([
    {
      id: '1',
      durationMs: 1450,
      requestMethod: 'GET',
      normalizedPath: '/erp/sale-orders/page',
      routePath: '/erp/sale-orders/approved',
      routeTitle: '销售单（已审核）',
      statusCategory: 'SUCCESS',
      summary: '分页查询已审核销售单列表',
      startedAt: '2026-05-23T10:00:00.000Z'
    },
    {
      id: '2',
      durationMs: 120,
      requestMethod: 'POST',
      normalizedPath: '/erp/sale-orders/draft',
      routePath: '/erp/sale-orders/draft',
      routeTitle: '销售单（草稿）',
      statusCategory: 'FAIL',
      summary: '保存销售单草稿失败',
      startedAt: '2026-05-23T10:01:00.000Z'
    }
  ], {
    keyword: '销售单',
    requestMethod: 'GET',
    statusCategory: 'SUCCESS',
    routePath: '/erp/sale-orders/approved',
    minDurationMs: 1000,
    maxDurationMs: 2000
  })

  assert.deepEqual(filtered.map((item) => item.id), ['1'])
})

test('buildLatencySummary counts totals, averages, slow requests, and failures', () => {
  const summary = buildLatencySummary([
    { id: '1', durationMs: 1200, statusCategory: 'SUCCESS', startedAt: '2026-05-23T10:00:00.000Z' },
    { id: '2', durationMs: 300, statusCategory: 'FAIL', startedAt: '2026-05-23T10:01:00.000Z' },
    { id: '3', durationMs: 500, statusCategory: 'TIMEOUT', startedAt: '2026-05-23T10:02:00.000Z' },
    { id: '4', durationMs: 80, statusCategory: 'CANCELLED', startedAt: '2026-05-23T10:03:00.000Z' }
  ], 1000)

  assert.equal(summary.totalCount, 4)
  assert.equal(summary.totalDurationMs, 2080)
  assert.equal(summary.slowCount, 1)
  assert.equal(summary.failureCount, 3)
  assert.equal(summary.averageDurationMs, 520)
})

test('estimatePayloadBytes returns null for unknown payloads and byte length for supported payloads', () => {
  assert.equal(estimatePayloadBytes(undefined), null)
  assert.equal(estimatePayloadBytes(null), null)
  assert.equal(estimatePayloadBytes('abc'), Buffer.byteLength('abc'))
  assert.equal(
    estimatePayloadBytes({ a: '1234' }),
    Buffer.byteLength(JSON.stringify({ a: '1234' }))
  )
})

test('estimatePayloadBytes handles non JSON payload boundaries conservatively', () => {
  const searchParams = new URLSearchParams([['a', '1'], ['bb', '22']])
  const arrayBuffer = new Uint8Array([1, 2, 3, 4]).buffer
  const uint8Array = new Uint8Array([5, 6, 7])

  assert.equal(estimatePayloadBytes(searchParams), Buffer.byteLength(searchParams.toString()))
  assert.equal(estimatePayloadBytes(arrayBuffer), 4)
  assert.equal(estimatePayloadBytes(uint8Array), 3)
})

test('estimatePayloadBytes returns null when JSON.stringify produces undefined', () => {
  assert.equal(estimatePayloadBytes(function noop () {}), null)
  assert.equal(estimatePayloadBytes(Symbol('x')), null)
  assert.equal(estimatePayloadBytes({ toJSON () { return undefined } }), null)
})

test('filterLatencyRecords ignores an omitted statusCategory filter without empty-string sentinel', () => {
  const filtered = filterLatencyRecords([
    {
      id: '1',
      durationMs: 100,
      statusCategory: 'SUCCESS',
      startedAt: '2026-05-23T10:00:00.000Z'
    },
    {
      id: '2',
      durationMs: 200,
      statusCategory: 'FAIL',
      startedAt: '2026-05-23T10:01:00.000Z'
    }
  ], {})

  assert.deepEqual(filtered.map((item) => item.id), ['1', '2'])
})

test('normalizeLatencyStatus classifies timeout and cancellation before generic failure', () => {
  assert.equal(
    normalizeLatencyStatus({ code: 'ECONNABORTED', message: 'timeout of 10000ms exceeded' }),
    'TIMEOUT'
  )
  assert.equal(
    normalizeLatencyStatus({ code: 'ERR_CANCELED', message: 'canceled' }),
    'CANCELLED'
  )
  assert.equal(
    normalizeLatencyStatus({ message: 'Request failed with status code 500' }),
    'FAIL'
  )
})

test('buildLatencySummaryText prefers route title and action wording', () => {
  const summary = buildLatencySummaryText({
    requestMethod: 'GET',
    normalizedPath: '/erp/sale-orders/page',
    routeTitle: '销售单（已审核）',
    statusCategory: 'SUCCESS'
  })

  assert.equal(summary, '销售单（已审核）分页查询')
})
