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
                <el-popover
                  placement="bottom-start"
                  :width="360"
                  trigger="click"
                  popper-class="page-tree-popper"
                  v-model:visible="pageTreeVisible"
                >
                  <template #reference>
                    <button type="button" class="page-tree-trigger top-select">
                      <span :class="['page-tree-trigger__text', { 'is-placeholder': !pageKey }]">
                        {{ selectedPageLabel }}
                      </span>
                      <span class="page-tree-trigger__arrow" :class="{ 'is-open': pageTreeVisible }">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="6 9 12 15 18 9"></polyline>
                        </svg>
                      </span>
                    </button>
                  </template>

                  <div class="page-tree-dropdown">
                    <div v-for="item in pageTreeData" :key="item.id" class="page-tree-node page-tree-node--root">
                      <button
                        type="button"
                        class="page-tree-label page-tree-label--root"
                        :class="{ 'is-active': isTreeNodeActive(item) }"
                        @click.stop="handleTreeNodeClick(item)"
                      >
                        <span v-if="item.icon" class="page-tree-label__icon" v-html="item.icon"></span>
                        <span class="page-tree-label__text">{{ item.label }}</span>
                        <span
                          v-if="item.children.length"
                          class="page-tree-label__arrow"
                          :class="{ 'is-open': item.isOpen }"
                        >
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="9 18 15 12 9 6"></polyline>
                          </svg>
                        </span>
                      </button>

                      <div v-if="item.children.length && item.isOpen" class="page-tree-children">
                        <template v-for="child in item.children" :key="child.id">
                          <button
                            type="button"
                            class="page-tree-label page-tree-label--child"
                            :class="{
                              'is-active': isTreeNodeActive(child),
                              'is-leaf': child.selectable && child.children.length === 0,
                            }"
                            @click.stop="handleTreeNodeClick(child)"
                          >
                            <span
                              v-if="child.selectable && child.children.length === 0"
                              class="page-tree-label__bullet"
                              :class="{ 'is-active': isTreeNodeActive(child) && child.selectable }"
                            ></span>
                            <span class="page-tree-label__text">{{ child.label }}</span>
                            <span
                              v-if="child.children.length"
                              class="page-tree-label__arrow"
                              :class="{ 'is-open': child.isOpen }"
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="9 18 15 12 9 6"></polyline>
                              </svg>
                            </span>
                          </button>

                          <div v-if="child.children.length && child.isOpen" class="page-tree-grandchildren">
                            <button
                              v-for="grandChild in child.children"
                              :key="grandChild.id"
                              type="button"
                              class="page-tree-label page-tree-label--leaf"
                              :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                              @click.stop="handleTreeNodeClick(grandChild)"
                            >
                              <span
                                class="page-tree-label__bullet"
                                :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                              ></span>
                              <span class="page-tree-label__text">{{ grandChild.label }}</span>
                            </button>
                          </div>
                        </template>
                      </div>
                    </div>

                    <div v-if="pageTreeData.length === 0" class="page-tree-empty">
                      {{ t('table.empty') }}
                    </div>
                  </div>
                </el-popover>
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
            {{
              mode === 'role'
                ? `${t('field.roles')} 模板: ${roleTemplateSummary}`
                : `${t('field.tenant')} ${t('field.limit')}: ${tenantLimitSummary}`
            }}
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
import axios from 'axios'
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import { setTokens } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore, type MenuItem } from '@/stores/menu'
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

type RoleColumnResponse = {
  roleId: number
  pageKey: string
  visibleColumns: string[]
  updatedBy?: string | null
  updatedAt?: string | null
}

type PageOption = {
  key: string
  label: string
}

type ExtraPageNode = {
  pageKey: string
  label?: string
}

type PageTreeNode = {
  id: string
  label: string
  icon?: string
  pageKey?: string
  selectable: boolean
  isOpen: boolean
  children: PageTreeNode[]
}

const { t } = useI18n()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const { notifyError, notifySuccess, notifyWarning } = useApiError()

const isSuperAdmin = computed(() => authStore.hasRole('super_admin'))

