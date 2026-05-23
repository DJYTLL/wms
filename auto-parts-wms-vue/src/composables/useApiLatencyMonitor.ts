import { computed, readonly, ref } from 'vue'

import {
  appendLatencyRecord,
  buildLatencySummary,
  filterLatencyRecords,
  sortLatencyRecords
} from './apiLatencyMonitorCore.ts'
import type { ApiLatencyFilter, ApiLatencyRecord } from './apiLatencyMonitorCore.ts'

type ApiLatencyMonitorOptions = {
  maxRecords?: number
  slowThresholdMs?: number
}

export const createApiLatencyMonitor = (options: ApiLatencyMonitorOptions = {}) => {
  const maxRecords = options.maxRecords ?? 1000
  const slowThresholdMs = options.slowThresholdMs ?? 1000
  const internalRecords = ref<ApiLatencyRecord[]>([])
  const internalFilter = ref<ApiLatencyFilter>({})

  const records = readonly(internalRecords)
  const filter = readonly(internalFilter)

  const filteredRecords = computed(() => (
    sortLatencyRecords(filterLatencyRecords(internalRecords.value, internalFilter.value))
  ))

  const summary = computed(() => buildLatencySummary(internalRecords.value, slowThresholdMs))

  const addRecord = (record: ApiLatencyRecord) => {
    internalRecords.value = appendLatencyRecord(internalRecords.value, record, maxRecords)
  }

  const setFilter = (nextFilter: ApiLatencyFilter) => {
    internalFilter.value = { ...nextFilter }
  }

  const resetFilter = () => {
    internalFilter.value = {}
  }

  const clear = () => {
    internalRecords.value = []
    resetFilter()
  }

  return {
    records,
    filter,
    filteredRecords,
    summary,
    addRecord,
    setFilter,
    resetFilter,
    clear
  }
}

const sharedMonitor = createApiLatencyMonitor()

export const useApiLatencyMonitor = () => sharedMonitor
