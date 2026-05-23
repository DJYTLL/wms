import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessageBox } from 'element-plus'

import { useApiLatencyMonitor } from '../composables/useApiLatencyMonitor'
import {
  buildLatencySummaryText,
  estimatePayloadBytes,
  normalizeLatencyStatus,
  type ApiLatencyStatusCategory,
} from '../composables/apiLatencyMonitorCore'
import { createRequestRefreshQueue } from './requestRefreshQueueCore'

type DeletePromptConfig = {
  skipDeleteReasonPrompt?: boolean
  deletePromptEntityName?: string
  deletePromptTitle?: string
  deletePromptMessage?: string
}

type DeletePromptContext = {
  entityName: string
  title: string
  message: string
  placeholder: string
  confirmButtonText: string
}

type LatencyRequestMeta = {
  startedAtMs: number
  startedAtIso: string
  routePath: string
  routeTitle: string
  normalizedPath: string
  requestBytes: number | null
}

type LatencyAwareRequestConfig = InternalAxiosRequestConfig & {
  _latencyMeta?: LatencyRequestMeta
  _retry?: boolean
}

let routerModulePromise: Promise<typeof import('../router')> | null = null

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
})

const ACCESS_TOKEN_STORAGE_KEY = 'auth-access-token'

const readStoredToken = () => {
  if (typeof window === 'undefined') {
    return null
  }
  const stored = window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)
  return stored && stored.trim() ? stored : null
}

const persistToken = (token: string | null) => {
  if (typeof window === 'undefined') {
    return
  }
  if (token) {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token)
    return
  }
  window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY)
}

let accessToken: string | null = readStoredToken()

export const getToken = () => accessToken

const dispatchTokensUpdated = (token: string, authPayload?: unknown) => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(
      new CustomEvent('auth:tokens-updated', { detail: { token, authPayload } }),
    )
  }
}

const dispatchTokensCleared = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('auth:tokens-cleared'))
  }
}

export const setTokens = (token: string, authPayload?: unknown) => {
  accessToken = token
  persistToken(token)
  dispatchTokensUpdated(token, authPayload)
}

export const clearTokens = () => {
  accessToken = null
  persistToken(null)
  dispatchTokensCleared()
}

const hashString = (value: string) => {
  let hash = 0
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash).toString(16).padStart(8, '0')
}

const buildIdempotencyKey = (config: any) => {
  const method = (config.method || 'get').toLowerCase()
  const url = config.url || ''
  const data = typeof config.data === 'string' ? config.data : JSON.stringify(config.data || {})
  return hashString(`${method}:${url}:${data}`)
}

const resolveResponseErrorMessage = (data: any) => {
  if (!data) return ''
  if (typeof data === 'string') return data.trim()
  if (typeof data.message === 'string' && data.message.trim()) return data.message.trim()
  if (typeof data.errorMessage === 'string' && data.errorMessage.trim()) return data.errorMessage.trim()
  return ''
}

const isAuthEndpoint = (url?: string) => {
  if (!url) return false
  return url.includes('/login')
    || url.includes('/refresh')
    || url.includes('/logout')
}

const DELETE_PROMPT_ROUTES: Array<{ pattern: RegExp; entityName: string }> = [
  { pattern: /^\/users(\/|$)/, entityName: '用户' },
  { pattern: /^\/roles(\/|$)/, entityName: '角色' },
  { pattern: /^\/permissions(\/|$)/, entityName: '权限' },
  { pattern: /^\/menus(\/|$)/, entityName: '菜单' },
  { pattern: /^\/tenants(\/|$)/, entityName: '租户' },
  { pattern: /^\/erp\/assembly-orders(\/|$)/, entityName: '组装单' },
  { pattern: /^\/erp\/assembly-templates(\/|$)/, entityName: '组装模板' },
  { pattern: /^\/erp\/categories(\/|$)/, entityName: '分类' },
  { pattern: /^\/erp\/customer-categories(\/|$)/, entityName: '客户类别' },
  { pattern: /^\/erp\/customers(\/|$)/, entityName: '客户' },
  { pattern: /^\/erp\/delivery-methods(\/|$)/, entityName: '送货方式' },
  { pattern: /^\/erp\/locations(\/|$)/, entityName: '库位' },
  { pattern: /^\/erp\/payment-methods(\/|$)/, entityName: '付款方式' },
  { pattern: /^\/erp\/receipt-methods(\/|$)/, entityName: '收款方式' },
  { pattern: /^\/erp\/print-templates(\/|$)/, entityName: '打印模板' },
  { pattern: /^\/erp\/product-fitments(\/|$)/, entityName: '商品适配车型' },
  { pattern: /^\/erp\/products(\/|$)/, entityName: '商品' },
  { pattern: /^\/erp\/purchase-returns(\/|$)/, entityName: '采购退货单' },
  { pattern: /^\/erp\/sale-orders(\/|$)/, entityName: '销售单' },
  { pattern: /^\/erp\/sale-returns(\/|$)/, entityName: '销售退货单' },
  { pattern: /^\/erp\/settlement-methods(\/|$)/, entityName: '结算方式' },
  { pattern: /^\/erp\/suppliers(\/|$)/, entityName: '供应商' },
  { pattern: /^\/erp\/units(\/|$)/, entityName: '单位' },
  { pattern: /^\/erp\/vehicle-brands(\/|$)/, entityName: '车辆品牌' },
  { pattern: /^\/erp\/vehicle-models(\/|$)/, entityName: '车型' },
  { pattern: /^\/erp\/vehicle-series(\/|$)/, entityName: '车系' },
  { pattern: /^\/erp\/warehouses(\/|$)/, entityName: '仓库' },
]

