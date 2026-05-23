# API Latency Monitor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build front-end API latency capture in the shared request layer and add an ERP-style query page that uses the existing `ErpDataTable / ErpDataTableColumn` stack.

**Architecture:** Capture request timing in `src/utils/request.ts`, normalize records into a focused front-end monitor store/composable, and render them through a new system list page that follows the existing `page-shell + page-toolbar-card + table-card` structure. Keep storage local to the browser memory for v1, enforce a bounded record count, and reuse existing router, i18n, page-size, and table-column patterns instead of creating a parallel UI stack.

**Tech Stack:** Vue 3, TypeScript, Pinia/composables patterns already used in `auto-parts-wms-vue`, axios interceptors, vue-router, Element Plus, `ErpDataTable`, Node built-in test runner, `vue-tsc`

---

## File Structure

### Existing files to modify

- `auto-parts-wms-vue/src/utils/request.ts`
  Responsibility: inject timing capture into the shared axios request/response lifecycle.
- `auto-parts-wms-vue/src/router/index.ts`
  Responsibility: register the new page route and protect it with its route permission.
- `auto-parts-wms-vue/src/locales/zh.ts`
  Responsibility: add Chinese page title, filter labels, status labels, and summary copy for the new page.
- `auto-parts-wms-vue/src/locales/en.ts`
  Responsibility: add matching English keys so the locale shape stays consistent.

### New files to create

- `auto-parts-wms-vue/src/composables/apiLatencyMonitorCore.ts`
  Responsibility: pure logic for record normalization, bounded insertion, filtering, sorting, and summary calculations. This file is intentionally framework-light so it can be unit tested directly.
- `auto-parts-wms-vue/src/composables/useApiLatencyMonitor.ts`
  Responsibility: expose a singleton reactive record store and wrapper methods used by interceptors and the page.
- `auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs`
  Responsibility: test the core logic without rendering Vue.
- `auto-parts-wms-vue/src/views/system/ApiLatencyMonitorView.vue`
  Responsibility: ERP-style list page with summary strip, filters, `ErpDataTable`, tooltip-enabled long text columns, and pagination.

## Task 1: Build the latency monitor core with tests first

**Files:**
- Create: `auto-parts-wms-vue/src/composables/apiLatencyMonitorCore.ts`
- Test: `auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs`

- [ ] **Step 1: Write the failing test for bounded insertion and default sorting**

```js
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  appendLatencyRecord,
  sortLatencyRecords,
} from '../apiLatencyMonitorCore.ts'

test('appendLatencyRecord prepends newer records and trims to the max size', () => {
  const records = [
    { id: 'old-1', durationMs: 120, startedAt: '2026-05-23T10:00:00.000Z' },
    { id: 'old-2', durationMs: 90, startedAt: '2026-05-23T09:59:00.000Z' },
  ]

  const next = appendLatencyRecord(records, {
    id: 'new-1',
    durationMs: 450,
    startedAt: '2026-05-23T10:01:00.000Z',
  }, 2)

  assert.deepEqual(next.map((item) => item.id), ['new-1', 'old-1'])
})

test('sortLatencyRecords orders by duration descending by default', () => {
  const sorted = sortLatencyRecords([
    { id: 'a', durationMs: 120, startedAt: '2026-05-23T10:00:00.000Z' },
    { id: 'b', durationMs: 620, startedAt: '2026-05-23T10:01:00.000Z' },
    { id: 'c', durationMs: 240, startedAt: '2026-05-23T10:02:00.000Z' },
  ])

  assert.deepEqual(sorted.map((item) => item.id), ['b', 'c', 'a'])
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: FAIL because `apiLatencyMonitorCore.ts` does not exist yet.

- [ ] **Step 3: Write minimal core implementation**

```ts
export type ApiLatencyRecord = {
  id: string
  startedAt: string
  finishedAt?: string
  durationMs: number
  requestMethod?: string
  normalizedPath?: string
  routePath?: string
  routeTitle?: string
  statusCategory?: 'SUCCESS' | 'FAIL' | 'TIMEOUT' | 'CANCELLED'
  httpStatus?: number | null
  requestBytes?: number | null
  responseBytes?: number | null
  errorMessage?: string | null
  summary?: string
}

export const appendLatencyRecord = (
  records: ApiLatencyRecord[],
  record: ApiLatencyRecord,
  maxRecords: number,
) => [record, ...records].slice(0, Math.max(1, maxRecords))

