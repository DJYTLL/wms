export interface RoleEditPermissionContext {
  actorIsSuperAdmin: boolean
  actorTenantId?: number | null
  currentTenantId?: number | null
  actorRoleCodes?: string[]
  targetRoleCode?: string
  backendCanEditPermissions?: boolean | null
}

const normalizeCode = (code?: string) => (code || '').trim().toLowerCase()

export const canEditRolePermissions = ({
  actorIsSuperAdmin,
  actorTenantId,
  currentTenantId,
  actorRoleCodes = [],
  targetRoleCode,
  backendCanEditPermissions,
}: RoleEditPermissionContext) => {
  if (backendCanEditPermissions != null) {
    return backendCanEditPermissions
  }
  if (actorIsSuperAdmin) {
    return true
  }
  const sameTenant = actorTenantId == null || currentTenantId == null || actorTenantId === currentTenantId
  const isCurrentActorRole = sameTenant
    && actorRoleCodes.map(normalizeCode).includes(normalizeCode(targetRoleCode))
  if (isCurrentActorRole) {
    return false
  }
  return !['admin', 'super_admin'].includes(normalizeCode(targetRoleCode))
}
