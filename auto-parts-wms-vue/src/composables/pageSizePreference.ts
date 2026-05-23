import { onBeforeUnmount, watch } from 'vue'
import request from '@/utils/request'
import { sanitizePageSize, type PageSizePreferenceSource } from './pageSizePreferenceCore'

type TenantDisplaySettingsResponse = {
  defaultPageSize: number
  updatedAt?: string | null
  updatedBy?: string | null
}

type MyListPreferencesResponse = {
  pageSize: number | null
  updatedAt?: string | null
}

type EffectiveListPreferencesResponse = {
  pageSize: number
  source: PageSizePreferenceSource
}

const MY_PREFERENCE_EVENT = 'my-preferences:refresh'
const TENANT_SETTING_EVENT = 'tenant-settings:refresh'
const PAGE_SIZE_CACHE_KEY = 'my.preferences.page-size'
const BUILTIN_PAGE_SIZE = 20

const readCachedPageSize = () => {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    return sanitizePageSize(window.sessionStorage.getItem(PAGE_SIZE_CACHE_KEY))
  } catch {
    return null
  }
}

const writeCachedPageSize = (value: unknown) => {
  if (typeof window === 'undefined') {
    return
  }
  try {
    const parsed = sanitizePageSize(value)
    if (parsed == null) {
      window.sessionStorage.removeItem(PAGE_SIZE_CACHE_KEY)
      return
    }
    window.sessionStorage.setItem(PAGE_SIZE_CACHE_KEY, String(parsed))
  } catch {
    // Ignore storage failures and continue using runtime state.
  }
}

const clearCachedPageSize = () => {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.sessionStorage.removeItem(PAGE_SIZE_CACHE_KEY)
  } catch {
    // Ignore storage failures and continue using runtime state.
  }
}

export const usePageSizePreference = () => {
  const fetchTenantDisplaySettings = async () => {
    const res: any = await request.get('/tenant-settings/display')
    return (res.data.data || {}) as TenantDisplaySettingsResponse
  }

  const updateTenantDisplaySettings = async (defaultPageSize: number) => {
    const res: any = await request.put('/tenant-settings/display', {
      defaultPageSize
    })
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event(TENANT_SETTING_EVENT))
    }
    return (res.data.data || {}) as TenantDisplaySettingsResponse
  }

  const fetchMyListPreferences = async () => {
    const res: any = await request.get('/my/preferences/list')
    return (res.data.data || {}) as MyListPreferencesResponse
  }

  const updateMyListPreferences = async (pageSize: number) => {
    const res: any = await request.put('/my/preferences/list', { pageSize })
    const data = (res.data.data || {}) as MyListPreferencesResponse
    writeCachedPageSize(data.pageSize)
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event(MY_PREFERENCE_EVENT))
    }
    return data
  }

  const fetchEffectiveListPreferences = async () => {
    const res: any = await request.get('/my/preferences/list/effective')
    const data = (res.data.data || {}) as EffectiveListPreferencesResponse
    writeCachedPageSize(data.pageSize)
    return data
  }

  const bindPageSizeSync = (
    sizeRef: { value: number },
    reload: () => void,
    options?: {
      onInitialSyncComplete?: () => void
      reloadOnInitialSync?: boolean
    }
  ) => {
    const cachedPageSize = readCachedPageSize()
    let initialSyncCompleted = false
    let syncingFromServer = false
    const markInitialSyncComplete = () => {
      if (initialSyncCompleted) {
        return
      }
      initialSyncCompleted = true
      options?.onInitialSyncComplete?.()
    }
    if (cachedPageSize != null) {
      sizeRef.value = cachedPageSize
      markInitialSyncComplete()
    }
    const syncFromServer = async (allowReload = true) => {
      try {
        const result = await fetchEffectiveListPreferences()
        const effective = sanitizePageSize(result.pageSize) ?? BUILTIN_PAGE_SIZE
        if (sizeRef.value !== effective) {
          syncingFromServer = true
          sizeRef.value = effective
          if (allowReload) {
            reload()
          }
        }
      } catch {
        // Keep the current page size when preferences cannot be loaded.
      } finally {
        syncingFromServer = false
        markInitialSyncComplete()
      }
    }
    const syncFromServerAndReload = () => {
      syncFromServer(true)
    }
    if (cachedPageSize == null) {
      void syncFromServer(options?.reloadOnInitialSync !== false)
    }
    watch(() => sizeRef.value, (nextValue, previousValue) => {
      const nextPageSize = sanitizePageSize(nextValue)
      const previousPageSize = sanitizePageSize(previousValue)
      if (syncingFromServer || nextPageSize == null || nextPageSize === previousPageSize) {
        return
      }
      writeCachedPageSize(nextPageSize)
      void updateMyListPreferences(nextPageSize)
    })
    const handleTokensCleared = () => {
      clearCachedPageSize()
    }
    if (typeof window !== 'undefined') {
      window.addEventListener(MY_PREFERENCE_EVENT, syncFromServerAndReload)
      window.addEventListener(TENANT_SETTING_EVENT, syncFromServerAndReload)
      window.addEventListener('auth:tokens-updated', syncFromServerAndReload)
      window.addEventListener('auth:tokens-cleared', handleTokensCleared)
    }
    onBeforeUnmount(() => {
      if (typeof window !== 'undefined') {
        window.removeEventListener(MY_PREFERENCE_EVENT, syncFromServerAndReload)
        window.removeEventListener(TENANT_SETTING_EVENT, syncFromServerAndReload)
        window.removeEventListener('auth:tokens-updated', syncFromServerAndReload)
        window.removeEventListener('auth:tokens-cleared', handleTokensCleared)
      }
    })
  }

  return {
    fetchTenantDisplaySettings,
    updateTenantDisplaySettings,
    fetchMyListPreferences,
    updateMyListPreferences,
    fetchEffectiveListPreferences,
    bindPageSizeSync
  }
}
