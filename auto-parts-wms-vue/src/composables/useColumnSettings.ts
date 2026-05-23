import { computed, ref, watch } from 'vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'
import { createRequestStateCache } from '@/composables/requestStateCacheCore'

const buildStorageKey = (key: string) => `table-columns:${key}`
const columnSettingsCache = createRequestStateCache()

export const useColumnSettings = (key: string, defaultKeys: string[]) => {
  const { notifyError } = useApiError()
  const authStore = useAuthStore()
  const storageKey = buildStorageKey(key)
  const stored = localStorage.getItem(storageKey)
  const visibleKeys = ref<string[]>(stored ? JSON.parse(stored) : [...defaultKeys])
  const tenantAllowedKeys = ref<string[] | null>(null)

  const hasRoleColumnPermission = (columnKey: string) => {
    const permission = `column:${key}:${columnKey}`
    return authStore.hasPermission(permission) || authStore.hasPermission(`PERM_${permission}`)
  }

  const cacheKey = computed(() => `tenant-columns::${key}::${authStore.tenantId ?? authStore.tenantCode ?? 'default'}`)

  const effectiveKeys = computed(() => {
    // 租户/角色权限永远优先于当前用户的列偏好；用户设置只能在允许的列集合内生效。
    const tenantFilteredKeys = tenantAllowedKeys.value === null
      ? visibleKeys.value
      : visibleKeys.value.filter((keyItem) => tenantAllowedKeys.value?.includes(keyItem))

    return tenantFilteredKeys.filter(hasRoleColumnPermission)
  })

  const isVisible = (columnKey: string) => {
    return effectiveKeys.value.includes(columnKey)
  }

  const applyUnrestrictedTenantKeys = () => {
    tenantAllowedKeys.value = null
    const merged = new Set<string>([...visibleKeys.value, ...defaultKeys])
    visibleKeys.value = Array.from(merged).filter((item) => defaultKeys.includes(item))
  }

  const applyTenantKeys = (keys: string[]) => {
    tenantAllowedKeys.value = keys

    if (keys.length === 0) {
      visibleKeys.value = []
      return
    }

    // 关键修复：
    // 之前如果租户限制把列“裁短”了，localStorage 也会被裁短，
    // 当租户重新放开列时，visibleKeys 里已经没有这些列，导致永远显示不出来。
    // 这里用“默认列 ∩ 租户允许列”来补齐，再与当前可见列合并。
    const allowedDefaultKeys = defaultKeys.filter((item) => keys.includes(item))
    const merged = new Set<string>([...visibleKeys.value, ...allowedDefaultKeys])
    visibleKeys.value = Array.from(merged).filter((item) => keys.includes(item))
  }

  const fetchTenantKeys = async () => {
    try {
      const data = await columnSettingsCache.getOrLoad(cacheKey.value, async () => {
        const res: any = await request.get(`/tenant-columns/${key}`)
        return res.data.data
      })
      const keys = Array.isArray(data?.visibleColumns) ? data.visibleColumns : []
      const hasTenantSetting = Boolean(data?.updatedAt || data?.updatedBy)
      if (!hasTenantSetting) {
        applyUnrestrictedTenantKeys()
        return
      }
      applyTenantKeys(keys)
    } catch (error) {
      notifyError(error)
    }
  }

  const saveTenantKeys = async () => {
    try {
      const payload = { visibleColumns: visibleKeys.value }
      await request.put(`/tenant-columns/${key}`, payload)
      columnSettingsCache.set(cacheKey.value, {
        visibleColumns: [...visibleKeys.value],
        updatedAt: new Date().toISOString(),
        updatedBy: authStore.user?.username || authStore.user?.name || 'current-user'
      })
    } catch (error) {
      notifyError(error)
    }
  }

  const reset = () => {
    visibleKeys.value = [...defaultKeys]
  }

  watch(visibleKeys, (value) => {
    localStorage.setItem(storageKey, JSON.stringify(value))
  }, { deep: true })

  return {
    visibleKeys,
    isVisible,
    reset,
    fetchTenantKeys,
    saveTenantKeys,
    effectiveKeys
  }
}
