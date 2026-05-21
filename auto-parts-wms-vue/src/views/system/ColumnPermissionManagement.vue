<template>
  <div class="page-shell page-shell--system column-permission-page">
    <div class="page-header">
      <div class="page-title">{{ t('page.columnPermissionManagement') }}</div>
    </div>

    <div class="column-permission-panel">
      <div class="column-tree-list">
        <el-input
          v-model="columnTreeSearch"
          class="column-tree-search"
          placeholder="搜索菜单、页面或列名"
          clearable
        />

        <div v-for="item in displayPageTreeData" :key="item.id" class="page-tree-node">
          <button
            type="button"
            class="page-tree-label page-tree-label--root"
            :class="{ 'is-active': isTreeNodeActive(item) }"
            @click.stop="handleTreeNodeClick(item)"
          >
            <span v-if="item.icon" class="page-tree-label__icon" v-html="item.icon"></span>
            <span class="page-tree-label__text">{{ item.label }}</span>
            <span class="page-tree-label__count">
              {{ treeNodeColumnStats[item.id]?.selected || 0 }}/{{ treeNodeColumnStats[item.id]?.total || 0 }}
            </span>
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
                <span class="page-tree-label__count">
                  {{ treeNodeColumnStats[child.id]?.selected || 0 }}/{{ treeNodeColumnStats[child.id]?.total || 0 }}
                </span>
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
                <div v-for="grandChild in child.children" :key="grandChild.id" class="page-tree-node">
                  <button
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
                    <span class="page-tree-label__count">
                      {{ treeNodeColumnStats[grandChild.id]?.selected || 0 }}/{{ treeNodeColumnStats[grandChild.id]?.total || 0 }}
                    </span>
                    <span
                      v-if="grandChild.children.length"
                      class="page-tree-label__arrow"
                      :class="{ 'is-open': grandChild.isOpen }"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="9 18 15 12 9 6"></polyline>
                      </svg>
                    </span>
                  </button>

                  <div v-if="grandChild.children.length && grandChild.isOpen" class="page-tree-greatgrandchildren">
                    <button
                      v-for="greatGrandChild in grandChild.children"
                      :key="greatGrandChild.id"
                      type="button"
                      class="page-tree-label page-tree-label--leaf page-tree-label--nested-leaf"
                      :class="{ 'is-active': isTreeNodeActive(greatGrandChild) }"
                      @click.stop="handleTreeNodeClick(greatGrandChild)"
                    >
                      <span
                        class="page-tree-label__bullet"
                        :class="{ 'is-active': isTreeNodeActive(greatGrandChild) }"
                      ></span>
                      <span class="page-tree-label__text">{{ greatGrandChild.label }}</span>
                      <span class="page-tree-label__count">
                        {{ treeNodeColumnStats[greatGrandChild.id]?.selected || 0 }}/{{ treeNodeColumnStats[greatGrandChild.id]?.total || 0 }}
                      </span>
                    </button>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <div v-if="displayPageTreeData.length === 0" class="page-tree-empty">
          {{ t('table.empty') }}
        </div>
      </div>

      <div class="column-config-panel">
        <div class="column-config-toolbar">
          <div class="column-config-toolbar__left">
            <el-radio-group v-model="mode">
              <el-radio-button value="role">{{ t('field.roles') }}</el-radio-button>
              <el-radio-button v-if="isSuperAdmin" value="tenant">{{ t('field.tenant') }}</el-radio-button>
            </el-radio-group>

            <el-select v-if="mode === 'role'" v-model="selectedRoleId" placeholder="Select Role" class="target-select">
              <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>

            <el-select
              v-if="mode === 'tenant' && isSuperAdmin"
              v-model="selectedTenantId"
              placeholder="Select Tenant"
              class="target-select"
            >
              <el-option
                v-for="tenant in tenantOptions"
                :key="tenant.id"
                :label="tenant.name"
                :value="tenant.id"
              />
            </el-select>
          </div>

          <div class="column-config-toolbar__right">
            <el-tag type="info" effect="plain">{{ selectedPageSummary }}</el-tag>
            <el-tag type="success" effect="plain">
              {{
                mode === 'role'
                  ? `${t('field.roles')} 模板: ${roleTemplateSummary}`
                  : `${t('field.tenant')} ${t('field.limit')}: ${tenantLimitSummary}`
              }}
            </el-tag>
            <el-button size="small" @click="selectCurrentPageColumns">
              {{ t('action.selectAll') }}
            </el-button>
            <el-button size="small" plain @click="clearCurrentPageColumns">
              全部清空
            </el-button>
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

        <div class="column-config-body">
          <div v-if="!pageKey" class="empty-tip">
            {{ t('message.required') }}
          </div>

          <div v-else class="column-config-content">
            <section class="column-config-section column-config-section--config">
              <div class="column-config-section__title">
                {{ mode === 'role' ? t('field.roleColumnConfig') : t('field.tenantColumnConfig') }}
              </div>

              <el-empty
                v-if="currentPagePermissions.length === 0"
                class="column-config-empty"
                :description="t('table.empty')"
              />

              <el-checkbox-group v-else-if="mode === 'role'" v-model="selectedRolePermissionIds" class="column-check-list">
                <div
                  v-for="permission in currentPagePermissions"
                  :key="permission.id"
                  class="column-check-item"
                  spellcheck="false"
                >
                  <el-checkbox :value="permission.id" />
                  <span class="column-check-item__main">
                    <span class="column-check-item__name">{{ permission.name }}</span>
                  </span>
                </div>
              </el-checkbox-group>

              <el-checkbox-group v-else v-model="tenantVisibleColumns" class="column-check-list">
                <div
                  v-for="permission in currentPagePermissions"
                  :key="permission.id"
                  class="column-check-item"
                  spellcheck="false"
                >
                  <el-checkbox :value="permission.columnKey" />
                  <span class="column-check-item__main">
                    <span class="column-check-item__name">{{ permission.name }}</span>
                  </span>
                </div>
              </el-checkbox-group>
            </section>

            <section class="column-config-section column-config-section--preview">
              <div class="column-config-section__title">
                {{ t('field.preview') }}
              </div>

              <div class="preview-table-wrapper">
                <ErpDataTable :data="previewRows" height="100%" stripe table-key="column-permission-management">
                  <ErpDataTableColumn type="index" width="64" :label="t('table.index')" />
                  <ErpDataTableColumn
                    v-for="column in previewColumns"
                    :key="column.key"
                    :prop="column.key"
                    :label="column.label"
                    min-width="140"
                    show-overflow-tooltip />
                </ErpDataTable>
              </div>

              <div v-if="previewColumns.length === 0" class="empty-tip">
                {{ t('table.empty') }}
              </div>
            </section>
          </div>
        </div>
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
const pageTreeOpenState = ref<Record<string, boolean>>({})
const columnTreeSearch = ref('')

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
const rolePageSettingMap = ref<Record<string, RoleColumnResponse | null>>({})
const rolePageSettingLoadSeq = ref(0)

