<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.permissionManagement') }}</div>

      <div class="permission-toolbar">
        <div class="table-toolbar">
          <div class="table-filters permission-toolbar__filters">
            <el-form label-width="72px" class="permission-filter-form">
              <el-form-item :label="$t('field.page')">
                <el-popover
                  placement="bottom-start"
                  :width="360"
                  trigger="click"
                  popper-class="page-tree-popper"
                  v-model:visible="pageTreeVisible"
                >
                  <template #reference>
                    <button type="button" class="page-tree-trigger top-select">
                      <span :class="['page-tree-trigger__text', { 'is-placeholder': !selectedResourceKey }]">
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
                      {{ $t('table.empty') }}
                    </div>
                  </div>
                </el-popover>
              </el-form-item>

              <el-form-item :label="$t('action.search')">
                <el-input
                  v-model="searchQuery"
                  :placeholder="$t('action.search')"
                  class="table-search permission-toolbar__search--wide"
                  clearable
                />
              </el-form-item>

              <el-form-item :label="$t('field.status')">
                <el-select
                  v-model="statusFilter"
                  :placeholder="$t('field.status')"
                  class="table-search permission-toolbar__search--narrow"
                >
                  <el-option :label="$t('filter.all')" value="all" />
                  <el-option :label="$t('status.active')" value="enabled" />
                  <el-option :label="$t('status.inactive')" value="disabled" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>

          <div class="table-actions">
            <el-button type="primary" v-permission="'permission:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="main-headers">
      <div class="main-header">{{ currentPageLabel }}</div>
      <div class="main-header main-header--meta">共 {{ filteredPermissions.length }} 条权限</div>
    </div>

    <div class="table-card">
      <div v-if="!selectedResourceKey" class="empty-tip">
        {{ $t('message.required') }}
      </div>

      <div v-else class="table-body">
        <ErpDataTable
          v-loading="loading"
          :data="filteredPermissions"
          style="width: 100%"
          height="100%"
          :empty-text="$t('table.empty')"
         table-key="permission-management">
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="220" />
          <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="220">
            <template #default="{ row }">
              <code class="code-badge">{{ row.code }}</code>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn
            v-if="canShow('description')"
            prop="description"
            :label="$t('field.description')"
            min-width="240"
            show-overflow-tooltip />
          <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" align="center" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'permission:edit'" @click="openEditModal(row)">
                {{ $t('action.edit') }}
              </el-button>
              <el-button link type="danger" size="small" v-permission="'permission:delete'" @click="handleDelete(row)">
                {{ $t('action.delete') }}
              </el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
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

interface ResourceOption {
  key: string
  label: string
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
const pageTreeVisible = ref(false)
const pageTreeOpenState = ref<Record<string, boolean>>({})
const selectedResourceKey = ref('')

const permissionList = ref<Permission[]>([])
const treeMenus = ref<MenuItem[]>([])
const hiddenResourceKeys = new Set<string>()
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
  users: ['user'],
  roles: ['role'],
  permissions: ['permission'],
  'audit-logs': ['audit'],
  'column-permissions': ['column'],
  'menu-management': ['menu'],
  'system-config': ['system-config'],
  tenants: ['tenant'],
  erp: [],
  'erp-basic': [],
  'erp-product': ['erp-product'],
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
  'erp-assemble-order': ['erp-assembly'],
  'erp-disassemble-order': ['erp-assembly'],
  'erp-ar': ['erp-ar'],
  'erp-finance-summary': ['erp-finance-customer-debt'],
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
  const legacyMap: Record<string, string> = {
    'erp-finance-summary': 'erp-finance-customer-debt',
  }
  return legacyMap[normalized] || normalized
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
  labelMap.set('erp-finance-customer-debt', t('page.erpCustomerDebtManagement'))
  return labelMap
})

const pageOptions = computed<ResourceOption[]>(() => {
  const keys = Array.from(new Set([
    ...permissionList.value.map((item) => extractPermissionResource(item.code)),
    ...forcedResourceKeys,
  ]))
    .filter((key) => Boolean(key) && !hiddenResourceKeys.has(key))

  return keys.map((key) => ({
    key,
    label: resourceLabelMap.value.get(key) || formatGroupLabel(key),
  }))
})

const pageOptionsMap = computed(() => {
  return new Map(pageOptions.value.map((item) => [item.key, item]))
})

const selectedPageLabel = computed(() => {
  if (!selectedResourceKey.value) return t('field.page')
  return pageOptionsMap.value.get(selectedResourceKey.value)?.label || selectedResourceKey.value
})

const currentPageLabel = computed(() => {
  if (!selectedResourceKey.value) return t('field.page')
  return pageOptionsMap.value.get(selectedResourceKey.value)?.label || selectedResourceKey.value
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
  const mappedNodes = buildResourceLeafNodes(nodeId, item.title || '', menuResourceKeyMap[menuKey] || [])
  const extraNodes = buildExtraResourceLeafNodes(nodeId, menuKey)
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

  const containsCurrent = directPageKey === selectedResourceKey.value || children.some((child) => containsTreeSelection(child))
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
        fallbackNodes.some((item) => item.pageKey === selectedResourceKey.value),
      ),
      children: fallbackNodes,
    })
  }

  return tree
})

const isTreeNodeActive = (node: PageTreeNode): boolean => {
  if (node.pageKey && node.pageKey === selectedResourceKey.value) return true
  return node.children.some((child) => isTreeNodeActive(child))
}

const handleTreeNodeClick = (node: PageTreeNode) => {
  if (node.selectable && node.pageKey) {
    selectedResourceKey.value = node.pageKey
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

const basePermissions = computed(() => {
  if (!selectedResourceKey.value) return [] as Permission[]
  return permissionList.value.filter((item) => extractPermissionResource(item.code) === selectedResourceKey.value)
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
  await Promise.all([fetchPermissions(), fetchMenus(), fetchTenantKeys()])
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
      await fetchPermissions()
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
        await fetchPermissions()
      }
    } catch (error) {
      notifyError(error)
    }
  })
}
</script>

<style>
.permission-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-sizing: border-box;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.permission-toolbar__filters {
  min-width: 0;
}

.permission-filter-form {
  display: grid;
  grid-template-columns: minmax(240px, 360px) minmax(220px, 1fr) 160px;
  gap: 12px;
  align-items: start;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: flex-end;
  margin-left: 0;
}

.top-select {
  width: 100%;
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
  outline: none;
  appearance: none;
  -webkit-appearance: none;
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
  justify-content: flex-start;
}

.page-tree-label--child {
  min-height: 34px;
  padding: 7px 10px;
  font-size: 13.5px;
  color: #555;
  justify-content: flex-start;
}

.page-tree-label--child.is-leaf {
  padding-left: 10px;
}

.page-tree-label--leaf {
  min-height: 34px;
  padding: 6px 10px;
  font-size: 13px;
  color: #666;
  justify-content: flex-start;
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

.permission-filter-form :deep(.el-form-item__label) {
  color: #606266;
}

.permission-filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.main-headers {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
  margin-top: 16px;
}

.main-header {
  font-size: 16px;
  font-weight: 600;
}

.main-header--meta {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
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
  .permission-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .permission-filter-form {
    grid-template-columns: 1fr;
  }

  .table-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .main-headers {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
