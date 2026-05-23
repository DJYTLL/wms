type Loader<T> = () => Promise<T>;

type CacheEntry<T> = {
  value?: T;
  inflight?: Promise<T>;
};

const buildKey = (resource: string, tenantId: number | string) => `${resource}::${tenantId}`;

export const createTenantScopedResourceCache = () => {
  const entries = new Map<string, CacheEntry<unknown>>();

  const getOrLoad = async <T>(resource: string, tenantId: number | string, loader: Loader<T>): Promise<T> => {
    const key = buildKey(resource, tenantId);
    const existing = entries.get(key) as CacheEntry<T> | undefined;
    if (existing?.value !== undefined) {
      return existing.value;
    }
    if (existing?.inflight) {
      return existing.inflight;
    }
    const inflight = loader().then((value) => {
      entries.set(key, { value });
      return value;
    }).catch((error) => {
      entries.delete(key);
      throw error;
    });
    entries.set(key, { inflight });
    return inflight;
  };

  const invalidate = (resource?: string, tenantId?: number | string) => {
    if (!resource && tenantId == null) {
      entries.clear();
      return;
    }
    for (const key of entries.keys()) {
      const [entryResource, entryTenantId] = key.split('::');
      if (resource && entryResource !== resource) {
        continue;
      }
      if (tenantId != null && entryTenantId !== String(tenantId)) {
        continue;
      }
      entries.delete(key);
    }
  };

  return {
    getOrLoad,
    invalidate
  };
};