export const sortLatencyRecords = (records: ApiLatencyRecord[]) => (
  [...records].sort((left, right) => right.durationMs - left.durationMs)
)
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: PASS for both tests.

- [ ] **Step 5: Expand the failing tests for filtering, summaries, and size estimation**

```js
import {
  buildLatencySummary,
  estimatePayloadBytes,
  filterLatencyRecords,
  normalizeLatencyStatus,
} from '../apiLatencyMonitorCore.ts'

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
      startedAt: '2026-05-23T10:00:00.000Z',
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
      startedAt: '2026-05-23T10:01:00.000Z',
    },
  ], {
    keyword: '销售单',
    requestMethod: 'GET',
    statusCategory: 'SUCCESS',
    routePath: '/erp/sale-orders/approved',
    minDurationMs: 1000,
    maxDurationMs: 2000,
  })

  assert.deepEqual(filtered.map((item) => item.id), ['1'])
})

test('buildLatencySummary counts totals, averages, slow requests, and failures', () => {
  const summary = buildLatencySummary([
    { id: '1', durationMs: 1200, statusCategory: 'SUCCESS', startedAt: '2026-05-23T10:00:00.000Z' },
    { id: '2', durationMs: 300, statusCategory: 'FAIL', startedAt: '2026-05-23T10:01:00.000Z' },
    { id: '3', durationMs: 500, statusCategory: 'TIMEOUT', startedAt: '2026-05-23T10:02:00.000Z' },
  ], 1000)

  assert.equal(summary.totalCount, 3)
  assert.equal(summary.slowCount, 1)
  assert.equal(summary.failureCount, 2)
  assert.equal(summary.averageDurationMs, 667)
})

test('estimatePayloadBytes returns null for unknown payloads and JSON length for objects', () => {
  assert.equal(estimatePayloadBytes(undefined), null)
  assert.equal(estimatePayloadBytes({ a: '1234' }), Buffer.byteLength(JSON.stringify({ a: '1234' })))
})

test('normalizeLatencyStatus classifies timeout and cancellation before generic failure', () => {
  assert.equal(normalizeLatencyStatus({ code: 'ECONNABORTED', message: 'timeout of 10000ms exceeded' }), 'TIMEOUT')
  assert.equal(normalizeLatencyStatus({ code: 'ERR_CANCELED', message: 'canceled' }), 'CANCELLED')
  assert.equal(normalizeLatencyStatus({ message: 'Request failed with status code 500' }), 'FAIL')
})
```

- [ ] **Step 6: Implement the missing core helpers minimally**

```ts
export type ApiLatencyFilter = {
  keyword?: string
  requestMethod?: string
  statusCategory?: string
  routePath?: string
  minDurationMs?: number | null
  maxDurationMs?: number | null
}

export const estimatePayloadBytes = (payload: unknown): number | null => {
  if (payload == null) return null
  if (typeof payload === 'string') return new TextEncoder().encode(payload).length
  try {
    return new TextEncoder().encode(JSON.stringify(payload)).length
  } catch {
    return null
  }
}

export const normalizeLatencyStatus = (errorLike?: { code?: string; message?: string } | null) => {
  if (errorLike?.code === 'ERR_CANCELED') return 'CANCELLED'
  if (errorLike?.code === 'ECONNABORTED' || /timeout/i.test(errorLike?.message || '')) return 'TIMEOUT'
  return 'FAIL'
}

export const filterLatencyRecords = (
  records: ApiLatencyRecord[],
  filter: ApiLatencyFilter,
) => records.filter((record) => {
  const keyword = (filter.keyword || '').trim()
  if (keyword) {
    const haystack = [
      record.normalizedPath,
      record.routePath,
      record.routeTitle,
      record.summary,
      record.errorMessage,
    ].join(' ')
    if (!haystack.includes(keyword)) return false
  }
  if (filter.requestMethod && record.requestMethod !== filter.requestMethod) return false
  if (filter.statusCategory && record.statusCategory !== filter.statusCategory) return false
  if (filter.routePath && record.routePath !== filter.routePath) return false
  if (filter.minDurationMs != null && record.durationMs < filter.minDurationMs) return false
  if (filter.maxDurationMs != null && record.durationMs > filter.maxDurationMs) return false
  return true
})

export const buildLatencySummary = (records: ApiLatencyRecord[], slowThresholdMs: number) => {
  const totalCount = records.length
  const totalDurationMs = records.reduce((sum, record) => sum + record.durationMs, 0)
  const slowCount = records.filter((record) => record.durationMs >= slowThresholdMs).length
  const failureCount = records.filter((record) => (
    record.statusCategory === 'FAIL'
    || record.statusCategory === 'TIMEOUT'
    || record.statusCategory === 'CANCELLED'
  )).length
  return {
    totalCount,
    totalDurationMs,
    averageDurationMs: totalCount ? Math.round(totalDurationMs / totalCount) : 0,
    slowCount,
    failureCount,
  }
}
```