const selectedTenantId = ref<number | null>(null)
const tenantVisibleColumns = ref<string[]>([])
const tenantPageSettingMap = ref<Record<string, TenantColumnResponse | null>>({})
const tenantPageSettingLoadSeq = ref(0)

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
  user: ['user-management'],
  users: ['user-management'],
  role: ['role-management'],
  roles: ['role-management'],
  permission: ['permission-management'],
  permissions: ['permission-management'],
  audit: ['audit-logs'],
  'audit-logs': ['audit-logs'],
  column: [],
  'column-permissions': [],
  columnPermissions: [],
  menu: ['menu-management'],
  'menu-management': ['menu-management'],
  'system-config': ['system-configs'],
  tenant: ['tenant-management'],
  tenants: ['tenant-management'],
  'erp-product': ['erp-product'],
  'erp-product-fitment': ['erp-vehicle-brand', 'erp-vehicle-series', 'erp-vehicle-model', 'erp-product-fitment'],
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
  'erp-finance-customer-debt': ['erp-finance-customer-debt'],
  'erp-finance-summary': ['erp-finance-customer-debt'],
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

const containsTreeSelection = (node: PageTreeNode): boolean => {
  if (node.pageKey === pageKey.value) return true
  return node.children.some((child) => containsTreeSelection(child))
}