const mode = ref<ModeType>('role')
const pageKey = ref<string>('')
const pageTreeVisible = ref(false)
const pageTreeOpenState = ref<Record<string, boolean>>({})

const permissions = ref<PermissionItem[]>([])
const roleOptions = ref<RoleOption[]>([])
const tenantOptions = ref<TenantOption[]>([])
const treeMenus = ref<MenuItem[]>([])

const selectedRoleId = ref<number | null>(null)
const selectedRolePermissionIds = ref<number[]>([])
const fullRolePermissionIds = ref<number[]>([])
const updatingRoleSelection = ref(false)
const roleSettingExists = ref(false)
const currentRoleSetting = ref<RoleColumnResponse | null>(null)

const selectedTenantId = ref<number | null>(null)
const tenantVisibleColumns = ref<string[]>([])

const tenantAllowedKeys = ref<string[]>([])
const tenantSettingExists = ref(false)
const hiddenPageKeys = new Set([
  'edit',
  'role',
  'erp-purchase',
  'erp-purchase-return',
  'erp-sale',
  'erp-sale-return',
])

const menuPageKeyMap: Record<string, string[]> = {
  users: ['user-management'],
  roles: ['role-management'],
  permissions: ['permission-management'],
  'audit-logs': ['audit-logs'],
  'column-permissions': [],
  columnPermissions: [],
  'menu-management': ['menu-management'],
  'system-config': ['system-configs'],
  tenants: ['tenant-management'],
  'erp-product': ['erp-product'],
  'erp-vehicle-fitment': ['erp-vehicle-brand', 'erp-vehicle-series', 'erp-vehicle-model', 'erp-product-fitment'],
  'erp-customer': ['erp-customer'],
  'erp-customer-category': ['erp-customer-category'],
  'erp-supplier': ['erp-supplier'],
  'erp-warehouse': ['erp-warehouse'],
  'erp-location': ['erp-location'],
  'erp-category': ['erp-category'],
  'erp-unit': ['erp-unit'],
  'erp-settlement-method': ['erp-settlement-method'],
  'erp-payment-method': ['erp-payment-method'],
  'erp-receipt-method': ['erp-receipt-method'],
  'erp-delivery-method': ['erp-delivery-method'],
  'erp-print-template': ['erp-print-template'],
  'erp-purchase-draft': ['erp-purchase-draft'],
  'erp-purchase-approved': ['erp-purchase-approved'],
  'erp-purchase-return-draft': ['erp-purchase-return-draft'],
  'erp-purchase-return-approved': ['erp-purchase-return-approved'],
  'erp-sale-draft': ['erp-sale-draft'],
  'erp-sale-approved': ['erp-sale-approved'],
  'erp-sale-return-draft': ['erp-sale-return-draft'],
  'erp-sale-return-approved': ['erp-sale-return-approved'],
  'erp-stock': ['erp-stock'],
  'erp-stock-txn': ['erp-stock-txn'],
  'erp-stock-count': ['erp-stock-count'],
  'erp-stock-transfer': ['erp-stock-transfer'],
  'erp-stock-init': ['erp-stock-init'],
  'erp-stock-warning': ['erp-stock-warning'],
  'erp-assemble-order': ['erp-assemble-order'],
  'erp-disassemble-order': ['erp-disassemble-order'],
  'erp-ar': ['erp-ar'],
  'erp-finance-summary': ['erp-finance-customer-debt'],
  'erp-finance-customer-debt': ['erp-finance-customer-debt'],
  'erp-finance-supplier-debt': ['erp-finance-supplier-debt'],
  'erp-ap': ['erp-ap'],
  'erp-receipt': ['erp-receipt'],
  'erp-payment': ['erp-payment'],
}

const menuExtraPageMap: Record<string, ExtraPageNode[]> = {
  'erp-sale-approved': [
    { pageKey: 'erp-sale-form', label: '销售单表单/打印' },
  ],
}

