<template>
  <div class="page-shell page-shell--system permission-management-page">
    <div class="page-header">
      <div class="page-title">{{ $t('page.permissionManagement') }}</div>
    </div>

    <div class="permission-panel" v-loading="loading">
      <div class="permission-panel__toolbar">
        <div class="permission-panel__tools">
          <el-tag type="info" effect="plain">
            {{ currentPagePermissionSummary }}
          </el-tag>
          <el-tag type="success" effect="plain">
            {{ allPermissionSummary }}
          </el-tag>
          <el-tag :type="warningPermissionCount > 0 ? 'warning' : 'success'" effect="plain">
            风险: {{ warningPermissionCount }}
          </el-tag>
          <el-tag type="info" effect="plain">
            未分配: {{ unassignedPermissionCount }}
          </el-tag>
          <el-tag type="info" effect="plain">
            未挂菜单: {{ menuUnboundPermissionCount }}
          </el-tag>
          <el-select
            v-model="statusFilter"
            :placeholder="$t('field.status')"
            class="permission-status-filter"
          >
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
          <el-switch
            v-model="advancedMaintenance"
            active-text="高级维护"
            inactive-text="诊断视图"
          />
          <el-button v-if="advancedMaintenance" size="small" type="primary" plain v-permission="'permission:edit'" @click="enableAllPermissions">
            全部启用
          </el-button>
          <el-button v-if="advancedMaintenance" size="small" type="danger" plain v-permission="'permission:edit'" @click="disableAllPermissions">
            全部停用
          </el-button>
          <el-button v-if="advancedMaintenance" type="primary" v-permission="'permission:add'" @click="openAddModal">
            {{ $t('action.add') }}
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="advancedMaintenance"
        type="warning"
        show-icon
        :closable="false"
        title="高级维护会影响登录态和接口授权；修改权限编码不会自动同步后端注解、前端路由或菜单配置。"
      />

      <div class="permission-workspace">
        <div class="permission-tree-list">
          <el-input
            v-model="permissionTreeSearch"
            class="permission-tree-search"
            placeholder="搜索菜单、权限名称或编码"
            clearable
          />

          <div v-for="item in displayPermissionTreeData" :key="item.id" class="permission-tree-node">
            <button
              type="button"
              class="permission-tree-label permission-tree-label--root"
              :class="{ 'is-active': isTreeNodeActive(item) }"
              @click.stop="handleTreeNodeClick(item)"
            >
              <el-checkbox
                v-if="advancedMaintenance"
                :model-value="isTreeNodeChecked(item)"
                :indeterminate="isTreeNodeIndeterminate(item)"
                @click.stop
                @change="(checked: boolean) => toggleTreeNodePermissions(item, checked)"
              />
              <span v-if="item.icon" class="permission-tree-label__icon" v-html="item.icon"></span>
              <span class="permission-tree-label__text">{{ item.label }}</span>
              <span class="permission-tree-label__count">
                {{ treeNodePermissionStats[item.id]?.enabled || 0 }}/{{ treeNodePermissionStats[item.id]?.total || 0 }}
              </span>
              <span
                v-if="item.children.length"
                class="permission-tree-label__arrow"
                :class="{ 'is-open': item.isOpen }"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </span>
            </button>

            <div v-if="item.children.length && item.isOpen" class="permission-tree-children">
              <template v-for="child in item.children" :key="child.id">
                <button
                  type="button"
                  class="permission-tree-label permission-tree-label--child"
                  :class="{
                    'is-active': isTreeNodeActive(child),
                    'is-leaf': child.selectable && child.children.length === 0,
                  }"
                  @click.stop="handleTreeNodeClick(child)"
                >
                  <el-checkbox
                    v-if="advancedMaintenance"
                    :model-value="isTreeNodeChecked(child)"
                    :indeterminate="isTreeNodeIndeterminate(child)"
                    @click.stop
                    @change="(checked: boolean) => toggleTreeNodePermissions(child, checked)"
                  />
                  <span
                    v-if="child.selectable && child.children.length === 0"
                    class="permission-tree-label__bullet"
                    :class="{ 'is-active': isTreeNodeActive(child) && child.selectable }"
                  ></span>
                  <span class="permission-tree-label__text">{{ child.label }}</span>
                  <span class="permission-tree-label__count">
                    {{ treeNodePermissionStats[child.id]?.enabled || 0 }}/{{ treeNodePermissionStats[child.id]?.total || 0 }}
                  </span>
                  <span
                    v-if="child.children.length"
                    class="permission-tree-label__arrow"
                    :class="{ 'is-open': child.isOpen }"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="9 18 15 12 9 6"></polyline>
                    </svg>
                  </span>
                </button>

                <div v-if="child.children.length && child.isOpen" class="permission-tree-grandchildren">
                  <button
                    v-for="grandChild in child.children"
                    :key="grandChild.id"
                    type="button"
                    class="permission-tree-label permission-tree-label--leaf"
                    :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                    @click.stop="handleTreeNodeClick(grandChild)"
                  >
                    <el-checkbox
                      v-if="advancedMaintenance"
                      :model-value="isTreeNodeChecked(grandChild)"
                      :indeterminate="isTreeNodeIndeterminate(grandChild)"
                      @click.stop
                      @change="(checked: boolean) => toggleTreeNodePermissions(grandChild, checked)"
                    />
                    <span
                      class="permission-tree-label__bullet"
                      :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                    ></span>
                    <span class="permission-tree-label__text">{{ grandChild.label }}</span>
                    <span class="permission-tree-label__count">
                      {{ treeNodePermissionStats[grandChild.id]?.enabled || 0 }}/{{ treeNodePermissionStats[grandChild.id]?.total || 0 }}
                    </span>
                  </button>
                </div>
              </template>
            </div>
          </div>

          <div v-if="displayPermissionTreeData.length === 0" class="permission-tree-empty">
            未找到匹配的权限目录
          </div>
        </div>

        <div v-if="selectedResourceKey" class="permission-definition-panel">
          <div class="permission-definition-panel__header">
            <div>
              <div class="permission-definition-panel__title">{{ currentPageLabel }}</div>
              <div class="permission-definition-panel__meta">共 {{ filteredPermissions.length }} 条权限</div>
            </div>
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="permission-definition-panel__search"
              clearable
            />
          </div>

          <el-empty v-if="filteredPermissions.length === 0" :description="$t('table.empty')" />

          <div v-else class="permission-definition-list">
            <div
              v-for="permission in filteredPermissions"
              :key="permission.id"
              class="permission-definition-item"
              :class="{ 'permission-definition-item--readonly': !advancedMaintenance }"
            >
              <el-checkbox
                v-if="advancedMaintenance"
                :model-value="permission.enabled"
                v-permission="'permission:edit'"
                @change="(checked: boolean) => togglePermissionEnabled(permission, checked)"
              />
              <div class="permission-definition-item__content">
                <div class="permission-definition-item__main">
                  <span v-if="canShow('name')" class="permission-definition-item__name">{{ permission.name }}</span>
                  <code v-if="canShow('code')" class="code-badge">{{ permission.code }}</code>
                  <el-tag v-if="canShow('status')" :type="permission.enabled ? 'success' : 'danger'" size="small">
                    {{ permission.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                  <el-tag
                    :type="getPermissionDiagnostic(permission.id)?.riskLevel === 'ok' ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ getPermissionDiagnostic(permission.id)?.riskLevel === 'ok' ? '正常' : '需检查' }}
                  </el-tag>
                </div>
                <div v-if="canShow('description')" class="permission-definition-item__description">
                  {{ permission.description || '-' }}
                </div>
                <div class="permission-usage-row">
                  <span>角色 {{ getPermissionDiagnostic(permission.id)?.roleCount ?? 0 }}</span>
                  <span>菜单 {{ getPermissionDiagnostic(permission.id)?.menuCount ?? 0 }}</span>
                </div>
                <div
                  v-if="getPermissionDiagnostic(permission.id)?.warnings?.length"
                  class="permission-warning-row"
                >
                  <el-tag
                    v-for="warning in getPermissionDiagnostic(permission.id)?.warnings || []"
                    :key="warning"
                    type="warning"
                    effect="plain"
                    size="small"
                  >
                    {{ warning }}
                  </el-tag>
                </div>
              </div>
              <div v-if="advancedMaintenance" class="permission-definition-item__actions">
                <el-button link type="primary" size="small" v-permission="'permission:edit'" @click="openEditModal(permission)">
                  {{ $t('action.edit') }}
                </el-button>
                <el-button link type="danger" size="small" v-permission="'permission:delete'" @click="handleDelete(permission)">
                  {{ $t('action.delete') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="permission-definition-panel permission-definition-panel--empty">
          <el-empty :description="$t('table.empty')" />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showModal"
      :title="isEditing ? $t('action.edit') : $t('action.add')"
      width="500px"
      @closed="resetForm"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" placeholder="Ex: View User" />
        </el-form-item>
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" placeholder="Ex: user:view" />
        </el-form-item>
        <el-form-item :label="$t('field.description')">
          <el-input v-model="formData.description" type="textarea" placeholder="Optional description" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'
import { useAuthStore } from '@/stores/auth'
import { useColumnSettings } from '@/composables/useColumnSettings'
import { useMenuStore, type MenuItem } from '@/stores/menu'

interface Permission {
  id: number
  code: string
  name: string
  description: string
  enabled: boolean
}

interface PermissionDiagnostic {
  permissionId: number
  code: string
  roleCount: number
  menuCount: number
  riskLevel: 'ok' | 'warning'
  warnings: string[]
}

interface ResourceOption {
  key: string
  label: string
  resourceKeys?: string[]
}

interface PageTreeNode {
  id: string
  label: string
  icon?: string
  pageKey?: string
  selectable: boolean
  isOpen: boolean
  children: PageTreeNode[]
}

const { t } = useI18n()
const { notifyError, notifySuccess, notifyWarning } = useApiError()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const searchQuery = ref('')
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all')
const showModal = ref(false)
const isEditing = ref(false)
const loading = ref(false)
const advancedMaintenance = ref(false)
const pageTreeOpenState = ref<Record<string, boolean>>({})
const selectedResourceKey = ref('')
const permissionTreeSearch = ref('')

const permissionList = ref<Permission[]>([])
const permissionDiagnostics = ref<PermissionDiagnostic[]>([])
const treeMenus = ref<MenuItem[]>([])
const hiddenResourceKeys = new Set<string>(['erp-purchase', 'erp-sale', 'erp-assembly'])
const forcedResourceKeys = ['permission', 'menu']

const defaultColumns = ['name', 'code', 'description', 'status']
const { isVisible, fetchTenantKeys } = useColumnSettings('permission-management', defaultColumns)
const columnPermissionMap: Record<string, string> = {
  name: 'column:permission-management:name',
  code: 'column:permission-management:code',
  description: 'column:permission-management:description',
  status: 'column:permission-management:status',
}

const formData = reactive<Omit<Permission, 'id'>>({
  name: '',
  code: '',
  description: '',
  enabled: true,
})
const currentId = ref<number | null>(null)

const menuResourceKeyMap: Record<string, string[]> = {
  user: ['user'],
  users: ['user'],
  role: ['role'],
  roles: ['role'],
  permission: ['permission'],
  permissions: ['permission'],
  audit: ['audit'],
  'audit-logs': ['audit'],
  column: ['column'],
  'column-permissions': ['column'],
  menu: ['menu'],
  'menu-management': ['menu'],
  'system-config': ['system-config'],
  'tenant-setting': ['tenant-setting'],
  'api-latency-monitor': ['api-latency-monitor'],
  tenant: ['tenant'],
  tenants: ['tenant'],
  erp: [],
  'erp-basic': [],
  'erp-product': ['erp-product'],
  'erp-product-fitment': ['erp-product-fitment', 'erp-vehicle-brand', 'erp-vehicle-series', 'erp-vehicle-model'],
  'erp-vehicle-fitment': ['erp-product-fitment', 'erp-vehicle-brand', 'erp-vehicle-series', 'erp-vehicle-model'],
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
  'erp-warehouse-module': [],
  'erp-finance': [],
  'erp-assemble-order': ['erp-assemble-order'],
  'erp-disassemble-order': ['erp-disassemble-order'],
  'erp-ar': ['erp-ar'],
  'erp-finance-customer-debt': ['erp-finance-customer-debt'],
  'erp-finance-supplier-debt': ['erp-finance-supplier-debt'],
  'erp-ap': ['erp-ap'],
  'erp-receipt': ['erp-receipt'],
  'erp-payment': ['erp-payment'],
}

const menuExtraResourceMap: Record<string, ResourceOption[]> = {}

const formatGroupLabel = (value: string) => {
  if (!value) return 'Other'
  return value
    .split(/[-_]/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

const normalizeAlias = (value?: string | null) => {
  if (!value) return ''
  return value.trim().toLowerCase()
}

const normalizePermissionResource = (resource: string) => {
  const normalized = normalizeAlias(resource)
  return normalized
}

const extractPermissionResource = (code: string) => {
  if (!code) return ''
  if (code.startsWith('column:')) return 'column'
  return normalizePermissionResource(code.split(':')[0]?.trim().toLowerCase() || '')
}

const resourceLabelMap = computed(() => {
  const labelMap = new Map<string, string>()
  const walk = (items: MenuItem[]) => {
    items.forEach((item) => {
      const label = item.title?.trim() || item.key?.trim() || ''
      const mappedKeys = menuResourceKeyMap[item.key || ''] || []
      if (item.key && mappedKeys.length > 1 && label && !labelMap.has(item.key)) {
        labelMap.set(item.key, label)
      }
      mappedKeys.forEach((resourceKey) => {
        const normalized = normalizeAlias(resourceKey)
        if (normalized && label && !labelMap.has(normalized)) {
          labelMap.set(normalized, label)
        }
      })
      if (item.children?.length) {
        walk(item.children)
      }
    })
  }

  walk(treeMenus.value)
  labelMap.set('column', t('page.columnPermissionManagement'))
  labelMap.set('erp-vehicle-fitment', t('page.erpVehicleFitmentManagement'))
  labelMap.set('erp-vehicle-brand', `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleBrand')}`)
  labelMap.set('erp-vehicle-series', `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleSeries')}`)
  labelMap.set('erp-vehicle-model', `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleModel')}`)
  labelMap.set('erp-product-fitment', `${t('page.erpVehicleFitmentManagement')} - ${t('field.productFitment')}`)
  labelMap.set('erp-finance-customer-debt', t('page.erpCustomerDebtManagement'))
  return labelMap
})

const pageOptions = computed<ResourceOption[]>(() => {
  const keys = Array.from(new Set([
    ...permissionList.value.map((item) => extractPermissionResource(item.code)),
    ...forcedResourceKeys,
  ]))
    .filter((key) => Boolean(key) && !hiddenResourceKeys.has(key))

  const availableKeys = new Set(keys)
  const menuKeys = new Set<string>()
  const collectMenuKeys = (items: MenuItem[]) => {
    items.forEach((item) => {
      if (item.key) {
        menuKeys.add(item.key)
      }
      if (item.children?.length) {
        collectMenuKeys(item.children)
      }
    })
  }
  collectMenuKeys(treeMenus.value)
  const groupKeys = new Set(
    Object.entries(menuResourceKeyMap)
      .filter(([key, resourceKeys]) => {
        return resourceKeys.length > 1
          && menuKeys.has(key)
          && resourceKeys.some((resourceKey) => availableKeys.has(resourceKey))
      })
      .map(([key]) => key),
  )
  const resourceOptions = keys
    .filter((key) => !groupKeys.has(key))
    .map((key) => ({
      key,
      label: resourceLabelMap.value.get(key) || formatGroupLabel(key),
    }))
  const groupOptions = Object.entries(menuResourceKeyMap)
    .filter(([key, resourceKeys]) => {
      return groupKeys.has(key)
    })
    .map(([key, resourceKeys]) => ({
      key,
      label: resourceLabelMap.value.get(key) || formatGroupLabel(key),
      resourceKeys: resourceKeys.filter((resourceKey) => availableKeys.has(resourceKey)),
    }))

  return [...resourceOptions, ...groupOptions]
})

const pageOptionsMap = computed(() => {
  return new Map(pageOptions.value.map((item) => [item.key, item]))
})

const currentPageLabel = computed(() => {
  if (!selectedResourceKey.value) return t('field.page')
  return pageOptionsMap.value.get(selectedResourceKey.value)?.label || selectedResourceKey.value
})

const permissionDiagnosticsMap = computed(() => {
  return new Map(permissionDiagnostics.value.map((item) => [item.permissionId, item]))
})

const getPermissionDiagnostic = (permissionId: number) => {
  return permissionDiagnosticsMap.value.get(permissionId)
}

const warningPermissionCount = computed(() => {
  return permissionDiagnostics.value.filter((item) => item.riskLevel !== 'ok').length
})

const unassignedPermissionCount = computed(() => {
  return permissionDiagnostics.value.filter((item) => item.roleCount === 0).length
})

const menuUnboundPermissionCount = computed(() => {
  return permissionDiagnostics.value.filter((item) => item.menuCount === 0).length
})

const selectedResourceKeys = computed(() => {
  if (!selectedResourceKey.value) return [] as string[]
  const option = pageOptionsMap.value.get(selectedResourceKey.value)
  return option?.resourceKeys?.length ? option.resourceKeys : [selectedResourceKey.value]
})

const containsTreeSelection = (node: PageTreeNode): boolean => {
  if (node.pageKey === selectedResourceKey.value) return true
  return node.children.some((child) => containsTreeSelection(child))
}

const resolveTreeNodeOpen = (nodeId: string, defaultOpen: boolean): boolean => {
  const stored = pageTreeOpenState.value[nodeId]
  return stored === undefined ? defaultOpen : stored
}

const buildResourceLeafNodes = (parentId: string, label: string, resourceKeys: string[]): PageTreeNode[] => {
  const matched = Array.from(new Set(resourceKeys))
    .map((resourceKey) => pageOptionsMap.value.get(resourceKey))
    .filter((item): item is ResourceOption => Boolean(item))

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
    isOpen: resolveTreeNodeOpen(`${parentId}:group`, matched.some((item) => item.key === selectedResourceKey.value)),
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

const buildExtraResourceLeafNodes = (parentId: string, menuKey: string): PageTreeNode[] => {
  const extraNodes = menuExtraResourceMap[menuKey] || []
  return extraNodes
    .map<PageTreeNode | null>((item) => {
      const option = pageOptionsMap.value.get(item.key)
      if (!option) {
        return null
      }
      return {
        id: `${parentId}:extra:${item.key}`,
        label: item.label || option.label,
        pageKey: item.key,
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
  const mappedResourceKeys = menuResourceKeyMap[menuKey] || []
  const mappedNodes = buildResourceLeafNodes(nodeId, item.title || '', mappedResourceKeys)
  const extraNodes = buildExtraResourceLeafNodes(nodeId, menuKey)
  const childNodes = (item.children || [])
    .map((child) => buildTreeNode(child, nodeId))
    .filter((child): child is PageTreeNode => Boolean(child))

  const firstMappedNode = mappedNodes[0]
  const onlyDirectLeaf = mappedNodes.length === 1
    && Boolean(firstMappedNode?.pageKey)
    && childNodes.length === 0
    && extraNodes.length === 0
  const onlyMappedGroup = mappedNodes.length === 1
    && !firstMappedNode?.pageKey
    && firstMappedNode?.label === (item.title || '')
    && mappedResourceKeys.length > 1
    && pageOptionsMap.value.has(menuKey)
  const directPageKey = onlyDirectLeaf ? firstMappedNode?.pageKey : undefined
  const groupPageKey = onlyMappedGroup ? menuKey : directPageKey
  const children = onlyDirectLeaf ? [] : [
    ...(onlyMappedGroup ? (firstMappedNode?.children || []) : mappedNodes),
    ...extraNodes,
    ...childNodes,
  ]

  if (!onlyDirectLeaf && children.length === 0) {
    return null
  }

  const containsCurrent = groupPageKey === selectedResourceKey.value || children.some((child) => containsTreeSelection(child))
  return {
    id: nodeId,
    label: item.title || item.key || '',
    icon: parentId === 'root' ? item.icon : undefined,
    pageKey: groupPageKey,
    selectable: Boolean(groupPageKey),
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
    if (node.pageKey) {
      seen.add(node.pageKey)
      const option = pageOptionsMap.value.get(node.pageKey)
      option?.resourceKeys?.forEach((resourceKey) => seen.add(resourceKey))
    }
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
        fallbackNodes.some((item) => item.pageKey === selectedResourceKey.value),
      ),
      children: fallbackNodes,
    })
  }

  return tree
})

const collectTreeNodeResourceKeys = (node: PageTreeNode): string[] => {
  const option = node.pageKey ? pageOptionsMap.value.get(node.pageKey) : undefined
  const selfKeys = option?.resourceKeys?.length ? option.resourceKeys : node.pageKey ? [node.pageKey] : []
  const childKeys = node.children.flatMap((child) => collectTreeNodeResourceKeys(child))
  return Array.from(new Set([...selfKeys, ...childKeys]))
}

const treeNodeMatchesSearch = (node: PageTreeNode, keyword: string) => {
  const normalized = keyword.toLowerCase()
  const resourceKeys = collectTreeNodeResourceKeys(node)
  const matchedPermission = permissionList.value.some((permission) => {
    return resourceKeys.includes(extractPermissionResource(permission.code))
      && `${permission.name} ${permission.code} ${permission.description || ''}`.toLowerCase().includes(normalized)
  })
  return node.label.toLowerCase().includes(normalized) || matchedPermission
}

const filterTreeBySearch = (nodes: PageTreeNode[], keyword: string): PageTreeNode[] => {
  const normalized = keyword.trim().toLowerCase()
  if (!normalized) {
    return nodes
  }

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

const displayPermissionTreeData = computed(() => {
  return filterTreeBySearch(pageTreeData.value, permissionTreeSearch.value)
})

const isTreeNodeActive = (node: PageTreeNode): boolean => {
  if (node.pageKey && node.pageKey === selectedResourceKey.value) return true
  return node.children.some((child) => isTreeNodeActive(child))
}

const handleTreeNodeClick = (node: PageTreeNode) => {
  const nextOpenState = node.children.length > 0 ? !node.isOpen : undefined
  if (node.selectable && node.pageKey) {
    selectedResourceKey.value = node.pageKey
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

const basePermissions = computed(() => {
  if (!selectedResourceKey.value) return [] as Permission[]
  const keys = new Set(selectedResourceKeys.value)
  return permissionList.value.filter((item) => keys.has(extractPermissionResource(item.code)))
})

const filteredPermissions = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  return basePermissions.value.filter((item) => {
    const statusMatched = statusFilter.value === 'all'
      || (statusFilter.value === 'enabled' && item.enabled)
      || (statusFilter.value === 'disabled' && !item.enabled)

    const keywordMatched = !keyword
      || item.name.toLowerCase().includes(keyword)
      || item.code.toLowerCase().includes(keyword)
      || (item.description || '').toLowerCase().includes(keyword)

    return statusMatched && keywordMatched
  })
})

watch(pageOptions, (options) => {
  if (options.length === 0) {
    selectedResourceKey.value = ''
    return
  }
  if (!options.some((item) => item.key === selectedResourceKey.value)) {
    selectedResourceKey.value = options[0]?.key || ''
  }
}, { immediate: true })

const canShow = (key: string) => {
  const permission = columnPermissionMap[key]
  if (permission && !authStore.hasPermission(permission)) {
    return false
  }
  return isVisible(key)
}

const pagePermissionStats = computed<Record<string, { enabled: number; total: number }>>(() => {
  return pageOptions.value.reduce<Record<string, { enabled: number; total: number }>>((acc, item) => {
    const keys = new Set(item.resourceKeys?.length ? item.resourceKeys : [item.key])
    const permissions = permissionList.value.filter((permission) => keys.has(extractPermissionResource(permission.code)))
    acc[item.key] = {
      enabled: permissions.filter((permission) => permission.enabled).length,
      total: permissions.length,
    }
    return acc
  }, {})
})

const treeNodePermissionStats = computed<Record<string, { enabled: number; total: number }>>(() => {
  const stats: Record<string, { enabled: number; total: number }> = {}
  const walk = (node: PageTreeNode) => {
    const nodeStats = collectTreeNodeResourceKeys(node).reduce(
      (acc, key) => {
        const current = pagePermissionStats.value[key]
        if (!current) {
          return acc
        }
        acc.enabled += current.enabled
        acc.total += current.total
        return acc
      },
      { enabled: 0, total: 0 },
    )
    stats[node.id] = nodeStats
    node.children.forEach(walk)
  }

  pageTreeData.value.forEach(walk)
  return stats
})

const currentPagePermissionSummary = computed(() => {
  if (!selectedResourceKey.value) {
    return t('table.empty')
  }
  const stats = pagePermissionStats.value[selectedResourceKey.value] || { enabled: 0, total: 0 }
  return `${currentPageLabel.value}: ${stats.enabled}/${stats.total}`
})

const allPermissionSummary = computed(() => {
  const enabled = permissionList.value.filter((permission) => permission.enabled).length
  return `全部: ${enabled}/${permissionList.value.length}`
})

const getPermissionsByTreeNode = (node: PageTreeNode) => {
  const keys = collectTreeNodeResourceKeys(node)
  return permissionList.value.filter((permission) => keys.includes(extractPermissionResource(permission.code)))
}

const isTreeNodeChecked = (node: PageTreeNode) => {
  const permissions = getPermissionsByTreeNode(node)
  return permissions.length > 0 && permissions.every((permission) => permission.enabled)
}

const isTreeNodeIndeterminate = (node: PageTreeNode) => {
  const permissions = getPermissionsByTreeNode(node)
  if (permissions.length === 0) {
    return false
  }
  const enabledCount = permissions.filter((permission) => permission.enabled).length
  return enabledCount > 0 && enabledCount < permissions.length
}

const updatePermissionEnabled = async (permission: Permission, enabled: boolean) => {
  await request.put(`/permissions/${permission.id}`, {
    name: permission.name,
    code: permission.code,
    description: permission.description || '',
    enabled,
  })
}

const togglePermissionEnabled = async (permission: Permission, checked: string | number | boolean) => {
  const enabled = Boolean(checked)
  if (permission.enabled === enabled) {
    return
  }
  try {
    await updatePermissionEnabled(permission, enabled)
    permission.enabled = enabled
    await fetchPermissionDiagnostics()
    notifySuccess()
  } catch (error) {
    notifyError(error)
  }
}

const togglePermissionsEnabled = async (permissions: Permission[], enabled: boolean) => {
  const targets = permissions.filter((permission) => permission.enabled !== enabled)
  if (targets.length === 0) {
    return
  }

  loading.value = true
  try {
    await Promise.all(targets.map((permission) => updatePermissionEnabled(permission, enabled)))
    targets.forEach((permission) => {
      permission.enabled = enabled
    })
    await fetchPermissionDiagnostics()
    notifySuccess()
  } catch (error) {
    notifyError(error)
    await fetchPermissions()
  } finally {
    loading.value = false
  }
}

const toggleTreeNodePermissions = (node: PageTreeNode, checked: string | number | boolean) => {
  togglePermissionsEnabled(getPermissionsByTreeNode(node), Boolean(checked))
}

const enableAllPermissions = () => {
  togglePermissionsEnabled(permissionList.value, true)
}

const disableAllPermissions = () => {
  togglePermissionsEnabled(permissionList.value, false)
}

const fetchPermissions = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/permissions')
    if (res.data.code === 200) {
      permissionList.value = res.data.data || []
    }
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

const fetchPermissionDiagnostics = async () => {
  try {
    const res: any = await request.get('/permissions/diagnostics')
    if (res.data.code === 200) {
      permissionDiagnostics.value = res.data.data || []
    }
  } catch (error) {
    notifyError(error)
  }
}

const fetchMenus = async () => {
  try {
    if (authStore.hasRole('super_admin')) {
      const res: any = await request.get('/menus/all')
      const data = res.data.data || []
      treeMenus.value = Array.isArray(data)
        ? data.map((item: any) => normalizeTreeMenu(item))
        : []
      return
    }
    await menuStore.fetchMenus()
    treeMenus.value = menuStore.menus
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

const refreshPage = async () => {
  await Promise.all([fetchPermissions(), fetchPermissionDiagnostics(), fetchMenus(), fetchTenantKeys()])
}

onMounted(() => {
  refreshPage()
})

onActivated(() => {
  refreshPage()
})

const openAddModal = () => {
  isEditing.value = false
  currentId.value = null
  resetForm()
  showModal.value = true
}

const openEditModal = (row: Permission) => {
  isEditing.value = true
  currentId.value = row.id
  formData.name = row.name
  formData.code = row.code
  formData.description = row.description || ''
  formData.enabled = row.enabled
  showModal.value = true
}

const resetForm = () => {
  formData.name = ''
  formData.code = ''
  formData.description = ''
  formData.enabled = true
}

const saveData = async () => {
  if (!formData.name || !formData.code) {
    notifyWarning(t('message.required'))
    return
  }

  try {
    const url = isEditing.value && currentId.value
      ? `/permissions/${currentId.value}`
      : '/permissions'

    const method = isEditing.value ? request.put : request.post
    const res: any = await method(url, formData)

    if (res.data.code === 200) {
      notifySuccess()
      showModal.value = false
      await Promise.all([fetchPermissions(), fetchPermissionDiagnostics()])
    }
  } catch (error) {
    notifyError(error)
  }
}

const handleDelete = (row: Permission) => {
  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} "${row.name}"?`,
    'Warning',
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' },
  ).then(async () => {
    try {
      const res: any = await request.delete(`/permissions/${row.id}`, {
        data: {
          reason: `Delete permission ${row.code}`,
        },
      })
      if (res.data.code === 200) {
        notifySuccess()
        await Promise.all([fetchPermissions(), fetchPermissionDiagnostics()])
      }
    } catch (error) {
      notifyError(error)
    }
  })
}
</script>

<style>
.permission-management-page {
  overflow: hidden;
}

.permission-management-page .page-header {
  flex: 0 0 auto;
}

.permission-panel {
  width: 100%;
  min-height: 0;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1 1 auto;
  box-sizing: border-box;
  overflow: hidden;
}

.permission-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 0 0 auto;
}

.permission-panel__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-status-filter {
  width: 120px;
}

.permission-workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
}

.permission-tree-list {
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

.permission-tree-search {
  margin-bottom: 4px;
}

.permission-tree-empty {
  padding: 18px 10px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.permission-tree-node,
.permission-tree-children,
.permission-tree-grandchildren {
  display: flex;
  flex-direction: column;
}

.permission-tree-children {
  padding: 4px 0 8px 12px;
}

.permission-tree-grandchildren {
  padding: 4px 0 4px 16px;
}

.permission-tree-label {
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

:deep(.permission-tree-label .el-checkbox) {
  height: 16px;
  margin-right: 0;
}

.permission-tree-label:hover {
  background: #eef5ff;
}

.permission-tree-label__icon {
  display: flex;
  margin-right: 2px;
  opacity: 0.8;
}

.permission-tree-label__text {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-tree-label__count {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}

.permission-tree-label__arrow {
  width: 14px;
  height: 14px;
  color: #909399;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.permission-tree-label__arrow.is-open {
  transform: rotate(90deg);
}

.permission-tree-label__bullet {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #909399;
  flex: 0 0 auto;
  opacity: 0.5;
}

.permission-tree-label__bullet.is-active {
  background: var(--el-color-primary);
  opacity: 1;
}

.permission-tree-label--root {
  min-height: 40px;
  padding: 0 10px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
}

.permission-tree-label--child {
  min-height: 34px;
  padding: 7px 9px;
  border-radius: 9px;
  font-size: 13px;
  color: #555;
}

.permission-tree-label--leaf {
  min-height: 32px;
  padding: 6px 9px;
  border-radius: 9px;
  font-size: 12.5px;
  color: #666;
}

.permission-tree-label.is-active {
  color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.14);
}

.permission-tree-label.is-active .permission-tree-label__count {
  color: var(--el-color-primary);
}

.permission-definition-panel {
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.permission-definition-panel--empty {
  border-style: dashed;
  display: flex;
  align-items: center;
  justify-content: center;
}

.permission-definition-panel__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 12px;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.permission-definition-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.permission-definition-panel__meta {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.permission-definition-panel__search {
  width: 100%;
}

.permission-definition-list {
  display: flex;
  flex-direction: column;
}

.permission-definition-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 66px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
}

.permission-definition-item--readonly {
  grid-template-columns: minmax(0, 1fr);
}

.permission-definition-item:last-child {
  border-bottom: 0;
}

.permission-definition-item__content {
  min-width: 0;
}

.permission-definition-item__main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-definition-item__name {
  font-weight: 500;
  color: #303133;
}

.permission-definition-item__description {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.permission-usage-row,
.permission-warning-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.permission-usage-row {
  color: #606266;
  font-size: 12px;
}

.permission-definition-item__actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.code-badge {
  display: inline-flex;
  max-width: 100%;
  background: #f5f5f7;
  padding: 3px 8px;
  border-radius: 999px;
  font-family: monospace;
  font-size: 12px;
  color: #be185d;
}

@media (max-width: 1280px) {
  .permission-panel__toolbar {
    justify-content: flex-start;
  }

  .permission-panel__tools {
    justify-content: flex-start;
  }

  .permission-workspace {
    grid-template-columns: 220px minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .permission-workspace {
    grid-template-columns: 1fr;
  }

  .permission-tree-list {
    max-height: 260px;
  }

  .permission-definition-panel__header {
    grid-template-columns: 1fr;
  }

  .permission-definition-item {
    grid-template-columns: 24px minmax(0, 1fr);
  }

  .permission-definition-item--readonly {
    grid-template-columns: minmax(0, 1fr);
  }

  .permission-definition-item__actions {
    grid-column: 2;
    justify-content: flex-start;
  }
}
</style>
