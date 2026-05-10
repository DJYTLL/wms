import axios, { type AxiosError, type AxiosInstance } from 'axios'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
})

let accessToken: string | null = null

export const getToken = () => accessToken

const dispatchTokensUpdated = (token: string) => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(
      new CustomEvent('auth:tokens-updated', { detail: { token } }),
    )
  }
}

const dispatchTokensCleared = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('auth:tokens-cleared'))
  }
}

export const setTokens = (token: string) => {
  accessToken = token
  dispatchTokensUpdated(token)
}

export const clearTokens = () => {
  accessToken = null
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

// 请求拦截器：自动带上 token
request.interceptors.request.use(
  (config) => {
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
    return config
  },
  (error) => Promise.reject(error),
)

let isRefreshing = false
let pendingQueue: Array<(token: string) => void> = []

// 响应拦截器：401 刷新 token 并重试
request.interceptors.response.use(
  (response) => {
    const data = response.data

    if (response.config.responseType === 'blob') {
      return response
    }

    if (data && typeof data.code === 'number' && data.code !== 200) {
      if (data.code === 401) {
        clearTokens()
        window.location.href = '/login'
      }
      return Promise.reject(new Error(data.message || 'Error'))
    }

    return response
  },
  async (error: AxiosError) => {
    const original = error.config as any
    const status = error.response?.status

    if (status === 401 && original && !original._retry && !isAuthEndpoint(original.url)) {
      original._retry = true

      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingQueue.push((newToken: string) => {
            original.headers = original.headers || {}
            original.headers.Authorization = `Bearer ${newToken}`
            resolve(request(original))
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

        const { token } = refreshData.data
        setTokens(token)

        pendingQueue.forEach((cb) => cb(token))
        pendingQueue = []

        original.headers = original.headers || {}
        original.headers.Authorization = `Bearer ${token}`
        return request(original)
      } catch (e) {
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

    console.error('Request Error:', error)
    return Promise.reject(error)
  },
)

export default request