const pageLabelMap = computed<Record<string, string>>(() => ({
  'user-management': t('page.userManagement'),
  'role-management': t('page.roleManagement'),
  'permission-management': t('page.permissionManagement'),
  'menu-management': t('page.menuManagement'),
  'tenant-management': t('page.tenantManagement'),
  'audit-logs': t('page.auditLogManagement'),
  'system-configs': t('page.systemConfigManagement'),
  'erp-product': t('page.erpProductManagement'),
  'erp-customer': t('page.erpCustomerManagement'),
  'erp-customer-category': t('page.erpCustomerCategoryManagement'),
  'erp-vehicle-brand': `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleBrand')}`,
  'erp-vehicle-series': `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleSeries')}`,
  'erp-vehicle-model': `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleModel')}`,
  'erp-product-fitment': `${t('page.erpVehicleFitmentManagement')} - ${t('field.productFitment')}`,
  'erp-supplier': t('page.erpSupplierManagement'),
  'erp-warehouse': t('page.erpWarehouseManagement'),
  'erp-location': t('page.erpLocationManagement'),
  'erp-category': t('page.erpCategoryManagement'),
  'erp-unit': t('page.erpUnitManagement'),
  'erp-settlement-method': t('page.erpSettlementMethodManagement'),
  'erp-payment-method': t('page.erpPaymentMethodManagement'),
  'erp-receipt-method': t('page.erpReceiptMethodManagement'),
  'erp-delivery-method': t('page.erpDeliveryMethodManagement'),
  'erp-purchase-draft': t('page.erpPurchaseOrderDraft'),
  'erp-purchase-approved': t('page.erpPurchaseOrderApproved'),
  'erp-purchase-return-draft': t('nav.erpPurchaseReturnDraft'),
  'erp-purchase-return-approved': t('nav.erpPurchaseReturnApproved'),
  'erp-sale-draft': t('nav.erpSaleDraft'),
  'erp-sale-approved': t('nav.erpSaleApproved'),
  'erp-sale-form': '销售单表单/打印',
  'erp-sale-return-draft': t('nav.erpSaleReturnDraft'),
  'erp-sale-return-approved': t('nav.erpSaleReturnApproved'),
  'erp-ar': t('page.erpAccountsReceivableManagement'),
  'erp-ap': t('page.erpAccountsPayableManagement'),
  'erp-receipt': t('page.erpReceiptManagement'),
  'erp-payment': t('page.erpPaymentManagement'),
  'erp-finance-customer-debt': t('page.erpCustomerDebtManagement'),
  'erp-finance-supplier-debt': t('page.erpSupplierDebtManagement'),
  'erp-print-template': t('page.erpPrintTemplateManagement'),
  'erp-stock': t('page.erpStockManagement'),
  'erp-stock-txn': t('page.erpStockTxnManagement'),
  'erp-stock-warning': t('page.erpStockWarningManagement'),
  'erp-stock-count': t('page.erpStockCountManagement'),
  'erp-stock-init': t('page.erpStockInitManagement'),
  'erp-stock-transfer': t('page.erpStockTransferManagement'),
  'erp-assemble-order': t('page.erpAssemblyOrderManagement'),
  'erp-disassemble-order': t('page.erpDisassembleOrderManagement'),
}))

const pageOptions = computed<PageOption[]>(() => {
  const keys = Array.from(new Set(permissions.value.map((item) => item.pageKey)))
  return keys.map((key) => ({
    key,
    label: pageLabelMap.value[key] || key,
  }))
})

const pageOptionsMap = computed(() => {
  return new Map(pageOptions.value.map((item) => [item.key, item]))
})

const selectedPageLabel = computed(() => {
  if (!pageKey.value) return t('field.page')
  return pageLabelMap.value[pageKey.value] || pageKey.value
})

const containsTreeSelection = (node: PageTreeNode): boolean => {
  if (node.pageKey === pageKey.value) return true
  return node.children.some((child) => containsTreeSelection(child))
}

const resolveTreeNodeOpen = (nodeId: string, defaultOpen: boolean): boolean => {
  const stored = pageTreeOpenState.value[nodeId]
  return stored === undefined ? defaultOpen : stored
}

