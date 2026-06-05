<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.sqlLatencyMonitor') }}</div>
      <div v-if="canViewSqlTimingSettings" class="sql-timing-settings-card">
        <div class="sql-timing-settings-card__meta">
          <div class="sql-timing-settings-card__title">SQL采集开关</div>
          <div class="sql-timing-settings-card__hint">打开后会记录当前租户后续请求产生的 SQL 明细；关闭时不会新增记录。</div>
        </div>
        <div class="sql-timing-settings-card__actions">
          <div class="sql-timing-settings-card__toggle">
            <span>记录 SQL</span>
            <el-switch
              v-model="sqlTimingEnabled"
              :disabled="!canEditSqlTimingSettings"
              :loading="settingsSaving"
              @change="handleSqlTimingEnabledChange"
            />
          </div>
          <div class="sql-timing-settings-card__toggle">
            <span>记录参数摘要</span>
            <el-switch
              v-model="sqlTimingLogParams"
              :disabled="!canEditSqlTimingSettings"
              :loading="settingsSaving"
              @change="handleSqlTimingLogParamsChange"
            />
          </div>
        </div>
      </div>
      <div class="sql-latency-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="filters.requestPath"
              class="table-search sql-latency-toolbar__search--wide"
              :placeholder="t('placeholder.sqlLatencyRequestPath')"
              clearable
              @keyup.enter="loadRows"
            />
            <el-select
              v-model="filters.requestMethod"
              class="table-search sql-latency-toolbar__search--narrow"
              :placeholder="t('field.method')"
              clearable
            >
              <el-option v-for="item in methodOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-input
              v-model="filters.responseStatus"
              class="table-search sql-latency-toolbar__search--narrow"
              :placeholder="t('field.httpStatus')"
              clearable
              @keyup.enter="loadRows"
            />
            <el-input
              v-model="filters.minRequestCostMs"
              class="table-search sql-latency-toolbar__search--narrow"
              placeholder="接口耗时 >= ms"
              clearable
              @keyup.enter="loadRows"
            />
            <el-input
              v-model="filters.minSqlCostMs"
              class="table-search sql-latency-toolbar__search--narrow"
              placeholder="SQL耗时 >= ms"
              clearable
              @keyup.enter="loadRows"
            />
            <el-date-picker
              v-model="filters.dateRange"
              type="datetimerange"
              :range-separator="t('separator.to')"
              :start-placeholder="t('field.startTime')"
              :end-placeholder="t('field.endTime')"
              format="YYYY-MM-DD HH:mm:ss"
              class="table-date-range sql-latency-toolbar__date-range"
            />
          </div>
          <div class="table-actions">
            <el-button type="primary" @click="loadRows">{{ t('action.search') }}</el-button>
            <el-button @click="resetFilters">{{ t('action.resetDefault') }}</el-button>
            <el-button @click="refreshRows">{{ t('action.refresh') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body" v-loading="loading">
        <ErpDataTable :data="rows" table-key="sql-latency-monitor" :empty-text="t('table.empty')">
          <ErpDataTableColumn prop="startedAt" label="请求时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.startedAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="requestPath" label="请求路径" min-width="220" show-overflow-tooltip />
          <ErpDataTableColumn prop="requestMethod" :label="t('field.method')" min-width="100" />
          <ErpDataTableColumn prop="responseStatus" :label="t('field.httpStatus')" min-width="110" />
          <ErpDataTableColumn prop="requestCostMs" label="接口总耗时" min-width="120">
            <template #default="{ row }">{{ formatDuration(row.requestCostMs) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="sqlTotalCostMs" label="SQL总耗时" min-width="120">
            <template #default="{ row }">{{ formatDuration(row.sqlTotalCostMs) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="sqlCount" label="SQL条数" min-width="100" />
          <ErpDataTableColumn prop="username" label="用户" min-width="120" />
          <ErpDataTableColumn :label="t('table.actions')" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEntries(row.requestId)">查看 SQL 明细</el-button>
            </template>
          </ErpDataTableColumn>
          <template #empty>
            <div class="table-empty">{{ t('message.sqlLatencyMonitorEmpty') }}</div>
          </template>
        </ErpDataTable>
      </div>
      <div class="table-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-drawer v-model="entriesVisible" title="SQL 明细" size="60%">
      <div class="table-body" v-loading="entriesLoading">
        <ErpDataTable :data="entries" table-key="sql-latency-monitor-entries" :empty-text="t('table.empty')">
          <ErpDataTableColumn prop="sequenceNo" label="执行顺序" min-width="90" />
          <ErpDataTableColumn prop="mapperId" label="Mapper" min-width="220" show-overflow-tooltip />
          <ErpDataTableColumn prop="sqlType" label="SQL类型" min-width="100" />
          <ErpDataTableColumn prop="costMs" label="耗时" min-width="100">
            <template #default="{ row }">{{ formatDuration(row.costMs) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="sqlText" label="SQL文本" min-width="320" show-overflow-tooltip />
          <ErpDataTableColumn prop="paramsSummary" label="参数摘要" min-width="180" show-overflow-tooltip />
          <ErpDataTableColumn prop="executedAt" label="执行时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.executedAt) }}</template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'
import { useAuthStore } from '@/stores/auth'

type SqlRequestTraceRow = {
  requestId: string
  requestPath: string
  requestMethod: string
  responseStatus: number | null
  requestCostMs: number
  sqlTotalCostMs: number
  sqlCount: number
  username: string | null
  startedAt: string
  finishedAt: string
}

type SqlTraceEntryRow = {
  sequenceNo: number
  mapperId: string
  sqlType: string
  costMs: number
  sqlText: string
  paramsSummary: string | null
  executedAt: string
}

const { t } = useI18n()
const { notifyError } = useApiError()
const authStore = useAuthStore()

const loading = ref(false)
const entriesLoading = ref(false)
const entriesVisible = ref(false)
const settingsLoading = ref(false)
const settingsSaving = ref(false)
const selectedRequestId = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<SqlRequestTraceRow[]>([])
const entries = ref<SqlTraceEntryRow[]>([])
const sqlTimingEnabled = ref(false)
const sqlTimingLogParams = ref(false)
const canViewSqlTimingSettings = authStore.hasPermission('system-config:sql-timing:view')
  || authStore.hasPermission('system-config:sql-timing:edit')
const canEditSqlTimingSettings = authStore.hasPermission('system-config:sql-timing:edit')

const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']

const filters = reactive({
  requestPath: '',
  requestMethod: '',
  responseStatus: '',
  minRequestCostMs: '',
  minSqlCostMs: '',
  dateRange: null as [Date, Date] | null
})

const toNumberOrUndefined = (value: string) => {
  const normalized = value.trim()
  if (!normalized) return undefined
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : undefined
}

const loadRows = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: page.value,
      size: size.value,
      requestPath: filters.requestPath || undefined,
      requestMethod: filters.requestMethod || undefined,
      responseStatus: toNumberOrUndefined(filters.responseStatus),
      minRequestCostMs: toNumberOrUndefined(filters.minRequestCostMs),
      minSqlCostMs: toNumberOrUndefined(filters.minSqlCostMs),
    }
    if (filters.dateRange?.length === 2) {
      params.startAt = filters.dateRange[0].toISOString()
      params.endAt = filters.dateRange[1].toISOString()
    }
    const response: any = await request.get('/system/sql-latency/requests/page', { params })
    if (response.data.code === 200) {
      rows.value = response.data.data.items || []
      total.value = response.data.data.total || 0
    }
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

const loadSettings = async () => {
  if (!canViewSqlTimingSettings) return
  settingsLoading.value = true
  try {
    const [enabledResponse, paramsResponse] = await Promise.all([
      request.get('/system-configs/wms.monitor.sql-timing-enabled'),
      request.get('/system-configs/wms.monitor.sql-timing-log-params')
    ])
    sqlTimingEnabled.value = enabledResponse.data?.data?.value === 'true'
    sqlTimingLogParams.value = paramsResponse.data?.data?.value === 'true'
  } catch (error) {
    notifyError(error)
  } finally {
    settingsLoading.value = false
  }
}

const saveBooleanConfig = async (key: string, value: boolean, description: string) => {
  settingsSaving.value = true
  try {
    await request.put(`/system-configs/${key}`, {
      value: String(value),
      valueType: 'bool',
      description,
      isPublic: false
    })
  } finally {
    settingsSaving.value = false
  }
}

const handleSqlTimingEnabledChange = async (value: string | number | boolean) => {
  const nextValue = Boolean(value)
  try {
    await saveBooleanConfig('wms.monitor.sql-timing-enabled', nextValue, 'SQL耗时采集开关')
  } catch (error) {
    sqlTimingEnabled.value = !nextValue
    notifyError(error)
  }
}

const handleSqlTimingLogParamsChange = async (value: string | number | boolean) => {
  const nextValue = Boolean(value)
  try {
    await saveBooleanConfig('wms.monitor.sql-timing-log-params', nextValue, 'SQL耗时参数摘要开关')
  } catch (error) {
    sqlTimingLogParams.value = !nextValue
    notifyError(error)
  }
}

const loadEntries = async () => {
  if (!selectedRequestId.value) return
  entriesLoading.value = true
  try {
    const response: any = await request.get(`/system/sql-latency/requests/${selectedRequestId.value}/entries`)
    if (response.data.code === 200) {
      entries.value = response.data.data || []
    }
  } catch (error) {
    notifyError(error)
  } finally {
    entriesLoading.value = false
  }
}

const openEntries = async (requestId: string) => {
  selectedRequestId.value = requestId
  entriesVisible.value = true
  await loadEntries()
}

const resetFilters = () => {
  filters.requestPath = ''
  filters.requestMethod = ''
  filters.responseStatus = ''
  filters.minRequestCostMs = ''
  filters.minSqlCostMs = ''
  filters.dateRange = null
  page.value = 1
  loadRows()
}

const refreshRows = () => {
  loadRows()
}

const handlePageChange = (nextPage: number) => {
  page.value = nextPage
  loadRows()
}

const handleSizeChange = (nextSize: number) => {
  size.value = nextSize
  page.value = 1
  loadRows()
}

const formatDuration = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) return '-'
  return `${Math.round(value)} ms`
}

