import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const roleManagementSource = readFileSync(join(currentDir, '../RoleManagement.vue'), 'utf8')
const permissionManagementSource = readFileSync(join(currentDir, '../PermissionManagement.vue'), 'utf8')

test('role permission tree maps sql timing system-config permissions under sql latency monitor', () => {
  assert.match(
    roleManagementSource,
    /\{\s*prefix:\s*'system-config:sql-timing:',\s*pageKeys:\s*\['sql-latency-monitor'\]\s*\}/
  )
})

test('permission management maps sql timing system-config permissions to sql latency monitor resource', () => {
  assert.match(
    permissionManagementSource,
    /if\s*\(code\.startsWith\('system-config:sql-timing:'\)\)\s*return\s*'sql-latency-monitor'/
  )
})
