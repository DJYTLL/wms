import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourcePath = resolve('src/views/system/TenantManagement.vue')
const source = readFileSync(sourcePath, 'utf8')

const requiredSnippets = [
  'tenant-menu-dialog',
  'tenant-menu-layout',
  'tenant-menu-sidebar',
  'tenant-menu-editor',
  'selectedMenuGroup',
  'menuGroupStats',
  'expandedMenuGroupIds',
  'toggleMenuGroupExpanded',
  'isMenuNodeVisibleInTenantConfig',
  "node.key !== 'dashboard'",
  "node.path !== '/'",
]

for (const snippet of requiredSnippets) {
  if (!source.includes(snippet)) {
    throw new Error(`Tenant menu config layout is missing ${snippet}.`)
  }
}

if (source.includes('仪表盘')) {
  throw new Error('Tenant menu config must not hard-code dashboard text.')
}

if (source.includes('<div class="menu-tree-wrapper">\n        <el-tree')) {
  throw new Error('Tenant menu config must not use the old single-column tree wrapper.')
}

console.log('Tenant menu config layout checks passed.')
