import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourcePath = resolve('src/views/system/ColumnPermissionManagement.vue')
const source = readFileSync(sourcePath, 'utf8')

const pageStatsMatch = source.match(/const pageColumnStats = computed[\s\S]*?\n}\)\n/)

if (!pageStatsMatch) {
  throw new Error('Unable to find pageColumnStats computed block.')
}

const pageStats = pageStatsMatch[0]
const tenantBranch = pageStats.split('} else {')[1] || ''

if (!source.includes('tenantPageSettingMap')) {
  throw new Error('Tenant tree stats must cache per-page tenant settings.')
}

if (!source.includes('loadAllTenantSettingsForStats')) {
  throw new Error('Tenant tree stats must bulk-load all tenant page settings.')
}

if (tenantBranch.includes('tenantVisibleColumns.value')) {
  throw new Error('Tenant tree stats must not use the current page tenantVisibleColumns.')
}

console.log('Column permission tenant tree stat checks passed.')
