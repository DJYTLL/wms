import test from 'node:test'
import assert from 'node:assert/strict'

import {
  canEditRolePermissions,
} from '../rolePermissionPolicy.ts'

test('switched super admin can edit target tenant admin role permissions', () => {
  assert.equal(canEditRolePermissions({
    actorIsSuperAdmin: true,
    actorTenantId: 1,
    currentTenantId: 6,
    actorRoleCodes: ['super_admin', 'admin'],
    targetRoleCode: 'admin',
  }), true)
})

test('tenant admin still cannot edit own admin role permissions', () => {
  assert.equal(canEditRolePermissions({
    actorIsSuperAdmin: false,
    actorTenantId: 6,
    currentTenantId: 6,
    actorRoleCodes: ['admin'],
    targetRoleCode: 'admin',
  }), false)
})

test('backend role permission capability overrides frontend role-code fallback', () => {
  assert.equal(canEditRolePermissions({
    actorIsSuperAdmin: false,
    actorTenantId: 6,
    currentTenantId: 6,
    actorRoleCodes: ['admin'],
    targetRoleCode: 'admin',
    backendCanEditPermissions: true,
  }), true)

  assert.equal(canEditRolePermissions({
    actorIsSuperAdmin: true,
    actorTenantId: 1,
    currentTenantId: 6,
    actorRoleCodes: ['super_admin'],
    targetRoleCode: 'ops',
    backendCanEditPermissions: false,
  }), false)
})
