import { warmupColumnSettings } from '@/composables/useColumnSettings'
import { warmupUserTableSettings } from '@/composables/useUserTableSettings'
import request from '@/utils/request'
import { markErpNavigationPerf } from '@/utils/erpNavigationPerfTrace'

type SaleListWarmupConfig = {
  columnKey: string
  pageUrl: string
  status: string
}

type Loader<T> = () => Promise<T>

const SALE_LIST_CACHE_TTL_MS = 15000

export const saleRouteWarmupRequests: Record<string, SaleListWarmupConfig> = {
  '/erp/sale-orders/draft': { columnKey: 'erp-sale-draft', pageUrl: '/erp/sale-orders/draft/page', status: 'DRAFT' },
  '/erp/sale-orders/approved': { columnKey: 'erp-sale-approved', pageUrl: '/erp/sale-orders/approved/page', status: 'APPROVED,CANCELLED,RED_FLUSHED' },
  '/erp/sale-returns/draft': { columnKey: 'erp-sale-return-draft', pageUrl: '/erp/sale-returns/draft/page', status: 'DRAFT' },
  '/erp/sale-returns/approved': { columnKey: 'erp-sale-return-approved', pageUrl: '/erp/sale-returns/approved/page', status: 'APPROVED,RED_FLUSHED' }
}

const responseCache = new Map<string, { expiresAt: number; value?: unknown; inflight?: Promise<unknown> }>()
const warmupRouteExpiresAt = new Map<string, number>()

export const buildSaleListRequestKey = (url: string, params: Record<string, unknown>) => (
  `${url}?${JSON.stringify(params)}`
)

export const getCachedSaleListRequest = async <T>(key: string, loader: Loader<T>, ttlMs = SALE_LIST_CACHE_TTL_MS): Promise<T> => {
  const now = Date.now()
  const existing = responseCache.get(key) as { expiresAt: number; value?: T; inflight?: Promise<T> } | undefined
  if (existing && existing.expiresAt > now) {
    if (existing.value !== undefined) {
      markErpNavigationPerf('sale-list-cache:hit', { key })
      return existing.value
    }
    if (existing.inflight) {
      markErpNavigationPerf('sale-list-cache:inflight-hit', { key })
      return existing.inflight
    }
  }

  markErpNavigationPerf('sale-list-cache:miss', { key })
  const inflight = loader()
    .then((value) => {
      markErpNavigationPerf('sale-list-cache:store', { key })
      responseCache.set(key, { expiresAt: Date.now() + ttlMs, value })
      return value
    })
    .catch((error) => {
      responseCache.delete(key)
      throw error
    })
  responseCache.set(key, { expiresAt: now + ttlMs, inflight })
  return inflight
}

export const invalidateSaleListRequestCache = (key?: string) => {
  if (key) {
    responseCache.delete(key)
    return
  }
  responseCache.clear()
}

export const warmupSaleRouteData = (path: string) => {
  const now = Date.now()
  const warmup = saleRouteWarmupRequests[path]
  const warmupExpiresAt = warmupRouteExpiresAt.get(path)
  if (!warmup || (warmupExpiresAt && warmupExpiresAt > now)) {
    return
  }
  warmupRouteExpiresAt.set(path, now + SALE_LIST_CACHE_TTL_MS)

  const params = { page: 1, size: 20, status: warmup.status }
  const requestKey = buildSaleListRequestKey(warmup.pageUrl, params)

  void warmupColumnSettings(warmup.columnKey).catch(() => undefined)
  void warmupUserTableSettings(warmup.columnKey).catch(() => undefined)
  void getCachedSaleListRequest(requestKey, () => (
    request.get(warmup.pageUrl, { params })
  )).catch(() => undefined)
}
