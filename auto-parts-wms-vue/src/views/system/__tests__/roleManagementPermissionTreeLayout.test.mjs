import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const roleManagementSource = readFileSync(join(currentDir, '../RoleManagement.vue'), 'utf8')

test('role permission tree labels wrap instead of truncating long menu names', () => {
  const permissionTextStyleMatch = roleManagementSource.match(
    /\.permission-tree-label__text\s*\{[\s\S]*?\n\}/
  )

  assert.ok(permissionTextStyleMatch, 'expected permission tree label text style to exist')

  const permissionTextStyle = permissionTextStyleMatch[0]
  assert.match(permissionTextStyle, /white-space:\s*normal;/)
  assert.doesNotMatch(permissionTextStyle, /text-overflow:\s*ellipsis;/)
  assert.doesNotMatch(permissionTextStyle, /white-space:\s*nowrap;/)
})