const buildPageLeafNodes = (parentId: string, label: string, mappedKeys: string[]): PageTreeNode[] => {
  const matched = mappedKeys
    .map((mappedKey) => pageOptionsMap.value.get(mappedKey))
    .filter((item): item is PageOption => Boolean(item))

  if (matched.length === 0) {
    return []
  }

  if (matched.length === 1) {
    const single = matched[0]
    if (!single) {
      return []
    }
    return [{
      id: `${parentId}:${single.key}`,
      label: label || single.label,
      pageKey: single.key,
      selectable: true,
      isOpen: false,
      children: [],
    }]
  }

  return [{
    id: `${parentId}:group`,
    label,
    selectable: false,
    isOpen: resolveTreeNodeOpen(`${parentId}:group`, matched.some((item) => item.key === pageKey.value)),
    children: matched.map((item) => ({
      id: `${parentId}:${item.key}`,
      label: item.label,
      pageKey: item.key,
      selectable: true,
      isOpen: false,
      children: [],
    })),
  }]
}

const buildExtraPageLeafNodes = (parentId: string, menuKey: string): PageTreeNode[] => {
  const extraNodes = menuExtraPageMap[menuKey] || []
  return extraNodes
    .map<PageTreeNode | null>((item) => {
      const option = pageOptionsMap.value.get(item.pageKey)
      if (!option) {
        return null
      }
      return {
        id: `${parentId}:extra:${item.pageKey}`,
        label: item.label || option.label,
        pageKey: item.pageKey,
        selectable: true,
        isOpen: false,
        children: [],
      }
    })
    .filter((item): item is PageTreeNode => Boolean(item))
}

const buildTreeNode = (item: MenuItem, parentId: string): PageTreeNode | null => {
  const menuKey = item.key || ''
  const nodeId = `${parentId}:${menuKey || item.id}`
  const mappedNodes = buildPageLeafNodes(nodeId, item.title || '', menuPageKeyMap[menuKey] || [])
  const extraNodes = buildExtraPageLeafNodes(nodeId, menuKey)
  const childNodes = (item.children || [])
    .map((child) => buildTreeNode(child, nodeId))
    .filter((child): child is PageTreeNode => Boolean(child))

  const firstMappedNode = mappedNodes[0]
  const onlyDirectLeaf = mappedNodes.length === 1
    && Boolean(firstMappedNode?.pageKey)
    && childNodes.length === 0
    && extraNodes.length === 0
  const directPageKey = onlyDirectLeaf ? firstMappedNode?.pageKey : undefined
  const children = onlyDirectLeaf ? [] : [...mappedNodes, ...extraNodes, ...childNodes]

  if (!onlyDirectLeaf && children.length === 0) {
    return null
  }

  const containsCurrent = directPageKey === pageKey.value || children.some((child) => containsTreeSelection(child))
  return {
    id: nodeId,
    label: item.title || item.key || '',
    icon: parentId === 'root' ? item.icon : undefined,
    pageKey: directPageKey,
    selectable: Boolean(directPageKey),
    isOpen: resolveTreeNodeOpen(nodeId, containsCurrent || parentId === 'root'),
    children,
  }
}

const pageTreeData = computed<PageTreeNode[]>(() => {
  const tree = treeMenus.value
    .map((item) => buildTreeNode(item, 'root'))
    .filter((item): item is PageTreeNode => Boolean(item))

  const seen = new Set<string>()
  const walk = (node: PageTreeNode) => {
    if (node.pageKey) seen.add(node.pageKey)
    node.children.forEach(walk)
  }
  tree.forEach(walk)

  const fallbackNodes = pageOptions.value
    .filter((item) => !seen.has(item.key))
    .map((item) => ({
      id: `fallback:${item.key}`,
      label: item.label,
      pageKey: item.key,
      selectable: true,
      isOpen: false,
      children: [],
    }))

  if (fallbackNodes.length > 0) {
    tree.push({
      id: 'root:fallback',
      label: '未映射页面',
      selectable: false,
      isOpen: resolveTreeNodeOpen(
        'root:fallback',
        fallbackNodes.some((item) => item.pageKey === pageKey.value),
      ),
      children: fallbackNodes,
    })
  }

  return tree
})

const isTreeNodeActive = (node: PageTreeNode): boolean => {
  if (node.pageKey && node.pageKey === pageKey.value) return true
  return node.children.some((child) => isTreeNodeActive(child))
}