- [ ] **Step 7: Run the expanded core tests**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: PASS for all core tests.

- [ ] **Step 8: Commit**

```bash
git add auto-parts-wms-vue/src/composables/apiLatencyMonitorCore.ts auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs
git commit -m "test: add api latency monitor core"
```

## Task 2: Add the reactive monitor store with test-first coverage

**Files:**
- Create: `auto-parts-wms-vue/src/composables/useApiLatencyMonitor.ts`
- Modify: `auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs`

- [ ] **Step 1: Write the failing test for add, clear, and derived summary behavior**

```js
import { createApiLatencyMonitor } from '../useApiLatencyMonitor.ts'

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
    summary: '分页查询已审核销售单列表',
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
    summary: '保存销售单草稿失败',
  })

  assert.equal(monitor.summary.value.totalCount, 2)
  assert.equal(monitor.summary.value.slowCount, 1)

  monitor.filter.value = { requestMethod: 'GET' }
  assert.deepEqual(monitor.filteredRecords.value.map((item) => item.id), ['1'])

  monitor.clear()
  assert.equal(monitor.records.value.length, 0)
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: FAIL because `useApiLatencyMonitor.ts` does not exist yet.

- [ ] **Step 3: Implement the minimal reactive monitor wrapper**

```ts
import { computed, ref } from 'vue'
import {
  appendLatencyRecord,
  buildLatencySummary,
  filterLatencyRecords,
  sortLatencyRecords,
  type ApiLatencyFilter,
  type ApiLatencyRecord,
} from './apiLatencyMonitorCore'

export const createApiLatencyMonitor = (
  options: { maxRecords?: number; slowThresholdMs?: number } = {},
) => {
  const maxRecords = options.maxRecords ?? 1000
  const slowThresholdMs = options.slowThresholdMs ?? 1000
  const records = ref<ApiLatencyRecord[]>([])
  const filter = ref<ApiLatencyFilter>({})

  const filteredRecords = computed(() => (
    sortLatencyRecords(filterLatencyRecords(records.value, filter.value))
  ))

  const summary = computed(() => buildLatencySummary(records.value, slowThresholdMs))

  const addRecord = (record: ApiLatencyRecord) => {
    records.value = appendLatencyRecord(records.value, record, maxRecords)
  }

  const clear = () => {
    records.value = []
  }

  return {
    records,
    filter,
    filteredRecords,
    summary,
    addRecord,
    clear,
  }
}

const sharedMonitor = createApiLatencyMonitor()

export const useApiLatencyMonitor = () => sharedMonitor
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: PASS including the new monitor wrapper test.

- [ ] **Step 5: Commit**

```bash
git add auto-parts-wms-vue/src/composables/useApiLatencyMonitor.ts auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs
git commit -m "feat: add reactive api latency monitor store"
```

## Task 3: Wire request interceptor capture through the shared monitor

**Files:**
- Modify: `auto-parts-wms-vue/src/utils/request.ts`
- Modify: `auto-parts-wms-vue/src/composables/apiLatencyMonitorCore.ts`

- [ ] **Step 1: Write the failing core test for route-aware summary generation**

```js
import { buildLatencySummaryText } from '../apiLatencyMonitorCore.ts'

test('buildLatencySummaryText prefers route title and action wording', () => {
  const summary = buildLatencySummaryText({
    requestMethod: 'GET',
    normalizedPath: '/erp/sale-orders/page',
    routeTitle: '销售单（已审核）',
    statusCategory: 'SUCCESS',
  })

  assert.equal(summary, '销售单（已审核）分页查询')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: FAIL because `buildLatencySummaryText` is missing.

- [ ] **Step 3: Implement minimal summary text helpers in the core**

```ts
const inferLatencyAction = (method?: string, path?: string) => {
  if (method === 'GET' && /\/page($|\?)/.test(path || '')) return '分页查询'
  if (method === 'GET') return '查询'
  if (method === 'POST') return '新增或提交'
  if (method === 'PUT') return '更新'
  if (method === 'DELETE') return '删除'
  return method || '请求'
}

