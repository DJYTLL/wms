type RequestRefreshQueueEntry<T = unknown> = {
  resolve: (value: T | PromiseLike<T>) => void
  reject: (reason?: unknown) => void
  retry: (token: string) => Promise<T>
}

export const createRequestRefreshQueue = <T = unknown>() => {
  let entries: RequestRefreshQueueEntry<T>[] = []

  const enqueue = (entry: RequestRefreshQueueEntry<T>) => {
    entries.push(entry)
  }

  const resolveAll = (token: string) => {
    const currentEntries = entries
    entries = []

    currentEntries.forEach((entry) => {
      entry.retry(token).then(entry.resolve, entry.reject)
    })
  }

  const rejectAll = (error: unknown) => {
    const currentEntries = entries
    entries = []

    currentEntries.forEach((entry) => {
      entry.reject(error)
    })
  }

  const size = () => entries.length

  return {
    enqueue,
    resolveAll,
    rejectAll,
    size
  }
}
