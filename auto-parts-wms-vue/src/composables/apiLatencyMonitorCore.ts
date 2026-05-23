export type ApiLatencyStatusCategory = 'SUCCESS' | 'FAIL' | 'TIMEOUT' | 'CANCELLED'

export type ApiLatencyRecord = {
  id: string
  startedAt: string
  finishedAt?: string
  durationMs: number
  requestMethod?: string
  requestUrl?: string
  normalizedPath?: string
  routePath?: string
  routeTitle?: string
  statusCategory?: ApiLatencyStatusCategory
  httpStatus?: number | null
  requestBytes?: number | null
  responseBytes?: number | null
  errorMessage?: string | null
  summary?: string
}

export type ApiLatencyFilter = {
  keyword?: string
  requestMethod?: string
  statusCategory?: ApiLatencyStatusCategory
  routePath?: string
  minDurationMs?: number | null
  maxDurationMs?: number | null
}

export type ApiLatencySummary = {
  totalCount: number
  totalDurationMs: number
  averageDurationMs: number
  slowCount: number
  failureCount: number
}

type ErrorLike = {
  code?: string
  message?: string
} | null | undefined

const textEncoder = new TextEncoder()

const normalizeMaxRecords = (maxRecords: number) => {
  if (!Number.isFinite(maxRecords) || maxRecords <= 0) {
    return 0
  }
  return Math.floor(maxRecords)
}

const inferLatencyAction = (method?: string, path?: string) => {
  if (method === 'GET' && /\/page($|\?)/.test(path || '')) {
    return '分页查询'
  }
  if (method === 'GET') {
    return '查询'
  }
  if (method === 'POST') {
    return '新增或提交'
  }
  if (method === 'PUT') {
    return '更新'
  }
  if (method === 'DELETE') {
    return '删除'
  }
  return method || '请求'
}

export const appendLatencyRecord = (
  records: ApiLatencyRecord[],
  record: ApiLatencyRecord,
  maxRecords: number
) => {
  const limit = normalizeMaxRecords(maxRecords)
  if (limit === 0) {
    return []
  }
  return [record, ...records].slice(0, limit)
}

export const sortLatencyRecords = (records: ApiLatencyRecord[]) => (
  [...records].sort((left, right) => right.durationMs - left.durationMs)
)

export const filterLatencyRecords = (
  records: ApiLatencyRecord[],
  filter: ApiLatencyFilter = {}
) => records.filter((record) => {
  const keyword = filter.keyword?.trim()
  if (keyword) {
    const haystack = [
      record.normalizedPath,
      record.routePath,
      record.routeTitle,
      record.summary,
      record.errorMessage
    ].filter(Boolean).join(' ')
    if (!haystack.includes(keyword)) {
      return false
    }
  }

  if (filter.requestMethod && record.requestMethod !== filter.requestMethod) {
    return false
  }
  if (filter.statusCategory && record.statusCategory !== filter.statusCategory) {
    return false
  }
  if (filter.routePath && record.routePath !== filter.routePath) {
    return false
  }
  if (filter.minDurationMs != null && record.durationMs < filter.minDurationMs) {
    return false
  }
  if (filter.maxDurationMs != null && record.durationMs > filter.maxDurationMs) {
    return false
  }

  return true
})

export const buildLatencySummary = (
  records: ApiLatencyRecord[],
  slowThresholdMs: number
): ApiLatencySummary => {
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
    averageDurationMs: totalCount > 0 ? Math.round(totalDurationMs / totalCount) : 0,
    slowCount,
    failureCount
  }
}

export const estimatePayloadBytes = (payload: unknown): number | null => {
  if (payload == null) {
    return null
  }
  if (typeof payload === 'string') {
    return textEncoder.encode(payload).length
  }
  if (payload instanceof URLSearchParams) {
    return textEncoder.encode(payload.toString()).length
  }
  if (payload instanceof ArrayBuffer) {
    return payload.byteLength
  }
  if (ArrayBuffer.isView(payload)) {
    return payload.byteLength
  }
  if (
    (typeof Blob !== 'undefined' && payload instanceof Blob)
    || (typeof FormData !== 'undefined' && payload instanceof FormData)
  ) {
    return null
  }

  try {
    const serialized = JSON.stringify(payload)
    if (serialized === undefined) {
      return null
    }
    return textEncoder.encode(serialized).length
  } catch {
    return null
  }
}

export const normalizeLatencyStatus = (errorLike?: ErrorLike): ApiLatencyStatusCategory => {
  if (errorLike?.code === 'ERR_CANCELED') {
    return 'CANCELLED'
  }
  if (errorLike?.code === 'ECONNABORTED' || /timeout/i.test(errorLike?.message ?? '')) {
    return 'TIMEOUT'
  }
  return 'FAIL'
}

export const buildLatencySummaryText = (record: Partial<ApiLatencyRecord>) => {
  const action = inferLatencyAction(record.requestMethod, record.normalizedPath)
  const subject = record.routeTitle || record.normalizedPath || record.requestMethod || '接口'

  if (
    record.statusCategory === 'FAIL'
    || record.statusCategory === 'TIMEOUT'
    || record.statusCategory === 'CANCELLED'
  ) {
    return `${subject}${action}失败${record.errorMessage ? `：${record.errorMessage}` : ''}`
  }

  return `${subject}${action}`
}
