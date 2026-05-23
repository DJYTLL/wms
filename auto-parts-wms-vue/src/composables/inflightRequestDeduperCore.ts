type Loader<T> = () => Promise<T>

export const createInflightRequestDeduper = () => {
  const inflight = new Map<string, Promise<unknown>>()

  const run = async <T>(key: string, loader: Loader<T>): Promise<T> => {
    const existing = inflight.get(key) as Promise<T> | undefined
    if (existing) {
      return existing
    }
    const promise = loader().finally(() => {
      inflight.delete(key)
    })
    inflight.set(key, promise)
    return promise
  }

  return {
    run
  }
}