export const buildLatencySummaryText = (record: Partial<ApiLatencyRecord>) => {
  const action = inferLatencyAction(record.requestMethod, record.normalizedPath)
  const subject = record.routeTitle || record.normalizedPath || record.requestMethod || '接口'
  if (record.statusCategory === 'FAIL' || record.statusCategory === 'TIMEOUT' || record.statusCategory === 'CANCELLED') {
    return `${subject}${action}失败${record.errorMessage ? `：${record.errorMessage}` : ''}`
  }
  return `${subject}${action}`
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: PASS including the summary-text test.

- [ ] **Step 5: Add request capture into the shared axios instance**

```ts
import router from '@/router'
import { useApiLatencyMonitor } from '@/composables/useApiLatencyMonitor'
import {
  buildLatencySummaryText,
  estimatePayloadBytes,
  normalizeLatencyStatus,
} from '@/composables/apiLatencyMonitorCore'

type LatencyRequestMeta = {
  startedAtMs: number
  startedAtIso: string
  routePath: string
  routeTitle: string
  normalizedPath: string
  requestBytes: number | null
}

const buildLatencyMeta = (config: any): LatencyRequestMeta => {
  const currentRoute = router.currentRoute.value
  return {
    startedAtMs: performance.now(),
    startedAtIso: new Date().toISOString(),
    routePath: currentRoute.path,
    routeTitle: String(currentRoute.meta?.title || currentRoute.path || ''),
    normalizedPath: normalizeRequestUrl(config.url),
    requestBytes: estimatePayloadBytes(config.data),
  }
}

const finalizeLatencyRecord = (
  config: any,
  payload: {
    finishedAtIso: string
    durationMs: number
    statusCategory: 'SUCCESS' | 'FAIL' | 'TIMEOUT' | 'CANCELLED'
    httpStatus?: number | null
    responseBytes?: number | null
    errorMessage?: string | null
  },
) => {
  const monitor = useApiLatencyMonitor()
  const meta = config._latencyMeta as LatencyRequestMeta | undefined
  if (!meta) return
  const record = {
    id: `${meta.startedAtIso}-${config.method || 'get'}-${meta.normalizedPath}`,
    startedAt: meta.startedAtIso,
    finishedAt: payload.finishedAtIso,
    durationMs: payload.durationMs,
    requestMethod: String(config.method || 'GET').toUpperCase(),
    requestUrl: String(config.url || ''),
    normalizedPath: meta.normalizedPath,
    routePath: meta.routePath,
    routeTitle: meta.routeTitle,
    statusCategory: payload.statusCategory,
    httpStatus: payload.httpStatus ?? null,
    requestBytes: meta.requestBytes,
    responseBytes: payload.responseBytes ?? null,
    errorMessage: payload.errorMessage ?? null,
    summary: '',
  }
  record.summary = buildLatencySummaryText(record)
  monitor.addRecord(record)
}
```

- [ ] **Step 6: Finish the interceptor integration**

```ts
request.interceptors.request.use(async (config) => {
  config._latencyMeta = buildLatencyMeta(config)
  // existing token/idempotency/delete-reason logic stays in place
  return ensureDeleteReason(config)
})

request.interceptors.response.use(
  (response) => {
    finalizeLatencyRecord(response.config, {
      finishedAtIso: new Date().toISOString(),
      durationMs: Math.round(performance.now() - response.config._latencyMeta.startedAtMs),
      statusCategory: 'SUCCESS',
      httpStatus: response.status,
      responseBytes: estimatePayloadBytes(response.data),
    })
    return response
  },
  async (error: AxiosError) => {
    const original = error.config as any
    if (original?._latencyMeta) {
      finalizeLatencyRecord(original, {
        finishedAtIso: new Date().toISOString(),
        durationMs: Math.round(performance.now() - original._latencyMeta.startedAtMs),
        statusCategory: normalizeLatencyStatus(error),
        httpStatus: error.response?.status ?? null,
        responseBytes: estimatePayloadBytes(error.response?.data),
        errorMessage: error.message,
      })
    }
    return Promise.reject(error)
  },
)
```

- [ ] **Step 7: Run the core test suite again**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
```

Expected: PASS with the new helper logic still green.

- [ ] **Step 8: Commit**

```bash
git add auto-parts-wms-vue/src/utils/request.ts auto-parts-wms-vue/src/composables/apiLatencyMonitorCore.ts auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs
git commit -m "feat: capture api latency in shared request layer"
```

## Task 4: Add the query page UI on the existing ERP list-page stack

**Files:**
- Create: `auto-parts-wms-vue/src/views/system/ApiLatencyMonitorView.vue`
- Modify: `auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `auto-parts-wms-vue/src/locales/en.ts`

- [ ] **Step 1: Write the failing page skeleton using the existing UI stack**

```vue
<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.apiLatencyMonitor') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input v-model="keyword" class="table-search table-search--wide" :placeholder="t('placeholder.keyword')" clearable />
            <el-select v-model="requestMethod" class="table-search table-search--narrow" :placeholder="t('field.method')" clearable />
            <el-select v-model="statusCategory" class="table-search table-search--narrow" :placeholder="t('field.result')" clearable />
            <el-input v-model.number="minDurationMs" class="table-search table-search--narrow" :placeholder="t('field.minDurationMs')" clearable />
            <el-date-picker v-model="dateRange" type="datetimerange" class="table-date-range table-date-range--compact" />
          </div>
          <div class="table-actions">
            <el-button type="primary">{{ t('action.search') }}</el-button>
            <el-button>{{ t('action.export') }}</el-button>
          </div>
        </div>
      </div>
    </div>
    <div class="table-card"></div>
  </div>
</template>
```

- [ ] **Step 2: Add the failing locale keys**

```ts
page: {
  apiLatencyMonitor: '接口耗时记录',
}
field: {
  minDurationMs: '最小耗时(ms)',
  maxDurationMs: '最大耗时(ms)',
  requestBytes: '请求大小',
  responseBytes: '响应大小',
  durationMs: '耗时(ms)',
}
```

Add matching English keys with the same structure.

- [ ] **Step 3: Run type-check to verify the page and locale references fail where wiring is incomplete**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'auto-parts-wms-vue/node_modules/vue-tsc/bin/vue-tsc.js' --build
```

Expected: FAIL because the page is not wired into routing yet and/or imports are incomplete.

- [ ] **Step 4: Implement the page with summary strip and `ErpDataTable`**

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useApiLatencyMonitor } from '@/composables/useApiLatencyMonitor'
import { usePageSizePreference } from '@/composables/usePageSizePreference'

const { t } = useI18n()
const monitor = useApiLatencyMonitor()

const keyword = ref('')
const requestMethod = ref('')
const statusCategory = ref('')
const routePath = ref('')
const minDurationMs = ref<number | undefined>()
const maxDurationMs = ref<number | undefined>()
const dateRange = ref<[Date, Date] | []>([])
const page = ref(1)
const size = ref(20)

const filteredRows = computed(() => {
  monitor.filter.value = {
    keyword: keyword.value,
    requestMethod: requestMethod.value,
    statusCategory: statusCategory.value,
    routePath: routePath.value,
    minDurationMs: minDurationMs.value ?? null,
    maxDurationMs: maxDurationMs.value ?? null,
  }
  return monitor.filteredRecords.value
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredRows.value.slice(start, start + size.value)
})

const summary = computed(() => monitor.summary.value)
```

Use `ErpDataTable` columns for:

```vue
<ErpDataTable :data="pagedRows" table-key="api-latency-monitor">
  <ErpDataTableColumn type="index" width="60" :label="t('table.index')" />
  <ErpDataTableColumn prop="startedAt" :label="t('field.createdTime')" min-width="180" />
  <ErpDataTableColumn prop="routeTitle" :label="t('field.page')" min-width="160" show-overflow-tooltip />
  <ErpDataTableColumn prop="requestMethod" :label="t('field.method')" min-width="100" />
  <ErpDataTableColumn prop="normalizedPath" :label="t('field.path')" min-width="220" show-overflow-tooltip />
  <ErpDataTableColumn prop="statusCategory" :label="t('field.result')" min-width="120" />
  <ErpDataTableColumn prop="durationMs" :label="t('field.durationMs')" min-width="120" />
  <ErpDataTableColumn prop="requestBytes" :label="t('field.requestBytes')" min-width="120" />
  <ErpDataTableColumn prop="responseBytes" :label="t('field.responseBytes')" min-width="120" />
  <ErpDataTableColumn prop="summary" :label="t('field.summary')" min-width="280" show-overflow-tooltip />
</ErpDataTable>
```

- [ ] **Step 5: Add export and clear actions minimally**

```ts
const exportRows = () => {
  const headers = ['startedAt', 'routeTitle', 'requestMethod', 'normalizedPath', 'statusCategory', 'durationMs', 'requestBytes', 'responseBytes', 'summary']
  const rows = filteredRows.value.map((item) => headers.map((key) => JSON.stringify(item[key as keyof typeof item] ?? '')).join(','))
  const csv = [headers.join(','), ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'api-latency-records.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}
```

- [ ] **Step 6: Run type-check to verify the page compiles**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'auto-parts-wms-vue/node_modules/vue-tsc/bin/vue-tsc.js' --build
```

Expected: PASS for the page and locale changes.

- [ ] **Step 7: Commit**

```bash
git add auto-parts-wms-vue/src/views/system/ApiLatencyMonitorView.vue auto-parts-wms-vue/src/locales/zh.ts auto-parts-wms-vue/src/locales/en.ts
git commit -m "feat: add api latency monitor page"
```

## Task 5: Add route wiring and finish integration checks

**Files:**
- Modify: `auto-parts-wms-vue/src/router/index.ts`
- Modify: `auto-parts-wms-vue/src/views/system/ApiLatencyMonitorView.vue`

- [ ] **Step 1: Add the failing route entry**

```ts
{
  path: 'api-latency-monitor',
  name: 'api-latency-monitor',
  component: () => import('../views/system/ApiLatencyMonitorView.vue'),
  meta: { title: '接口耗时记录', permission: 'api-latency-monitor:view' }
}
```

Place it near the other system pages such as `audit-logs` and `system-config`.

- [ ] **Step 2: Run type-check to surface any missing imports or page references**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'auto-parts-wms-vue/node_modules/vue-tsc/bin/vue-tsc.js' --build
```

Expected: FAIL only if the page still references missing fields or helpers.

- [ ] **Step 3: Fix the remaining integration details minimally**

Apply the missing details that type-check flags:

```ts
const formatBytes = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) return '-'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(2)} MB`
}