const resolveTreeNodeOpen = (nodeId: string, defaultOpen: boolean): boolean => {
  const stored = pageTreeOpenState.value[nodeId]
  return stored === undefined ? defaultOpen : stored
}

const buildPageLeafNodes = (
  parentId: string,
  label: string,
  mappedKeys: string[],
  collapseSingleLeaf = true,
): PageTreeNode[] => {
  const matched = mappedKeys
    .map((mappedKey) => pageOptionsMap.value.get(mappedKey))
    .filter((item): item is PageOption => Boolean(item))

  if (matched.length === 0) {
    return []
  }

  if (matched.length === 1 && collapseSingleLeaf) {
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
  const mappedPageKeys = menuPageKeyMap[menuKey] || []
  const mappedNodes = buildPageLeafNodes(
    nodeId,
    item.title || '',
    mappedPageKeys,
    mappedPageKeys.length <= 1,
  )
  const extraNodes = buildExtraPageLeafNodes(nodeId, menuKey)
  const childNodes = (item.children || [])
    .map((child) => buildTreeNode(child, nodeId))
    .filter((child): child is PageTreeNode => Boolean(child))

  const firstMappedNode = mappedNodes[0]
  const onlyMappedGroup = mappedNodes.length === 1
    && !firstMappedNode?.pageKey
    && childNodes.length === 0
    && extraNodes.length === 0
  const onlyDirectLeaf = mappedNodes.length === 1
    && Boolean(firstMappedNode?.pageKey)
    && childNodes.length === 0
    && extraNodes.length === 0
  const directPageKey = onlyDirectLeaf ? firstMappedNode?.pageKey : undefined
  const children = onlyDirectLeaf
    ? []
    : onlyMappedGroup
      ? firstMappedNode?.children || []
      : [...mappedNodes, ...extraNodes, ...childNodes]

  if (!onlyDirectLeaf && !onlyMappedGroup && children.length === 0) {
    return null
  }

  const containsCurrent = directPageKey === pageKey.value || children.some((child) => containsTreeSelection(child))
  return {
    id: nodeId,
    label: item.title || item.key || '',
    icon: parentId === 'root' ? item.icon : undefined,
    pageKey: directPageKey,
    selectable: Boolean(directPageKey),
    isOpen: resolveTreeNodeOpen(
      nodeId,
      containsCurrent || parentId === 'root' || (onlyMappedGroup && firstMappedNode?.isOpen) || false,
    ),
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

const pageColumnStats = computed<Record<string, { selected: number; total: number }>>(() => {
  return pageOptions.value.reduce<Record<string, { selected: number; total: number }>>((acc, item) => {
    const pageColumns = permissions.value.filter((permission) => permission.pageKey === item.key)
    let selected = 0
    if (mode.value === 'role') {
      const cachedSetting = rolePageSettingMap.value[item.key]
      if (cachedSetting === undefined) {
        const selectedIds = new Set(fullRolePermissionIds.value)
        selected = pageColumns.filter((permission) => selectedIds.has(permission.id)).length
      } else if (!cachedSetting?.updatedAt && !cachedSetting?.updatedBy) {
        selected = pageColumns.length
      } else {
        const visibleColumns = new Set(cachedSetting.visibleColumns || [])
        selected = pageColumns.filter((permission) => visibleColumns.has(permission.columnKey)).length
      }
    } else {
      const cachedSetting = tenantPageSettingMap.value[item.key]
      if (cachedSetting === undefined) {
        if (item.key === pageKey.value) {
          const visibleKeys = new Set(tenantVisibleColumns.value)
          selected = pageColumns.filter((permission) => visibleKeys.has(permission.columnKey)).length
        }
      } else if (!cachedSetting?.updatedAt && !cachedSetting?.updatedBy) {
        selected = pageColumns.length
      } else {
        const visibleKeys = new Set(cachedSetting.visibleColumns || [])
        selected = pageColumns.filter((permission) => visibleKeys.has(permission.columnKey)).length
      }
    }
    acc[item.key] = {
      selected,
      total: pageColumns.length,
    }
    return acc
  }, {})
})

const collectTreeNodePageKeys = (node: PageTreeNode): string[] => {
  const keys = node.pageKey ? [node.pageKey] : []
  node.children.forEach((child) => {
    keys.push(...collectTreeNodePageKeys(child))
  })
  return Array.from(new Set(keys))
}

const treeNodeColumnStats = computed<Record<string, { selected: number; total: number }>>(() => {
  const stats: Record<string, { selected: number; total: number }> = {}
  const walk = (node: PageTreeNode) => {
    const nodeStats = collectTreeNodePageKeys(node).reduce(
      (acc, key) => {
        const current = pageColumnStats.value[key]
        if (!current) return acc
        acc.selected += current.selected
        acc.total += current.total
        return acc
      },
      { selected: 0, total: 0 },
    )
    stats[node.id] = nodeStats
    node.children.forEach(walk)
  }
  pageTreeData.value.forEach(walk)
  return stats
})

const treeNodeMatchesSearch = (node: PageTreeNode, keyword: string) => {
  const normalized = keyword.toLowerCase()
  const pageKeys = collectTreeNodePageKeys(node)
  const matchedColumn = permissions.value.some((permission) => (
    pageKeys.includes(permission.pageKey)
    && `${permission.name} ${permission.columnKey} ${permission.code}`.toLowerCase().includes(normalized)
  ))
  return node.label.toLowerCase().includes(normalized) || matchedColumn
}

const filterTreeBySearch = (nodes: PageTreeNode[], keyword: string): PageTreeNode[] => {
  const normalized = keyword.trim().toLowerCase()
  if (!normalized) return nodes

  return nodes
    .map<PageTreeNode | null>((node) => {
      const children = filterTreeBySearch(node.children, normalized)
      if (!treeNodeMatchesSearch(node, normalized) && children.length === 0) {
        return null
      }
      return {
        ...node,
        isOpen: children.length > 0 ? true : node.isOpen,
        children,
      }
    })
    .filter((node): node is PageTreeNode => Boolean(node))
}

const displayPageTreeData = computed(() => filterTreeBySearch(pageTreeData.value, columnTreeSearch.value))

const isTreeNodeActive = (node: PageTreeNode): boolean => {
  if (node.pageKey && node.pageKey === pageKey.value) return true
  return node.children.some((child) => isTreeNodeActive(child))
}

const handleTreeNodeClick = (node: PageTreeNode) => {
  const nextOpenState = node.children.length > 0 ? !node.isOpen : undefined
  if (node.selectable && node.pageKey) {
    pageKey.value = node.pageKey
    if (nextOpenState !== undefined) {
      pageTreeOpenState.value = {
        ...pageTreeOpenState.value,
        [node.id]: nextOpenState,
      }
    }
    return
  }

  if (nextOpenState !== undefined) {
    pageTreeOpenState.value = {
      ...pageTreeOpenState.value,
      [node.id]: nextOpenState,
    }
  }
}

const currentPageLabel = computed(() => {
  if (!pageKey.value) return '-'
  return pageLabelMap.value[pageKey.value] || pageKey.value
})

const selectedPageSummary = computed(() => {
  if (!pageKey.value) return t('table.empty')
  const stats = pageColumnStats.value[pageKey.value] || { selected: 0, total: 0 }
  return `${currentPageLabel.value}: ${stats.selected}/${stats.total}`
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
    if (selectedRoleId.value && !roleOptions.value.some((role) => role.id === selectedRoleId.value)) {
      selectedRoleId.value = null
      selectedRolePermissionIds.value = []
      fullRolePermissionIds.value = []
      roleSettingExists.value = false
      currentRoleSetting.value = null
    }
    if (!selectedRoleId.value && roleOptions.value.length > 0) {
      const defaultRole = roleOptions.value.find((role) => role.code === 'super_admin') ?? roleOptions.value[0]
      if (defaultRole) {
        selectedRoleId.value = defaultRole.id
      }
    }
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
    rolePageSettingMap.value = {}
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

const loadAllRoleSettingsForStats = async () => {
  if (mode.value !== 'role' || !selectedRoleId.value || pageOptions.value.length === 0) {
    rolePageSettingMap.value = {}
    return
  }
  const seq = rolePageSettingLoadSeq.value + 1
  rolePageSettingLoadSeq.value = seq
  const roleId = selectedRoleId.value
  try {
    const entries = await Promise.all(pageOptions.value.map(async (item) => {
      const res: any = await request.get(`/roles/${roleId}/column-settings/${item.key}`)
      const data: RoleColumnResponse = res.data.data || {
        roleId,
        pageKey: item.key,
        visibleColumns: [],
      }
      return [item.key, data] as const
    }))
    if (rolePageSettingLoadSeq.value !== seq || selectedRoleId.value !== roleId || mode.value !== 'role') {
      return
    }
    rolePageSettingMap.value = Object.fromEntries(entries)
  } catch (error) {
    notifyError(error)
  }
}

const loadAllTenantSettingsForStats = async () => {
  if (
    mode.value !== 'tenant'
    || !isSuperAdmin.value
    || !selectedTenantId.value
    || pageOptions.value.length === 0
  ) {
    tenantPageSettingMap.value = {}
    return
  }
  const seq = tenantPageSettingLoadSeq.value + 1
  tenantPageSettingLoadSeq.value = seq
  const tenantId = selectedTenantId.value
  try {
    const entries = await Promise.all(pageOptions.value.map(async (item) => {
      const res: any = await request.get(`/tenants/${tenantId}/columns/${item.key}`)
      const data: TenantColumnResponse = res.data.data || {
        pageKey: item.key,
        visibleColumns: [],
      }
      return [item.key, data] as const
    }))
    if (
      tenantPageSettingLoadSeq.value !== seq
      || selectedTenantId.value !== tenantId
      || mode.value !== 'tenant'
    ) {
      return
    }
    tenantPageSettingMap.value = Object.fromEntries(entries)
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
    rolePageSettingMap.value = {
      ...rolePageSettingMap.value,
      [pageKey.value]: data,
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
      tenantPageSettingMap.value = {
        ...tenantPageSettingMap.value,
        [pageKey.value]: data,
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
    tenantPageSettingMap.value = {
      ...tenantPageSettingMap.value,
      [pageKey.value]: data,
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
    rolePageSettingMap.value = {
      ...rolePageSettingMap.value,
      [pageKey.value]: currentRoleSetting.value,
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
    tenantPageSettingMap.value = {
      ...tenantPageSettingMap.value,
      [pageKey.value]: {
        pageKey: pageKey.value,
        visibleColumns: [...tenantVisibleColumns.value],
        updatedAt: new Date().toISOString(),
      },
    }
    tenantSettingExists.value = true
    notifySuccess()
  } catch (error) {
    notifyError(error)
  }
}

const selectAllTenantColumns = () => {
  tenantVisibleColumns.value = [...allPageColumnKeys.value]
}

const selectCurrentPageColumns = () => {
  if (mode.value === 'role') {
    selectedRolePermissionIds.value = roleEditablePermissions.value.map((item) => item.id)
    return
  }
  tenantVisibleColumns.value = [...allPageColumnKeys.value]
}

const clearCurrentPageColumns = () => {
  if (mode.value === 'role') {
    selectedRolePermissionIds.value = []
    return
  }
  tenantVisibleColumns.value = []
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
  loadAllRoleSettingsForStats()
})

watch([pageOptions, mode], () => {
  loadAllRoleSettingsForStats()
  loadAllTenantSettingsForStats()
})

watch(selectedTenantId, () => {
  tenantPageSettingMap.value = {}
  loadAllTenantSettingsForStats()
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
.column-permission-page {
  overflow: hidden;
}

.column-permission-page .page-header {
  flex: 0 0 auto;
}

.column-permission-panel {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
  box-sizing: border-box;
}

.column-tree-list {
  min-height: 0;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.column-tree-search {
  margin-bottom: 4px;
  flex: 0 0 auto;
}

.page-tree-node,
.page-tree-children,
.page-tree-grandchildren,
.page-tree-greatgrandchildren {
  display: flex;
  flex-direction: column;
}

.page-tree-children {
  padding: 4px 0 8px 12px;
}

.page-tree-grandchildren {
  padding: 4px 0 4px 16px;
}

.page-tree-greatgrandchildren {
  padding: 4px 0 4px 16px;
}

.page-tree-label {
  width: 100%;
  border: 0;
  background: transparent;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.page-tree-label:hover {
  background: #eef5ff;
}

.page-tree-label__icon {
  display: flex;
  margin-right: 2px;
  opacity: 0.8;
}

.page-tree-label__text {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-tree-label__count {
  flex: 0 0 auto;
  font-size: 12px;
  color: #909399;
}

.page-tree-label__arrow {
  width: 14px;
  height: 14px;
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
  min-height: 40px;
  padding: 0 10px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
}

.page-tree-label--child {
  min-height: 34px;
  padding: 7px 9px;
  border-radius: 9px;
  font-size: 13px;
  color: #555;
}

.page-tree-label--leaf {
  min-height: 32px;
  padding: 6px 9px;
  border-radius: 9px;
  font-size: 12.5px;
  color: #666;
}

.page-tree-label--nested-leaf {
  font-size: 12px;
}

.page-tree-label.is-active {
  color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.14);
}

.page-tree-label.is-active .page-tree-label__count {
  color: var(--el-color-primary);
}

.page-tree-empty {
  padding: 18px 10px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.column-config-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

.column-config-toolbar {
  flex: 0 0 auto;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.column-config-toolbar__left,
.column-config-toolbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.column-config-toolbar__right {
  justify-content: flex-end;
}

.target-select {
  width: 220px;
}

.column-config-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.column-config-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.column-config-section {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.column-config-section--config {
  flex: 0 0 auto;
  max-height: 142px;
}

.column-config-section--preview {
  flex: 1 1 auto;
  min-height: 0;
}

.column-config-section__title {
  flex: 0 0 auto;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 22px;
  margin-bottom: 4px;
}

.column-config-section--preview .column-config-section__title {
  color: #303133;
}

.column-config-empty {
  flex: 0 0 auto;
  min-height: 82px;
  border: 1px solid #eef1f4;
  border-radius: 8px;
}

.column-check-list {
  flex: 0 1 auto;
  max-height: 116px;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  align-content: start;
  gap: 6px;
  padding: 2px 4px 2px 0;
}

.column-check-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.column-check-item__main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.column-check-item__name {
  font-weight: 500;
  color: #303133;
  font-size: 12px;
  text-decoration: none;
}

.column-check-item :deep(.el-checkbox) {
  height: 18px;
}

.preview-table-wrapper {
  flex: 1 1 auto;
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
  .column-permission-panel {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .column-config-toolbar,
  .column-config-toolbar__right {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .column-permission-panel {
    grid-template-columns: 1fr;
  }

  .column-tree-list {
    max-height: 260px;
  }

  .target-select {
    width: 100%;
  }
}
</style>
