import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import test from 'node:test'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)
const source = readFileSync(join(__dirname, '../SqlLatencyMonitorView.vue'), 'utf8')

test('sql latency monitor view keeps actions in the first-row action area', () => {
  assert.ok(source.includes('<div class="table-actions">'))
  assert.ok(source.includes("{{ t('action.search') }}"))
  assert.ok(source.includes("{{ t('action.resetDefault') }}"))
  assert.ok(source.includes("{{ t('action.refresh') }}"))
})

test('sql latency monitor view loads request list and SQL entries', () => {
  assert.ok(source.includes("request.get('/system/sql-latency/requests/page'"))
  assert.ok(source.includes("request.get(`/system/sql-latency/requests/${selectedRequestId.value}/entries`)"))
  assert.ok(source.includes("{{ t('page.sqlLatencyMonitor') }}"))
  assert.ok(source.includes("{{ t('message.sqlLatencyMonitorEmpty') }}"))
})

test('sql latency monitor view exposes sql timing config toggles', () => {
  assert.ok(source.includes("request.get('/system-configs/wms.monitor.sql-timing-enabled'"))
  assert.ok(source.includes("saveBooleanConfig('wms.monitor.sql-timing-enabled'"))
  assert.ok(source.includes("saveBooleanConfig('wms.monitor.sql-timing-log-params'"))
  assert.ok(source.includes("SQL采集开关"))
})
