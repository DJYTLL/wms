import { computed, ref, watch } from 'vue'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'

const buildStorageKey = (key: string) => `table-columns:${key}`

export const useColumnSettings = (key: string, defaultKeys: string[]) => {
  const { notifyError } = useApiError()
  const storageKey = buildStorageKey(key)
  const stored = localStorage.getItem(storageKey)
  const visibleKeys = ref<string[]>(stored ? JSON.parse(stored) : defaultKeys)
  const tenantAllowedKeys = ref<string[] | null>(null)

  const effectiveKeys = computed(() => {
    if (!tenantAllowedKeys.value || tenantAllowedKeys.value.length === 0) {
      return visibleKeys.value
    }
    return visibleKeys.value.filter((keyItem) => tenantAllowedKeys.value?.includes(keyItem))
  })

  const isVisible = (columnKey: string) => effectiveKeys.value.includes(columnKey)

  const applyTenantKeys = (keys: string[]) => {
    // 约定：租户未配置（空数组）表示“不限制”，此时恢复默认列
    if (!keys || keys.length === 0) {
      tenantAllowedKeys.value = null
      visibleKeys.value = [...defaultKeys]
      return
    }

    tenantAllowedKeys.value = keys

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
      const res: any = await request.get(`/tenant-columns/${key}`)
      const data = res.data.data
      const keys = Array.isArray(data?.visibleColumns) ? data.visibleColumns : []
      applyTenantKeys(keys)
    } catch (error) {
      notifyError(error)
    }
  }

  const saveTenantKeys = async () => {
    try {
      const payload = { visibleColumns: visibleKeys.value }
      await request.put(`/tenant-columns/${key}`, payload)
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