const handleTreeNodeClick = (node: PageTreeNode) => {
  if (node.selectable && node.pageKey) {
    pageKey.value = node.pageKey
    pageTreeVisible.value = false
    return
  }

  if (node.children.length > 0) {
    pageTreeOpenState.value = {
      ...pageTreeOpenState.value,
      [node.id]: !node.isOpen,
    }
  }
}

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
    return basePagePermissions.value
  }
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

const roleTemplateSummary = computed(() => {
  if (!pageKey.value) return t('table.empty')
  const selectedCount = selectedRolePermissionIds.value.length
  const totalCount = roleEditablePermissions.value.length
  if (!roleSettingExists.value) {
    return `${selectedCount}/${totalCount} (${t('status.default')})`
  }
  return `${selectedCount}/${totalCount}`
})

const currentUserRoleCodes = computed(() => {
  const roles = (authStore.user as { roles?: Array<string | { code?: string }> } | null)?.roles
  if (!Array.isArray(roles)) return [] as string[]
  return roles
    .map((item) => {
      if (typeof item === 'string') return item
      return item?.code || ''
    })
    .filter((item) => Boolean(item))
})

const isEditingCurrentUserRole = computed(() => {
  if (!selectedRoleId.value) return false
  const targetRole = roleOptions.value.find((item) => item.id === selectedRoleId.value)
  if (!targetRole?.code) return false
  return currentUserRoleCodes.value.includes(targetRole.code)
})

const refreshCurrentSession = async () => {
  const res: any = await axios.post('/api/refresh', {}, { withCredentials: true })
  const refreshData = res?.data
  if (!refreshData || refreshData.code !== 200 || !refreshData.data?.token) {
    throw new Error(refreshData?.message || '刷新登录态失败')
  }
  setTokens(refreshData.data.token, refreshData.data.authPayload)
}

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
    permissions.value = data.map((item: any) => {
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
    }).filter((item: PermissionItem) => !hiddenPageKeys.has(item.pageKey))
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

const normalizeTreeMenu = (item: any): MenuItem => {
  return {
    id: Number(item.id),
    key: String(item.code || item.key || ''),
    title: item.title || '',
    path: item.path || '',
    icon: item.icon || '',
    permissionCode: item.permissionCode || null,
    children: Array.isArray(item.children) ? item.children.map((child: any) => normalizeTreeMenu(child)) : [],
  }
}

