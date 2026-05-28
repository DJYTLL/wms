import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const roleManagementSource = readFileSync(join(currentDir, '../RoleManagement.vue'), 'utf8')

test('role permission tree loads the full menu catalog instead of sidebar-visible menus', () => {
  const loadTreeMenusBlockMatch = roleManagementSource.match(
    /const loadTreeMenus = async \(\) => \{[\s\S]*?\n\};/
  )

  assert.ok(loadTreeMenusBlockMatch, 'expected loadTreeMenus block to exist')

  const loadTreeMenusBlock = loadTreeMenusBlockMatch[0]
  assert.match(loadTreeMenusBlock, /request\.get\('\/menus\/all'\)/)
  assert.doesNotMatch(loadTreeMenusBlock, /actorIsSuperAdmin\.value/)
  assert.doesNotMatch(loadTreeMenusBlock, /menuStore\.fetchMenus\(\)/)
})
