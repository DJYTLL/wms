type Loader<T> = () => Promise<T>

type Entry<T> = {
  value?: T
  inflight?: Promise<T>
}

export const createRequestStateCache = () => {
  const entries = new Map<string, Entry<unknown>>()

  const getOrLoad = async <T>(key: string, loader: Loader<T>): Promise<T> => {
    const existing = entries.get(key) as Entry<T> | undefined
    if (existing?.value !== undefined) {
      return existing.value
    }
    if (existing?.inflight) {
      return existing.inflight
    }
    const inflight = loader()
      .then((value) => {
        entries.set(key, { value })
        return value
      })
      .catch((error) => {
        entries.delete(key)
        throw error
      })
    entries.set(key, { inflight })
    return inflight
  }

  const set = <T>(key: string, value: T) => {
    entries.set(key, { value })
  }

  const invalidate = (key?: string) => {
    if (!key) {
      entries.clear()
      return
    }
    entries.delete(key)
  }

  return {
    getOrLoad,
    set,
    invalidate
  }
}