const loadTreeMenus = async () => {
  try {
    if (isSuperAdmin.value) {
      const res: any = await request.get('/menus/all')
      const data = res.data.data || []
      treeMenus.value = Array.isArray(data) ? data.map((item: any) => normalizeTreeMenu(item)) : []
      return
    }
    await menuStore.fetchMenus()
    treeMenus.value = menuStore.menus
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
    roleSettingExists.value = false
    currentRoleSetting.value = null
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

const applyRoleSettingForCurrentPage = (resp: RoleColumnResponse | null) => {
  currentRoleSetting.value = resp
  const visibleColumns = Array.isArray(resp?.visibleColumns) ? resp.visibleColumns : []
  roleSettingExists.value = Boolean(resp?.updatedAt || resp?.updatedBy)

  const candidates = roleEditablePermissions.value
  if (!roleSettingExists.value) {
    updatingRoleSelection.value = true
    selectedRolePermissionIds.value = candidates.map((item) => item.id)
    updatingRoleSelection.value = false
    return
  }

  const selectedColumns = new Set(visibleColumns)
  updatingRoleSelection.value = true
  selectedRolePermissionIds.value = candidates
    .filter((item) => selectedColumns.has(item.columnKey))
    .map((item) => item.id)
  updatingRoleSelection.value = false
}

const loadRoleSettingForCurrentContext = async () => {
  if (mode.value !== 'role' || !selectedRoleId.value || !pageKey.value) {
    roleSettingExists.value = false
    currentRoleSetting.value = null
    return
  }
  try {
    const res: any = await request.get(`/roles/${selectedRoleId.value}/column-settings/${pageKey.value}`)
    const data: RoleColumnResponse = res.data.data || {
      roleId: selectedRoleId.value,
      pageKey: pageKey.value,
      visibleColumns: [],
    }
    applyRoleSettingForCurrentPage(data)
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
      pageKey: pageKey.value,
      permissionIds: selectedRolePermissionIds.value,
    })
    currentRoleSetting.value = {
      roleId: selectedRoleId.value,
      pageKey: pageKey.value,
      visibleColumns: currentPagePermissions.value
        .filter((item) => selectedRolePermissionIds.value.includes(item.id))
        .map((item) => item.columnKey),
      updatedAt: new Date().toISOString(),
    }
    roleSettingExists.value = true
    if (isEditingCurrentUserRole.value) {
      await refreshCurrentSession()
    }
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

watch([pageKey, mode, selectedRoleId], () => {
  loadRoleSettingForCurrentContext()
})

watch(selectedRolePermissionIds, () => {
  if (mode.value !== 'role') return
  if (updatingRoleSelection.value) return
  mergeCurrentPageToFull()
})

watch(roleEditablePermissions, () => {
  clampRoleSelections()
  if (mode.value === 'role' && selectedRoleId.value && pageKey.value) {
    applyRoleSettingForCurrentPage(currentRoleSetting.value)
  }
})

watch(basePagePermissions, () => {
  clampTenantSelections()
})

onMounted(async () => {
  await loadTreeMenus()
  await loadColumnPermissions()
  await loadRoleOptions()
  await loadTenantOptions()

  const resolveFirstSelectable = (node?: PageTreeNode): string | undefined => {
    if (!node) return undefined
    if (node.pageKey) return node.pageKey
    for (const child of node.children) {
      const matched = resolveFirstSelectable(child)
      if (matched) return matched
    }
    return undefined
  }

  const firstPageKey = resolveFirstSelectable(pageTreeData.value[0]) || pageOptions.value[0]?.key
  if (firstPageKey) {
    pageKey.value = firstPageKey
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

.page-tree-trigger {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.page-tree-trigger:hover,
.page-tree-trigger:focus-visible {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
  outline: none;
}

.page-tree-trigger__text {
  min-width: 0;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-tree-trigger__text.is-placeholder {
  color: #a8abb2;
}

.page-tree-trigger__arrow {
  width: 16px;
  height: 16px;
  color: #909399;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.page-tree-trigger__arrow.is-open {
  transform: rotate(180deg);
}

.page-tree-dropdown {
  max-height: 420px;
  overflow: auto;
  padding: 6px 0;
}

.page-tree-node {
  display: flex;
  flex-direction: column;
}

.page-tree-children,
.page-tree-grandchildren {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-tree-children {
  padding: 4px 0 10px 14px;
}

.page-tree-grandchildren {
  padding: 4px 0 4px 18px;
}

.page-tree-label {
  width: 100%;
  border: 0;
  background: transparent;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.page-tree-label__icon {
  display: flex;
  margin-right: 2px;
  opacity: 0.8;
}

.page-tree-label__text {
  min-width: 0;
  flex: 1 1 auto;
}

.page-tree-label__arrow {
  width: 16px;
  height: 16px;
  color: #909399;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.page-tree-label__arrow.is-open {
  transform: rotate(90deg);
}

.page-tree-label__bullet {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #909399;
  flex: 0 0 auto;
  opacity: 0.5;
}

.page-tree-label__bullet.is-active {
  background: var(--el-color-primary);
  opacity: 1;
}

.page-tree-label--root {
  min-height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

.page-tree-label--child {
  min-height: 34px;
  padding: 7px 10px;
  font-size: 13.5px;
  color: #555;
}

.page-tree-label--child.is-leaf {
  padding-left: 10px;
}

.page-tree-label--leaf {
  min-height: 34px;
  padding: 6px 10px;
  font-size: 13px;
  color: #666;
}

.page-tree-label.is-active {
  color: var(--el-color-primary);
}

.page-tree-label--root.is-active {
  background: rgba(64, 158, 255, 0.14);
}

.page-tree-label--child.is-active,
.page-tree-label--leaf.is-active {
  background: rgba(64, 158, 255, 0.1);
  border-radius: 10px;
}

.page-tree-empty {
  padding: 18px 16px;
  color: #909399;
  font-size: 13px;
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
