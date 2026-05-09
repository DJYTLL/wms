<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.columnPermissionManagement') }}</div>

      <div class="top-card">
        <div class="top-card-main">
          <el-form label-width="96px" class="top-form">
            <div class="top-left">
              <el-form-item :label="t('field.mode')" class="mode-item">
                <el-radio-group v-model="mode">
                  <el-radio-button value="role">{{ t('field.roles') }}</el-radio-button>
                  <el-radio-button v-if="isSuperAdmin" value="tenant">{{
                    t('field.tenant')
                  }}</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </div>

            <div class="top-right">
              <el-form-item :label="t('field.page')">
                <el-select v-model="pageKey" placeholder="Select Page" class="top-select">
                  <el-option
                    v-for="page in pageOptions"
                    :key="page.key"
                    :label="page.label"
                    :value="page.key"
                  />
                </el-select>
              </el-form-item>

              <el-form-item v-if="mode === 'role'" :label="t('field.roles')">
                <el-select v-model="selectedRoleId" placeholder="Select Role" class="top-select">
                  <el-option
                    v-for="role in roleOptions"
                    :key="role.id"
                    :label="role.name"
                    :value="role.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item v-if="mode === 'tenant' && isSuperAdmin" :label="t('field.tenant')">
                <el-select v-model="selectedTenantId" placeholder="Select Tenant" class="top-select">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
            </div>
          </el-form>

          <div class="card-actions">
            <el-button
              v-if="mode === 'role'"
              type="primary"
              :disabled="!selectedRoleId || currentPagePermissions.length === 0"
              @click="saveRolePermissions"
            >
              {{ t('action.save') }}
            </el-button>

            <el-button
              v-else
              type="primary"
              :disabled="!selectedTenantId || currentPagePermissions.length === 0"
              @click="saveTenantColumns"
            >
              {{ t('action.save') }}
            </el-button>
          </div>
        </div>

        <div class="tenant-hint">
          <el-tag type="info" effect="plain">
            {{ t('field.tenant') }} {{ t('field.limit') }}: {{ tenantLimitSummary }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="main-headers">
      <div class="main-header">
        {{ mode === 'role' ? t('field.roleColumnConfig') : t('field.tenantColumnConfig') }}
      </div>
    </div>

    <div class="config-card control-card">
        <div v-if="!pageKey" class="empty-tip">
          {{ t('message.required') }}
        </div>

        <div v-if="mode === 'tenant' && isSuperAdmin && pageKey" class="tenant-tools">
          <el-button size="small" @click="selectAllTenantColumns">
            {{ t('action.selectAll') }}
          </el-button>
          <el-button size="small" type="primary" plain @click="selectAllTenantColumns">
            {{ t('action.resetDefault') }}
          </el-button>
        </div>

        <div v-if="pageKey" class="checkbox-panel">
          <el-empty v-if="currentPagePermissions.length === 0" :description="t('table.empty')" />

          <el-checkbox-group v-else-if="mode === 'role'" v-model="selectedRolePermissionIds">
            <el-checkbox
            v-for="permission in currentPagePermissions"
            :key="permission.id"
            :value="permission.id"
          >
            {{ permission.name }}
          </el-checkbox>
        </el-checkbox-group>

        <el-checkbox-group v-else v-model="tenantVisibleColumns">
          <el-checkbox
            v-for="permission in currentPagePermissions"
            :key="permission.id"
            :value="permission.columnKey"
          >
            {{ permission.name }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </div>

    <div class="main-headers">
      <div class="main-header">{{ t('field.preview') }}</div>
    </div>

    <div class="config-card preview-card">
      <div class="preview-table-wrapper">
        <el-table :data="previewRows" height="100%" stripe>
          <el-table-column type="index" width="64" :label="t('table.index')" />
          <el-table-column
            v-for="column in previewColumns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            min-width="140"
            show-overflow-tooltip
          />
        </el-table>
      </div>

      <div v-if="previewColumns.length === 0" class="empty-tip">
        {{ t('table.empty') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'

type ModeType = 'role' | 'tenant'

type PermissionItem = {
  id: number
  code: string
  name: string
  columnKey: string
  pageKey: string
}

type RoleOption = {
  id: number
  code: string
  name: string
}

type TenantOption = {
  id: number
  name: string
}

type TenantColumnResponse = {
  pageKey: string
  visibleColumns: string[]
  updatedBy?: string | null
  updatedAt?: string | null
}

const { t } = useI18n()
const authStore = useAuthStore()
const { notifyError, notifySuccess, notifyWarning } = useApiError()

const isSuperAdmin = computed(() => authStore.hasRole('super_admin'))

const mode = ref<ModeType>('role')
const pageKey = ref<string>('')

const permissions = ref<PermissionItem[]>([])
const roleOptions = ref<RoleOption[]>([])
const tenantOptions = ref<TenantOption[]>([])

const selectedRoleId = ref<number | null>(null)
const selectedRolePermissionIds = ref<number[]>([])
const fullRolePermissionIds = ref<number[]>([])
const updatingRoleSelection = ref(false)

const selectedTenantId = ref<number | null>(null)
const tenantVisibleColumns = ref<string[]>([])

const tenantAllowedKeys = ref<string[]>([])
const tenantSettingExists = ref(false)

const pageLabelMap = computed<Record<string, string>>(() => ({
  'user-management': t('page.userManagement'),
  'role-management': t('page.roleManagement'),
  'permission-management': t('page.permissionManagement'),
  'menu-management': t('page.menuManagement'),
  'tenant-management': t('page.tenantManagement'),
  'audit-logs': t('page.auditLogManagement'),
  'system-configs': t('page.systemConfigManagement'),
  'warehouse-management': t('page.warehouseManagement'),
  'shelf-management': t('page.shelfManagement'),
  'product-management': t('page.productManagement'),
  'supplier-management': t('page.supplierManagement'),
  'category-management': t('page.categoryManagement'),
  'unit-management': t('page.unitManagement'),
  'inbound-management': t('page.inboundManagement'),
  'erp-product': t('page.erpProductManagement'),
  'erp-customer': t('page.erpCustomerManagement'),
  'erp-supplier': t('page.erpSupplierManagement'),
  'erp-warehouse': t('page.erpWarehouseManagement'),
  'erp-location': t('page.erpLocationManagement'),
  'erp-category': t('page.erpCategoryManagement'),
  'erp-unit': t('page.erpUnitManagement'),
  'erp-purchase': t('page.erpPurchaseOrderManagement'),
  'erp-sale': t('page.erpSaleOrderManagement'),
  'erp-sale-return': t('page.erpSaleReturnManagement'),
  'erp-stock': t('page.erpStockManagement'),
  'erp-stock-txn': t('page.erpStockTxnManagement'),
}))

const pageOptions = computed(() => {
  const keys = Array.from(new Set(permissions.value.map((item) => item.pageKey)))
  return keys.map((key) => ({
    key,
    label: pageLabelMap.value[key] || key,
  }))
})

const currentPageLabel = computed(() => {
  if (!pageKey.value) return '-'
  return pageLabelMap.value[pageKey.value] || pageKey.value
})

const basePagePermissions = computed(() => {
  if (!pageKey.value) return [] as PermissionItem[]
  return permissions.value.filter((item) => item.pageKey === pageKey.value)
})

const allPageColumnKeys = computed(() => {
  return basePagePermissions.value.map((item) => item.columnKey)
})

const allowedKeysSet = computed(() => new Set(tenantAllowedKeys.value))

const roleEditablePermissions = computed(() => {
  if (!pageKey.value) return [] as PermissionItem[]
  return basePagePermissions.value.filter((item) => allowedKeysSet.value.has(item.columnKey))
})

const currentPagePermissions = computed(() => {
  if (!pageKey.value) return [] as PermissionItem[]
  if (mode.value === 'tenant' && isSuperAdmin.value) {
    // 超级管理员在租户模式下需要看到所有可配置列，避免取消后无法再勾选
    return basePagePermissions.value
  }
  // 角色模式必须受租户列限制约束：未分配的列不渲染也不可操作
  return roleEditablePermissions.value
})

const tenantLimitSummary = computed(() => {
  if (!pageKey.value) return t('table.empty')
  const allowedCount = tenantAllowedKeys.value.length
  const totalCount = allPageColumnKeys.value.length
  if (!tenantSettingExists.value) {
    return `${allowedCount}/${totalCount} (${t('status.default')})`
  }
  return `${allowedCount}/${totalCount}`
})

const clampRoleSelections = () => {
  sanitizeFullForCurrentPage()
  setRoleSelectionFromFull()
}

const clampTenantSelections = () => {
  if (basePagePermissions.value.length === 0) {
    tenantVisibleColumns.value = []
    return
  }
  const allowedKeys = new Set(basePagePermissions.value.map((item) => item.columnKey))
  tenantVisibleColumns.value = tenantVisibleColumns.value.filter((key) => allowedKeys.has(key))
}

const sanitizeFullForCurrentPage = () => {
  if (basePagePermissions.value.length === 0) {
    return
  }
  const pageIds = new Set(basePagePermissions.value.map((item) => item.id))
  const allowedIds = new Set(roleEditablePermissions.value.map((item) => item.id))
  fullRolePermissionIds.value = fullRolePermissionIds.value.filter((id) => {
    if (!pageIds.has(id)) return true
    return allowedIds.has(id)
  })
}

const setRoleSelectionFromFull = () => {
  const fullSet = new Set(fullRolePermissionIds.value)
  updatingRoleSelection.value = true
  selectedRolePermissionIds.value = roleEditablePermissions.value
    .filter((item) => fullSet.has(item.id))
    .map((item) => item.id)
  updatingRoleSelection.value = false
}

const mergeCurrentPageToFull = () => {
  const pageIds = new Set(basePagePermissions.value.map((item) => item.id))
  const allowedIds = new Set(roleEditablePermissions.value.map((item) => item.id))
  const next = new Set(fullRolePermissionIds.value.filter((id) => !pageIds.has(id)))
  selectedRolePermissionIds.value.forEach((id) => {
    if (allowedIds.has(id)) {
      next.add(id)
    }
  })
  fullRolePermissionIds.value = Array.from(next)
}

const loadColumnPermissions = async () => {
  try {
    const res: any = await request.get('/permissions/columns')
    const data = res.data.data || []
    const allowed = data.filter((item: any) => authStore.hasPermission(item.code))
    permissions.value = allowed.map((item: any) => {
      const parts = String(item.code || '').split(':')
      const parsedPageKey = parts.length > 1 ? parts[1] : ''
      const columnKey = parts.length > 2 ? parts.slice(2).join(':') : ''
      return {
        id: item.id,
        code: item.code,
        name: item.name || item.code,
        pageKey: parsedPageKey,
        columnKey,
      }
    })
  } catch (error) {
    notifyError(error)
  }
}

const loadRoleOptions = async () => {
  try {
    const res: any = await request.get('/roles/options')
    roleOptions.value = res.data.data || []
  } catch (error) {
    notifyError(error)
  }
}

const loadTenantOptions = async () => {
  if (!isSuperAdmin.value) return
  try {
    const res: any = await request.get('/tenants')
    tenantOptions.value = res.data.data || []
  } catch (error) {
    notifyError(error)
  }
}

const loadRolePermissions = async () => {
  if (!selectedRoleId.value) {
    selectedRolePermissionIds.value = []
    fullRolePermissionIds.value = []
    return
  }
  try {
    const res: any = await request.get(`/roles/${selectedRoleId.value}/column-permissions`)
    const data = res.data.data || []
    fullRolePermissionIds.value = data.map((item: any) => item.id)
    clampRoleSelections()
  } catch (error) {
    notifyError(error)
  }
}

const resolveAllowedKeysFromTenantResponse = (resp: TenantColumnResponse | null) => {
  const allKeys = allPageColumnKeys.value
  if (!resp) {
    tenantSettingExists.value = false
    tenantAllowedKeys.value = [...allKeys]
    return
  }

  const hasSetting = Boolean(resp.updatedAt || resp.updatedBy)
  tenantSettingExists.value = hasSetting

  const rawKeys = Array.isArray(resp.visibleColumns) ? resp.visibleColumns : []
  if (!hasSetting && rawKeys.length === 0) {
    tenantAllowedKeys.value = [...allKeys]
    return
  }
  tenantAllowedKeys.value = rawKeys
}

const loadTenantSettingForCurrentContext = async () => {
  if (!pageKey.value) {
    tenantAllowedKeys.value = []
    tenantSettingExists.value = false
    return
  }

  try {
    if (mode.value === 'tenant' && isSuperAdmin.value) {
      if (!selectedTenantId.value) {
        tenantAllowedKeys.value = []
        tenantVisibleColumns.value = []
        tenantSettingExists.value = false
        return
      }
      const res: any = await request.get(
        `/tenants/${selectedTenantId.value}/columns/${pageKey.value}`,
      )
      const data: TenantColumnResponse = res.data.data || {
        pageKey: pageKey.value,
        visibleColumns: [],
      }
      resolveAllowedKeysFromTenantResponse(data)

      const allKeys = allPageColumnKeys.value
      if (!tenantSettingExists.value && tenantAllowedKeys.value.length === 0) {
        tenantVisibleColumns.value = [...allKeys]
      } else {
        tenantVisibleColumns.value = [...tenantAllowedKeys.value]
      }
      clampTenantSelections()
      return
    }

    const res: any = await request.get(`/tenant-columns/${pageKey.value}`)
    const data: TenantColumnResponse = res.data.data || {
      pageKey: pageKey.value,
      visibleColumns: [],
    }
    resolveAllowedKeysFromTenantResponse(data)
    clampRoleSelections()
  } catch (error) {
    notifyError(error)
  }
}

const saveRolePermissions = async () => {
  if (!selectedRoleId.value) {
    notifyWarning(t('message.required'))
    return
  }
  try {
    await request.put(`/roles/${selectedRoleId.value}/column-permissions`, {
      permissionIds: fullRolePermissionIds.value,
    })
    notifySuccess()
  } catch (error) {
    notifyError(error)
  }
}

const saveTenantColumns = async () => {
  if (!selectedTenantId.value || !pageKey.value) {
    notifyWarning(t('message.required'))
    return
  }
  try {
    await request.put(`/tenants/${selectedTenantId.value}/columns/${pageKey.value}`, {
      visibleColumns: tenantVisibleColumns.value,
    })
    tenantAllowedKeys.value = [...tenantVisibleColumns.value]
    tenantSettingExists.value = true
    notifySuccess()
  } catch (error) {
    notifyError(error)
  }
}

const selectAllTenantColumns = () => {
  tenantVisibleColumns.value = [...allPageColumnKeys.value]
}

const previewColumns = computed(() => {
  if (currentPagePermissions.value.length === 0) return [] as Array<{ key: string; label: string }>

  if (mode.value === 'role') {
    const selectedIds = new Set(selectedRolePermissionIds.value)
    return currentPagePermissions.value
      .filter((item) => selectedIds.has(item.id))
      .map((item) => ({
        key: item.columnKey,
        label: item.name,
      }))
  }

  const visibleSet = new Set(tenantVisibleColumns.value)
  return currentPagePermissions.value
    .filter((item) => visibleSet.has(item.columnKey))
    .map((item) => ({
      key: item.columnKey,
      label: item.name,
    }))
})

const sampleValue = (key: string, index: number) => {
  const i = index + 1
  switch (key) {
    case 'username':
      return index === 0 ? 'admin' : `user${i}`
    case 'displayName':
      return index === 0 ? '系统管理员' : `用户${i}`
    case 'roles':
      return index === 0 ? '超级管理员' : '普通角色'
    case 'status':
      return index % 2 === 0 ? t('status.active') : t('status.inactive')
    case 'email':
      return `user${i}@example.com`
    case 'phone':
      return `1380000000${i}`
    case 'code':
      return `CODE-${i}`
    case 'name':
      return `名称-${i}`
    case 'orderNumber':
      return `ORD-2026-${String(i).padStart(3, '0')}`
    case 'price':
      return `${100 + i * 5}`
    case 'quantity':
      return `${10 * i}`
    case 'createdAt':
    case 'updatedAt':
    case 'date':
    case 'loginTime':
      return `2026-01-2${i} 10:00:00`
    default:
      return `${key}-${i}`
  }
}

const previewRows = computed(() => {
  const keys = previewColumns.value.map((item) => item.key)
  if (keys.length === 0) return [] as Record<string, string>[]

  return Array.from({ length: 3 }).map((_, index) => {
    const row: Record<string, string> = {}
    keys.forEach((key) => {
      row[key] = sampleValue(key, index)
    })
    return row
  })
})

watch(selectedRoleId, () => {
  loadRolePermissions()
})

watch([pageKey, mode, selectedTenantId], () => {
  loadTenantSettingForCurrentContext()
})

watch(selectedRolePermissionIds, () => {
  if (mode.value !== 'role') return
  if (updatingRoleSelection.value) return
  mergeCurrentPageToFull()
})

watch(roleEditablePermissions, () => {
  clampRoleSelections()
})

watch(basePagePermissions, () => {
  clampTenantSelections()
})

onMounted(async () => {
  await loadColumnPermissions()
  await loadRoleOptions()
  await loadTenantOptions()

  const firstPage = pageOptions.value[0]
  if (firstPage) {
    pageKey.value = firstPage.key
  }
})
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.page-subtitle {
  margin-top: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.top-card {
  width: 100%;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-card-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.top-form {
  display: grid;
  grid-template-columns: minmax(280px, 336px) minmax(336px, 1fr);
  column-gap: 12px;
  row-gap: 12px;
  align-items: start;
}

.top-select {
  width: 336px;
  max-width: 100%;
}

.top-form :deep(.el-form-item__label) {
  color: #606266;
}

.top-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.top-left {
  display: flex;
  align-items: flex-start;
  min-height: 100%;
}

.top-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-right :deep(.el-form-item) {
  margin-bottom: 0;
}

.mode-item {
  align-self: flex-start;
  margin-bottom: 0;
}

.tenant-hint {
  display: flex;
  justify-content: flex-end;
}

.main-headers {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 12px;
  align-items: end;
  margin-top: 16px;
}

.main-header {
  font-size: 16px;
  font-weight: 600;
}

.main-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
}

.config-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-headers + .config-card {
  margin-top: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.tenant-tools {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.control-card {
  overflow: hidden;
}

.checkbox-panel {
  flex: 1;
  min-height: 0;
  border: 1px solid #eef1f4;
  border-radius: 8px;
  padding: 12px;
  overflow: auto;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.preview-card {
  overflow: hidden;
}

.preview-table-wrapper {
  flex: 1;
  min-height: 0;
  border: 1px solid #eef1f4;
  border-radius: 8px;
  overflow: hidden;
}

.empty-tip {
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
}

@media (max-width: 1200px) {
  .top-card-main {
    grid-template-columns: 1fr;
  }

  .top-form {
    grid-template-columns: 1fr;
    row-gap: 12px;
  }

  .top-left {
    align-items: flex-start;
  }

  .top-select {
    width: 100%;
  }

  .card-actions {
    justify-content: flex-start;
  }

  .main-headers {
    display: none;
  }

  .main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