const normalizeRequestUrl = (url?: string) => {
  if (!url) {
    return ''
  }
  const [path = ''] = url.split('?')
  return path.startsWith('/api/') ? path.slice(4) : path
}

const getRouterModule = async () => {
  if (!routerModulePromise) {
    routerModulePromise = import('../router')
  }
  return routerModulePromise
}

const readCurrentRouteContext = async () => {
  const fallbackPath = typeof window !== 'undefined' ? window.location.pathname : ''

  try {
    const routerModule = await getRouterModule()
    const currentRoute = routerModule.default.currentRoute.value
    const routePath = String(currentRoute.path || fallbackPath || '')
    const rawTitle = currentRoute.meta?.title
    const routeTitle = typeof rawTitle === 'string' && rawTitle.trim()
      ? rawTitle.trim()
      : routePath

    return {
      routePath,
      routeTitle,
    }
  } catch {
    return {
      routePath: fallbackPath,
      routeTitle: fallbackPath,
    }
  }
}

const buildLatencyMeta = async (config: LatencyAwareRequestConfig): Promise<LatencyRequestMeta> => {
  const routeContext = await readCurrentRouteContext()
  return {
    startedAtMs: performance.now(),
    startedAtIso: new Date().toISOString(),
    routePath: routeContext.routePath,
    routeTitle: routeContext.routeTitle,
    normalizedPath: normalizeRequestUrl(config.url),
    requestBytes: estimatePayloadBytes(config.data),
  }
}

