import { onActivated, onBeforeUnmount, onMounted, ref } from 'vue'
import request from '@/utils/request'

type ConfigItem = {
  key: string
  value: string
  valueType: string
  description?: string
  isPublic: boolean
}

const cached = ref<Record<string, string>>({})
let loaded = false

const loadFromStorage = () => {
  const raw = localStorage.getItem('system-config-public')
  if (raw) {
    try {
      cached.value = JSON.parse(raw)
      return true
    } catch {
      localStorage.removeItem('system-config-public')
    }
  }
  return false
}

const saveToStorage = () => {
  localStorage.setItem('system-config-public', JSON.stringify(cached.value))
}

const fetchPublicConfigs = async () => {
  if (loaded || loadFromStorage()) {
    loaded = true
    return cached.value
  }
  const res: any = await request.get('/system-configs/public')
  const data: ConfigItem[] = res.data.data || []
  cached.value = data.reduce((acc: Record<string, string>, item) => {
    acc[item.key] = item.value
    return acc
  }, {})
  saveToStorage()
  loaded = true
  return cached.value
}

const getDefaultPageSize = async () => {
  const configs = await fetchPublicConfigs()
  const raw = configs['default.page.size']
  const parsed = raw ? Number(raw) : 20
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 20
}

export const useSystemConfig = () => {
  const applyPageSize = async (sizeRef: { value: number }, reload: () => void) => {
    const defaultSize = await getDefaultPageSize()
    if (sizeRef.value !== defaultSize) {
      sizeRef.value = defaultSize
      reload()
    }
  }

  const bindPageSizeSync = (sizeRef: { value: number }, reload: () => void) => {
    const handler = () => {
      loaded = false
      cached.value = {}
      localStorage.removeItem('system-config-public')
      applyPageSize(sizeRef, reload)
    }
    handler()
    onMounted(handler)
    onActivated(handler)
    if (typeof window !== 'undefined') {
      window.addEventListener('system-config:refresh', handler)
      window.addEventListener('auth:tokens-updated', handler)
    }
    onBeforeUnmount(() => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('system-config:refresh', handler)
        window.removeEventListener('auth:tokens-updated', handler)
      }
    })
  }

  return {
    getDefaultPageSize,
    bindPageSizeSync
  }
}
