<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.apiLatencyMonitor') }}</div>
      <div class="latency-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="draftFilters.keyword"
              class="table-search table-search--wide"
              :placeholder="t('placeholder.apiLatencyKeyword')"
              clearable
              @clear="applyFilters"
              @keyup.enter="applyFilters"
            />
            <el-select
              v-model="draftFilters.requestMethod"
              class="table-search table-search--narrow"
              :placeholder="t('field.method')"
              clearable
            >
              <el-option
                v-for="option in methodOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
            <el-select
              v-model="draftFilters.statusCategory"
              class="table-search table-search--narrow"
              :placeholder="t('field.result')"
              clearable
            >
              <el-option
                v-for="option in statusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-input
              v-model="draftFilters.minDurationMsInput"
              class="table-search table-search--narrow"
              :placeholder="t('field.minDurationMs')"
              clearable
              @keyup.enter="applyFilters"
            />
            <el-input
              v-model="draftFilters.maxDurationMsInput"
              class="table-search table-search--narrow"
              :placeholder="t('field.maxDurationMs')"
              clearable
              @keyup.enter="applyFilters"
            />
            <el-date-picker
              v-model="draftFilters.dateRange"
              type="datetimerange"
              :range-separator="t('separator.to')"
              :start-placeholder="t('field.startTime')"
              :end-placeholder="t('field.endTime')"
              format="YYYY-MM-DD HH:mm:ss"
              class="table-date-range latency-toolbar__date-range"
            />
          </div>
          <div class="table-actions">
            <el-button @click="showAdvancedFilters = !showAdvancedFilters">
              {{ showAdvancedFilters ? t('action.collapseFilters') : t('action.moreFilters') }}
            </el-button>
            <el-button type="primary" @click="applyFilters">{{ t('action.search') }}</el-button>
            <el-button @click="resetFilters">{{ t('action.resetDefault') }}</el-button>
            <el-button :disabled="!filteredRows.length" @click="exportRows">{{ t('action.export') }}</el-button>
            <el-button :disabled="!monitor.records.value.length" @click="clearRecords">{{ t('action.clear') }}</el-button>
          </div>
        </div>
        <div v-if="showAdvancedFilters" class="latency-toolbar__advanced">
          <el-select
            v-model="draftFilters.routePath"
            class="table-search table-search--wide"
            :placeholder="t('field.routePath')"
            filterable
            clearable
          >
            <el-option
              v-for="option in routeOptions"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
          <el-select
            v-model="draftFilters.routeTitle"
            class="table-search table-search--wide"
            :placeholder="t('field.page')"
            filterable
            clearable
          >
            <el-option
              v-for="option in pageOptions"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="latency-summary">
        <div class="latency-summary__item">
          <span class="latency-summary__label">{{ t('field.totalRequests') }}</span>
          <strong class="latency-summary__value">{{ filteredSummary.totalCount }}</strong>
        </div>
        <div class="latency-summary__item">
          <span class="latency-summary__label">{{ t('field.averageDurationMs') }}</span>
          <strong class="latency-summary__value">{{ formatDuration(filteredSummary.averageDurationMs) }}</strong>
        </div>
        <div class="latency-summary__item">
          <span class="latency-summary__label">{{ t('field.totalDurationMs') }}</span>
          <strong class="latency-summary__value">{{ formatDuration(filteredSummary.totalDurationMs) }}</strong>
        </div>
        <div class="latency-summary__item">
          <span class="latency-summary__label">{{ t('field.slowRequests') }}</span>
          <strong class="latency-summary__value latency-summary__value--warning">{{ filteredSummary.slowCount }}</strong>
        </div>
        <div class="latency-summary__item">
          <span class="latency-summary__label">{{ t('field.failureRequests') }}</span>
          <strong class="latency-summary__value latency-summary__value--danger">{{ filteredSummary.failureCount }}</strong>
        </div>
      </div>

      <div class="table-body" v-loading="!pageSizeSyncReady">
        <ErpDataTable
          :data="pageSizeSyncReady ? pagedRows : []"
          table-key="api-latency-monitor"
          :empty-text="t('table.empty')"
        >
          <ErpDataTableColumn type="index" width="60" :label="t('table.index')" />
          <ErpDataTableColumn prop="startedAt" :label="t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatStartedAt(row.startedAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="routeTitle" :label="t('field.page')" min-width="170" show-overflow-tooltip />
          <ErpDataTableColumn prop="routePath" :label="t('field.routePath')" min-width="180" show-overflow-tooltip />
          <ErpDataTableColumn prop="requestMethod" :label="t('field.method')" min-width="100" />
          <ErpDataTableColumn prop="normalizedPath" :label="t('field.path')" min-width="220" show-overflow-tooltip />
          <ErpDataTableColumn prop="statusCategory" :label="t('field.result')" min-width="120">
            <template #default="{ row }">
              <el-tag :type="resolveStatusTagType(row.statusCategory)" size="small">
                {{ resolveStatusLabel(row.statusCategory) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="httpStatus" :label="t('field.httpStatus')" min-width="110">
            <template #default="{ row }">
              {{ row.httpStatus ?? '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="durationMs" :label="t('field.durationMs')" min-width="120">
            <template #default="{ row }">
              <span :class="{ 'latency-duration--slow': row.durationMs >= SLOW_THRESHOLD_MS }">
                {{ formatDuration(row.durationMs) }}
              </span>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="requestBytes" :label="t('field.requestBytes')" min-width="120">
            <template #default="{ row }">
              {{ formatBytes(row.requestBytes) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="responseBytes" :label="t('field.responseBytes')" min-width="120">
            <template #default="{ row }">
              {{ formatBytes(row.responseBytes) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="summary" :label="t('field.summary')" min-width="320" show-overflow-tooltip />
          <ErpDataTableColumn prop="errorMessage" :label="t('field.errorMessage')" min-width="260" show-overflow-tooltip />
          <template #empty>
            <div class="table-empty">{{ t('message.apiLatencyMonitorEmpty') }}</div>
          </template>
        </ErpDataTable>
      </div>

      <div class="table-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredRows.length"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  buildLatencySummary,
  type ApiLatencyRecord,
  type ApiLatencyStatusCategory
} from '@/composables/apiLatencyMonitorCore.ts'
import { useApiLatencyMonitor } from '@/composables/useApiLatencyMonitor'
import { usePageSizePreference } from '@/composables/pageSizePreference'

type DraftFilters = {
  keyword: string
  requestMethod: string
  statusCategory: ApiLatencyStatusCategory | ''
  routePath: string
  routeTitle: string
  minDurationMsInput: string
  maxDurationMsInput: string
  dateRange: [Date, Date] | null
}

const SLOW_THRESHOLD_MS = 1000

const { t } = useI18n()
const monitor = useApiLatencyMonitor()
const { bindPageSizeSync } = usePageSizePreference()

const page = ref(1)
const size = ref(20)
const pageSizeSyncReady = ref(false)
const showAdvancedFilters = ref(false)

const createDraftFilters = (): DraftFilters => ({
  keyword: '',
  requestMethod: '',
  statusCategory: '',
  routePath: '',
  routeTitle: '',
  minDurationMsInput: '',
  maxDurationMsInput: '',
  dateRange: null
})

const draftFilters = reactive<DraftFilters>(createDraftFilters())

const statusOptions = computed(() => ([
  { value: 'SUCCESS', label: t('latencyStatus.success') },
  { value: 'FAIL', label: t('latencyStatus.fail') },
  { value: 'TIMEOUT', label: t('latencyStatus.timeout') },
  { value: 'CANCELLED', label: t('latencyStatus.cancelled') }
]))

const methodOptions = computed(() => {
  const methods = new Set<string>()
  monitor.records.value.forEach((record) => {
    if (record.requestMethod) {
      methods.add(record.requestMethod)
    }
  })
  return Array.from(methods.values()).sort()
})

const routeOptions = computed(() => {
  const routePaths = new Set<string>()
  monitor.records.value.forEach((record) => {
    if (record.routePath) {
      routePaths.add(record.routePath)
    }
  })
  return Array.from(routePaths.values()).sort()
})

const pageOptions = computed(() => {
  const titles = new Set<string>()
  monitor.records.value.forEach((record) => {
    if (record.routeTitle) {
      titles.add(record.routeTitle)
    }
  })
  return Array.from(titles.values()).sort()
})

const filteredRows = computed(() => {
  let rows = monitor.filteredRecords.value

  if (draftFilters.routeTitle) {
    rows = rows.filter((record) => record.routeTitle === draftFilters.routeTitle)
  }

  if (draftFilters.dateRange?.length === 2) {
    const [start, end] = draftFilters.dateRange
    const startTime = start.getTime()
    const endTime = end.getTime()
    rows = rows.filter((record) => {
      const time = new Date(record.startedAt).getTime()
      return !Number.isNaN(time) && time >= startTime && time <= endTime
    })
  }

  return rows
})

const filteredSummary = computed(() => buildLatencySummary(filteredRows.value, SLOW_THRESHOLD_MS))

const pagedRows = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredRows.value.slice(start, start + size.value)
})

const parseOptionalNumber = (value: string) => {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }
  const parsed = Number(trimmed)
  return Number.isFinite(parsed) ? parsed : null
}

const applyFilters = () => {
  monitor.setFilter({
    keyword: draftFilters.keyword.trim() || undefined,
    requestMethod: draftFilters.requestMethod || undefined,
    statusCategory: draftFilters.statusCategory || undefined,
    routePath: draftFilters.routePath || undefined,
    minDurationMs: parseOptionalNumber(draftFilters.minDurationMsInput),
    maxDurationMs: parseOptionalNumber(draftFilters.maxDurationMsInput)
  })
  page.value = 1
}

const resetDraftFilters = () => {
  Object.assign(draftFilters, createDraftFilters())
}

const resetFilters = () => {
  resetDraftFilters()
  monitor.resetFilter()
  page.value = 1
}

const clearRecords = () => {
  monitor.clear()
  resetDraftFilters()
  page.value = 1
}

const escapeCsvValue = (value: unknown) => {
  const normalized = value == null ? '' : String(value)
  return `"${normalized.replace(/"/g, '""')}"`
}

const exportRows = () => {
  const headers = [
    t('field.createdTime'),
    t('field.page'),
    t('field.routePath'),
    t('field.method'),
    t('field.path'),
    t('field.result'),
    t('field.httpStatus'),
    t('field.durationMs'),
    t('field.requestBytes'),
    t('field.responseBytes'),
    t('field.summary'),
    t('field.errorMessage')
  ]

  const rows = filteredRows.value.map((record) => ([
    formatStartedAt(record.startedAt),
    record.routeTitle || '',
    record.routePath || '',
    record.requestMethod || '',
    record.normalizedPath || '',
    resolveStatusLabel(record.statusCategory),
    record.httpStatus ?? '',
    record.durationMs,
    formatBytes(record.requestBytes),
    formatBytes(record.responseBytes),
    record.summary || '',
    record.errorMessage || ''
  ].map(escapeCsvValue).join(',')))

  const csv = [headers.map(escapeCsvValue).join(','), ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  const href = URL.createObjectURL(blob)
  link.href = href
  link.download = 'api-latency-records.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(href)
}

const formatStartedAt = (value?: string) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

const formatDuration = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '-'
  }
  return `${Math.round(value)} ms`
}

const formatBytes = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '-'
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / (1024 * 1024)).toFixed(2)} MB`
}

const resolveStatusLabel = (status?: ApiLatencyStatusCategory) => {
  if (status === 'SUCCESS') return t('latencyStatus.success')
  if (status === 'FAIL') return t('latencyStatus.fail')
  if (status === 'TIMEOUT') return t('latencyStatus.timeout')
  if (status === 'CANCELLED') return t('latencyStatus.cancelled')
  return '-'
}

const resolveStatusTagType = (status?: ApiLatencyStatusCategory) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAIL') return 'danger'
  if (status === 'TIMEOUT') return 'warning'
  if (status === 'CANCELLED') return 'info'
  return 'info'
}

const handlePageChange = (nextPage: number) => {
  page.value = nextPage
}

const handleSizeChange = (nextSize: number) => {
  size.value = nextSize
  page.value = 1
}

watch(
  () => [filteredRows.value.length, size.value],
  () => {
    const maxPage = Math.max(1, Math.ceil(filteredRows.value.length / size.value))
    if (page.value > maxPage) {
      page.value = maxPage
    }
  }
)

onMounted(() => {
  bindPageSizeSync(size, () => {
    page.value = 1
  }, {
    reloadOnInitialSync: false,
    onInitialSyncComplete: () => {
      pageSizeSyncReady.value = true
    }
  })
  applyFilters()
})
</script>

<style scoped>
.latency-toolbar {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  box-sizing: border-box;
}

.latency-toolbar__advanced {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 320px));
  gap: 12px;
  margin-top: 12px;
}

.latency-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  padding: 16px 18px 0;
}

.latency-summary__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.latency-summary__label {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.latency-summary__value {
  color: #111827;
  font-size: 20px;
  line-height: 28px;
}

.latency-summary__value--warning {
  color: #d97706;
}

.latency-summary__value--danger {
  color: #dc2626;
}

.latency-duration--slow {
  color: #d97706;
  font-weight: 600;
}

:deep(.latency-toolbar__date-range) {
  width: 380px;
}

:deep(.latency-toolbar__date-range.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.latency-toolbar__date-range .el-range-input) {
  width: 132px;
  font-size: 12px;
}

@media (max-width: 1600px) {
  .latency-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .latency-toolbar__advanced {
    grid-template-columns: 1fr;
  }

  .latency-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .latency-summary {
    grid-template-columns: 1fr;
  }
}
</style>