const formatStartedAt = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}
```

Render these with column slots instead of raw values where needed.

- [ ] **Step 4: Run the full front-end verification set**

Run:

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'auto-parts-wms-vue/src/composables/__tests__/apiLatencyMonitorCore.test.mjs'
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'auto-parts-wms-vue/node_modules/vue-tsc/bin/vue-tsc.js' --build
```

Expected:

- Node tests: PASS
- `vue-tsc`: PASS

- [ ] **Step 5: Commit**

```bash
git add auto-parts-wms-vue/src/router/index.ts auto-parts-wms-vue/src/views/system/ApiLatencyMonitorView.vue
git commit -m "feat: wire api latency monitor route"
```

## Self-Review

### Spec coverage

- Shared request-layer timing capture: covered by Task 3.
- Local-only bounded storage: covered by Tasks 1 and 2.
- ERP-style query page with shared table component: covered by Task 4.
- Route-protected access path: covered by Task 5.
- Long-text tooltip behavior and sortable/filterable records: covered by Tasks 1 and 4.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Each code-changing step includes concrete file paths and code blocks.
- Every verification step includes an exact command and expected outcome.

### Type consistency

- Shared record type name is `ApiLatencyRecord` across core and reactive wrapper.
- Shared filter type name is `ApiLatencyFilter`.
- Shared monitor entry point is `useApiLatencyMonitor`.
- Summary helper name is `buildLatencySummaryText`.

## Notes Before Execution

- This workspace already has unrelated modified files. During implementation, touch only the files listed above unless test or type errors prove an adjacent file must change.
- The route uses a new permission code `api-latency-monitor:view`. If the user later wants the page to appear in backend-driven menus or permission seeds, that should be handled as a follow-up because this plan intentionally keeps v1 front-end scoped.