const formatDateTime = (value?: string | null) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

onMounted(() => {
  loadSettings()
  loadRows()
})
</script>

<style scoped>
.sql-timing-settings-card {
  width: 100%;
  margin-bottom: 16px;
  padding: 16px 18px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  box-sizing: border-box;
}

.sql-timing-settings-card__meta {
  min-width: 0;
}

.sql-timing-settings-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.sql-timing-settings-card__hint {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}

.sql-timing-settings-card__actions {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.sql-timing-settings-card__toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #334155;
  white-space: nowrap;
}

.sql-latency-toolbar {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  box-sizing: border-box;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.table-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

:deep(.sql-latency-toolbar__search--wide) {
  width: 240px;
  min-width: 240px;
}

:deep(.sql-latency-toolbar__search--narrow) {
  width: 140px;
  min-width: 140px;
}

:deep(.sql-latency-toolbar__date-range) {
  width: 360px;
  min-width: 360px;
}

@media (max-width: 1280px) {
  .sql-timing-settings-card {
    grid-template-columns: 1fr;
  }

  .sql-timing-settings-card__actions {
    justify-content: flex-start;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }

  :deep(.sql-latency-toolbar__search--wide),
  :deep(.sql-latency-toolbar__search--narrow),
  :deep(.sql-latency-toolbar__date-range) {
    width: 100% !important;
    min-width: 100% !important;
  }
}
</style>
