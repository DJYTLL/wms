import { computed, ref, unref, type Ref } from 'vue'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'
import { useAuthStore } from '@/stores/auth'
import { createRequestStateCache } from '@/composables/requestStateCacheCore'

type ColumnLayout = {
  width?: number
  visible?: boolean
  fixed?: 'left' | 'right' | boolean
  order?: number
}

type UserTableConfig = {
  columns?: Record<string, ColumnLayout>
  table?: Record<string, unknown>
}

const toSafeConfig = (value: unknown): UserTableConfig => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return {}
  }
  return value as UserTableConfig
}

const userTableSettingsCache = createRequestStateCache()

export const useUserTableSettings = (pageKey: string | Ref<string>) => {
  const { notifyError } = useApiError()
  const authStore = useAuthStore()
  const config = ref<UserTableConfig>({})
  const loaded = ref(false)
  const currentPageKey = computed(() => unref(pageKey))
  const cacheKey = computed(() => {
    const userScope = authStore.user?.id
      ?? authStore.user?.username
      ?? authStore.user?.name
      ?? 'anonymous'
    const tenantScope = authStore.tenantId ?? authStore.tenantCode ?? 'default'
    return `user-table::${currentPageKey.value}::${tenantScope}::${userScope}`
  })

  const fetchConfig = async () => {
    if (!currentPageKey.value) {
      config.value = {}
      loaded.value = true
      return
    }
    loaded.value = false
    try {
      const nextConfig = await userTableSettingsCache.getOrLoad(cacheKey.value, async () => {
        const res: any = await request.get(`/user-table-settings/${currentPageKey.value}`)
        return toSafeConfig(res.data.data?.config)
      })
      config.value = nextConfig
    } catch (error) {
      notifyError(error)
      config.value = {}
    } finally {
      loaded.value = true
    }
  }

  const saveConfig = async () => {
    try {
      await request.put(`/user-table-settings/${currentPageKey.value}`, {
        config: config.value
      })
      userTableSettingsCache.set(cacheKey.value, toSafeConfig(config.value))
    } catch (error) {
      notifyError(error)
    }
  }

  const resetConfig = async () => {
    config.value = {}
    loaded.value = true
    await saveConfig()
  }

  const getColumnWidth = (columnKey: string, fallback: number) => {
    const width = config.value.columns?.[columnKey]?.width
    return typeof width === 'number' && Number.isFinite(width) && width > 0 ? width : fallback
  }

  const getColumnLayout = (columnKey: string) => config.value.columns?.[columnKey] || {}

  const setColumnLayout = async (columnKey: string, patch: ColumnLayout) => {
    const columns = { ...(config.value.columns || {}) }
    const current = columns[columnKey] || {}
    columns[columnKey] = {
      ...current,
      ...patch
    }
    config.value = {
      ...config.value,
      columns
    }
    await saveConfig()
  }

  const setColumnWidth = async (columnKey: string, width: number) => {
    if (!Number.isFinite(width) || width <= 0) {
      return
    }
    await setColumnLayout(columnKey, { width: Math.round(width) })
  }

  const setColumnVisible = async (columnKey: string, visible: boolean) => {
    await setColumnLayout(columnKey, { visible })
  }

  const setColumnFixed = async (columnKey: string, fixed: ColumnLayout['fixed']) => {
    await setColumnLayout(columnKey, { fixed })
  }

  const setColumnOrder = async (orders: Record<string, number>) => {
    const columns = { ...(config.value.columns || {}) }
    Object.entries(orders).forEach(([columnKey, order]) => {
      columns[columnKey] = {
        ...(columns[columnKey] || {}),
        order
      }
    })
    config.value = {
      ...config.value,
      columns
    }
    await saveConfig()
  }

  return {
    config,
    loaded,
    fetchConfig,
    saveConfig,
    resetConfig,
    getColumnLayout,
    getColumnWidth,
    setColumnWidth,
    setColumnVisible,
    setColumnFixed,
    setColumnOrder
  }
}