const finalizeLatencyRecord = (
  config: LatencyAwareRequestConfig | undefined,
  payload: {
    finishedAtIso: string
    durationMs: number
    statusCategory: ApiLatencyStatusCategory
    httpStatus?: number | null
    responseBytes?: number | null
    errorMessage?: string | null
  },
) => {
  const meta = config?._latencyMeta as LatencyRequestMeta | undefined
  if (!meta) {
    return
  }

  const method = String(config?.method || 'GET').toUpperCase()
  const record = {
    id: `${meta.startedAtIso}-${method}-${meta.normalizedPath || config?.url || ''}`,
    startedAt: meta.startedAtIso,
    finishedAt: payload.finishedAtIso,
    durationMs: payload.durationMs,
    requestMethod: method,
    requestUrl: String(config?.url || ''),
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
  useApiLatencyMonitor().addRecord(record)
}

const buildLatencyDurationMs = (meta?: LatencyRequestMeta) => {
  if (!meta) {
    return 0
  }
  return Math.max(0, Math.round(performance.now() - meta.startedAtMs))
}

const resolveDeletePromptContext = (config: DeletePromptConfig & { url?: string }) => {
  const matchedEntity = DELETE_PROMPT_ROUTES.find(({ pattern }) => pattern.test(normalizeRequestUrl(config.url)))
    ?.entityName
  const entityName = config.deletePromptEntityName || matchedEntity || '数据'
  return {
    entityName,
    title: config.deletePromptTitle || `填写删除${entityName}原因`,
    message:
      config.deletePromptMessage
      || `请输入删除${entityName}的原因。删除后将无法恢复。`,
    placeholder: `请输入删除${entityName}的原因`,
    confirmButtonText: `确认删除${entityName}`,
  } satisfies DeletePromptContext
}

const ensureDeleteReason = async (config: LatencyAwareRequestConfig & DeletePromptConfig) => {
  const method = (config.method || 'get').toLowerCase()
  if (method !== 'delete' || isAuthEndpoint(config.url) || config.skipDeleteReasonPrompt) {
    return config
  }
  const currentReason = typeof config.data?.reason === 'string' ? config.data.reason.trim() : ''
  if (currentReason) {
    return config
  }
  const prompt = resolveDeletePromptContext(config)
  const { value } = await ElMessageBox.prompt(
    prompt.message,
    prompt.title,
    {
      confirmButtonText: prompt.confirmButtonText,
      cancelButtonText: '取消',
      inputPattern: /^(?=.*\S).{2,500}$/,
      inputErrorMessage: '删除原因至少 2 个字符',
      inputPlaceholder: prompt.placeholder,
      type: 'warning',
      distinguishCancelAndClose: true,
      closeOnClickModal: false,
      closeOnPressEscape: false,
    },
  )
  config.data = {
    ...(config.data || {}),
    reason: typeof value === 'string' ? value.trim() : value,
  }
  return config
}

// 请求拦截器：自动带上 token
request.interceptors.request.use(
  async (config: LatencyAwareRequestConfig) => {
    config._latencyMeta = await buildLatencyMeta(config)
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    const method = (config.method || 'get').toLowerCase()
    const url = config.url || ''
    const skipIdempotency = isAuthEndpoint(url)
      || url.includes('/system-configs')
    if (!skipIdempotency && method !== 'get' && method !== 'head' && method !== 'options') {
      config.headers = config.headers || {}
      if (!config.headers['Idempotency-Key']) {
        config.headers['Idempotency-Key'] = buildIdempotencyKey(config)
      }
    }
    return ensureDeleteReason(config)
  },
  (error) => Promise.reject(error),
)

let isRefreshing = false
const refreshQueue = createRequestRefreshQueue()

// 响应拦截器：401 刷新 token 并重试
request.interceptors.response.use(
  (response) => {
    const data = response.data
    const responseConfig = response.config as LatencyAwareRequestConfig
    const latencyMeta = responseConfig._latencyMeta

    if (response.config.responseType === 'blob') {
      finalizeLatencyRecord(responseConfig, {
        finishedAtIso: new Date().toISOString(),
        durationMs: buildLatencyDurationMs(latencyMeta),
        statusCategory: 'SUCCESS',
        httpStatus: response.status,
        responseBytes: estimatePayloadBytes(response.data),
      })
      return response
    }

    if (data && typeof data.code === 'number' && data.code !== 200) {
      const responseErrorMessage = resolveResponseErrorMessage(data) || data.message || 'Error'
      finalizeLatencyRecord(responseConfig, {
        finishedAtIso: new Date().toISOString(),
        durationMs: buildLatencyDurationMs(latencyMeta),
        statusCategory: 'FAIL',
        httpStatus: response.status,
        responseBytes: estimatePayloadBytes(response.data),
        errorMessage: responseErrorMessage,
      })
      if (data.code === 401) {
        clearTokens()
        window.location.href = '/login'
      }
      return Promise.reject(new Error(responseErrorMessage))
    }

    finalizeLatencyRecord(responseConfig, {
      finishedAtIso: new Date().toISOString(),
      durationMs: buildLatencyDurationMs(latencyMeta),
      statusCategory: 'SUCCESS',
      httpStatus: response.status,
      responseBytes: estimatePayloadBytes(response.data),
    })

    return response
  },
  async (error: AxiosError) => {
    const original = error.config as LatencyAwareRequestConfig | undefined
    const status = error.response?.status

    if (status === 401 && original && !original._retry && !isAuthEndpoint(original.url)) {
      original._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.enqueue({
            resolve,
            reject,
            retry: async (newToken: string) => {
              original.headers = original.headers || {}
              original.headers.Authorization = `Bearer ${newToken}`
              return request(original)
            },
          })
        })
      }

      isRefreshing = true
      try {
        const refreshRes = await axios.post('/api/refresh', {}, { withCredentials: true })

        const refreshData: any = refreshRes.data
        if (!refreshData || refreshData.code !== 200) {
          throw new Error(refreshData?.message || 'refresh failed')
        }

        const { token, authPayload } = refreshData.data
        setTokens(token, authPayload)

        refreshQueue.resolveAll(token)

        original.headers = original.headers || {}
        original.headers.Authorization = `Bearer ${token}`
        return request(original)
      } catch (e) {
        refreshQueue.rejectAll(e)
        finalizeLatencyRecord(original, {
          finishedAtIso: new Date().toISOString(),
          durationMs: buildLatencyDurationMs(original?._latencyMeta as LatencyRequestMeta | undefined),
          statusCategory: normalizeLatencyStatus(e as AxiosError),
          httpStatus: (e as AxiosError).response?.status ?? status ?? null,
          responseBytes: estimatePayloadBytes((e as AxiosError).response?.data),
          errorMessage: (e as Error).message,
        })
        clearTokens()
        window.location.href = '/login'
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }

    const apiMessage = resolveResponseErrorMessage(error.response?.data)
    if (apiMessage) {
      error.message = apiMessage
    }

    finalizeLatencyRecord(original, {
      finishedAtIso: new Date().toISOString(),
      durationMs: buildLatencyDurationMs(original?._latencyMeta as LatencyRequestMeta | undefined),
      statusCategory: normalizeLatencyStatus(error),
      httpStatus: error.response?.status ?? null,
      responseBytes: estimatePayloadBytes(error.response?.data),
      errorMessage: error.message,
    })

    console.error('Request Error:', error)
    return Promise.reject(error)
  },
)

export default request
