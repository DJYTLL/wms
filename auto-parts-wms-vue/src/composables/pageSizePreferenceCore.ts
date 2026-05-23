export type PageSizePreferenceSource = 'USER' | 'TENANT' | 'DEFAULT'

export const sanitizePageSize = (value: unknown): number | null => {
  const parsed = typeof value === 'number' ? value : Number(value)
  if (!Number.isInteger(parsed) || parsed < 5 || parsed > 200) {
    return null
  }
  return parsed
}

export const resolveEffectivePageSize = ({
  userPageSize,
  tenantDefaultPageSize,
  fallbackPageSize
}: {
  userPageSize: unknown
  tenantDefaultPageSize: unknown
  fallbackPageSize: number
}): { pageSize: number; source: PageSizePreferenceSource } => {
  const user = sanitizePageSize(userPageSize)
  if (user != null) {
    return { pageSize: user, source: 'USER' }
  }
  const tenant = sanitizePageSize(tenantDefaultPageSize)
  if (tenant != null) {
    return { pageSize: tenant, source: 'TENANT' }
  }
  return {
    pageSize: sanitizePageSize(fallbackPageSize) ?? 20,
    source: 'DEFAULT'
  }
}
