<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.tenantManagement') }}</div>
      <div class="tenant-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="table-search tenant-toolbar__search"
              clearable
            />
          </div>
          <div class="table-actions">
            <el-button v-if="canAdd" type="primary" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="filteredTenants" style="width: 100%" height="100%" v-loading="loading" :empty-text="$t('table.empty')" table-key="tenant-management">
        <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.tenantCode')" min-width="160" />
        <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.tenantName')" min-width="200" />
        <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="120">
          <template #default="{ row }">
            <div class="status-tags">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
              <el-tag v-if="row.code === authStore.tenantCode" type="info" size="small">
                {{ $t('status.current') }}
              </el-tag>
            </div>
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.createdAt) }}</span>
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn :label="$t('table.actions')" width="340" fixed="right" column-key="actions">
          <template #default="{ row }">
            <el-button
              v-if="canSwitch"
              link
              type="primary"
              size="small"
              :disabled="!row.enabled || row.code === authStore.tenantCode"
              @click="handleSwitch(row)"
            >
              {{ $t('action.switchTenant') }}
            </el-button>
            <el-button link type="primary" size="small" @click="openMenuModal(row)">
              {{ $t('menu.settings') }}
            </el-button>
            <el-button v-if="canEdit" link type="primary" size="small" @click="openEditModal(row)">
              {{ $t('action.edit') }}
            </el-button>
            <el-button
              v-if="canDisable"
              link
              :type="row.enabled ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.enabled ? $t('action.disable') : $t('action.enable') }}
            </el-button>
            <el-button
              v-if="canDelete"
              link
              type="danger"
              size="small"
              :disabled="row.code === 'default'"
              @click="handleDelete(row)"
            >
              {{ $t('action.delete') }}
            </el-button>
          </template>
        </ErpDataTableColumn>
        </ErpDataTable>
      </div>
    </div>

    <el-dialog
      v-model="showModal"
      :title="isEditing ? $t('action.edit') : $t('action.createTenant')"
      width="520px"
      @closed="resetForm"
    >
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('field.tenantCode')" required>
          <el-input v-model="formData.code" placeholder="tenant-a" :disabled="isEditing" />
        </el-form-item>
        <el-form-item :label="$t('field.tenantName')" required>
          <el-input v-model="formData.name" placeholder="Tenant A" />
        </el-form-item>
        <template v-if="!isEditing">
          <el-form-item :label="$t('field.adminUsername')">
            <el-input v-model="formData.adminUsername" placeholder="admin" />
          </el-form-item>
          <el-form-item :label="$t('field.adminPassword')">
            <el-input v-model="formData.adminPassword" type="password" placeholder="password" show-password />
          </el-form-item>
          <el-form-item :label="$t('menu.select')">
            <div class="menu-tree-wrapper">
              <el-tree
                ref="createMenuTreeRef"
                :data="createMenuTreeData"
                show-checkbox
                check-strictly
                node-key="id"
                :props="{ label: 'label', children: 'children' }"
                default-expand-all
              />
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveTenant">{{ $t('action.save') }}</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showMenuModal"
      :title="$t('menu.settings')"
      width="1080px"
      class="tenant-menu-dialog"
      @closed="resetMenuForm"
    >
      <div class="tenant-menu-layout">
        <aside class="tenant-menu-sidebar">
          <el-input
            v-model="menuSearchQuery"
            class="tenant-menu-search"
            placeholder="搜索菜单"
            clearable
          />
          <div class="tenant-menu-tree">
            <template v-for="node in displayMenuGroups" :key="node.id">
              <button
                type="button"
                class="tenant-menu-node tenant-menu-node--root"
                :class="{ 'is-active': selectedMenuGroupId === node.id }"
                @click="toggleMenuGroupExpanded(node)"
              >
                <span class="tenant-menu-node__arrow">
                  {{ node.children.length ? (isMenuGroupExpanded(node.id) ? '⌄' : '›') : '' }}
                </span>
                <span class="tenant-menu-node__label">{{ node.label }}</span>
                <span class="tenant-menu-node__count">{{ menuGroupStats[node.id]?.selected || 0 }}/{{ menuGroupStats[node.id]?.total || 0 }}</span>
              </button>

              <button
                v-if="isMenuGroupExpanded(node.id) || menuSearchQuery.trim()"
                v-for="child in node.children"
                :key="child.id"
                type="button"
                class="tenant-menu-node tenant-menu-node--child"
                :class="{ 'is-active': selectedMenuGroupId === child.id }"
                @click="selectMenuGroup(child)"
              >
                <span class="tenant-menu-node__dot"></span>
                <span class="tenant-menu-node__label">{{ child.label }}</span>
                <span class="tenant-menu-node__count">{{ menuGroupStats[child.id]?.selected || 0 }}/{{ menuGroupStats[child.id]?.total || 0 }}</span>
              </button>
            </template>
          </div>
        </aside>

        <section class="tenant-menu-editor">
          <div class="tenant-menu-toolbar">
            <div class="tenant-menu-current">
              <strong>{{ selectedMenuGroup?.label || $t('menu.select') }}</strong>
              <span v-if="selectedMenuGroup">
                当前分组 {{ selectedGroupStats.selected }}/{{ selectedGroupStats.total }}
              </span>
            </div>
            <div class="tenant-menu-actions">
              <el-tag type="success" effect="plain">
                租户菜单 {{ selectedMenuCount }}/{{ totalMenuCount }}
              </el-tag>
              <el-button size="small" @click="selectSelectedMenuGroupMenus">
                {{ $t('action.selectAll') }}
              </el-button>
              <el-button size="small" plain @click="clearSelectedMenuGroupMenus">
                全部清空
              </el-button>
              <el-button size="small" type="primary" plain @click="resetDefaultMenus">
                {{ $t('action.resetDefault') }}
              </el-button>
            </div>
          </div>

          <div class="tenant-menu-content">
            <el-empty v-if="!selectedMenuGroup" :description="$t('table.empty')" />
            <div v-else>
              <section
                v-for="section in selectedMenuSections"
                :key="section.id"
                class="tenant-menu-section"
              >
                <div class="tenant-menu-section__title">
                  <span>{{ section.label }}</span>
                  <span>{{ menuGroupStats[section.id]?.selected || 0 }}/{{ menuGroupStats[section.id]?.total || 0 }}</span>
                </div>
                <div class="tenant-menu-grid">
                  <label
                    v-for="item in section.items"
                    :key="item.id"
                    class="tenant-menu-check"
                  >
                    <el-checkbox
                      :model-value="isMenuChecked(item.id)"
                      @change="handleMenuCheckChange(item, $event)"
                    />
                    <span>{{ item.label }}</span>
                  </label>
                </div>
              </section>
            </div>
          </div>
        </section>
      </div>
      <template #footer>
        <div class="tenant-menu-footer">
          <span>已选菜单会同时保存必要的父级菜单</span>
          <span class="dialog-footer">
            <el-button @click="showMenuModal = false">{{ $t('action.cancel') }}</el-button>
            <el-button type="primary" @click="saveMenuConfig">{{ $t('action.save') }}</el-button>
          </span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onActivated, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, type ElTree } from 'element-plus';
import { useI18n } from 'vue-i18n';
import request, { setTokens } from '@/utils/request';
import { useAuthStore } from '@/stores/auth';
import { useApiError } from '@/composables/useApiError';
import { normalizeMenuKey } from '@/utils/i18n';
import { useColumnSettings } from '@/composables/useColumnSettings';

interface Tenant {
  id: number;
  code: string;
  name: string;
  enabled: boolean;
  createdAt?: string;
}

interface MenuTreeNode {
  id: number;
  key?: string;
  path?: string;
  label: string;
  children: MenuTreeNode[];
}

interface MenuSection {
  id: number;
  label: string;
  items: MenuTreeNode[];
}

const { t } = useI18n();
const router = useRouter();
const authStore = useAuthStore();
const searchQuery = ref('');
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const tenants = ref<Tenant[]>([]);
const loading = ref(false);
const showMenuModal = ref(false);
const menuTreeData = ref<MenuTreeNode[]>([]);
const selectedMenuIds = ref<number[]>([]);
const selectedMenuGroupId = ref<number | null>(null);
const expandedMenuGroupIds = ref<number[]>([]);
const menuSearchQuery = ref('');
const createMenuTreeRef = ref<InstanceType<typeof ElTree>>();
const createMenuTreeData = ref<any[]>([]);
const currentTenantId = ref<number | null>(null);
const canAdd = computed(() => authStore.hasPermission('tenant:add'));
const canEdit = computed(() => authStore.hasPermission('tenant:edit'));
const canDisable = computed(() => authStore.hasPermission('tenant:disable'));
const canDelete = computed(() => authStore.hasPermission('tenant:delete'));
const canSwitch = computed(() => authStore.hasRole('super_admin') && authStore.hasPermission('tenant:switch'));
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const defaultColumns = ['code', 'name', 'status', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('tenant-management', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  code: 'column:tenant-management:code',
  name: 'column:tenant-management:name',
  status: 'column:tenant-management:status',
  createdAt: 'column:tenant-management:createdAt'
};


const formData = reactive({
  code: '',
  name: '',
  adminUsername: '',
  adminPassword: ''
});

const filteredTenants = computed(() => {
  if (!searchQuery.value) return tenants.value;
  const q = searchQuery.value.toLowerCase();
  return tenants.value.filter((item) => {
    return item.code.toLowerCase().includes(q) || item.name.toLowerCase().includes(q);
  });
});

const flattenMenuIds = (nodes: MenuTreeNode[], ids: number[] = []) => {
  nodes.forEach((node) => {
    ids.push(node.id);
    if (node.children.length) {
      flattenMenuIds(node.children, ids);
    }
  });
  return ids;
};

const findMenuNode = (nodes: MenuTreeNode[], id: number | null): MenuTreeNode | null => {
  if (!id) return null;
  for (const node of nodes) {
    if (node.id === id) return node;
    const matched = findMenuNode(node.children, id);
    if (matched) return matched;
  }
  return null;
};

const menuNodeMatchesSearch = (node: MenuTreeNode, keyword: string): boolean => {
  const normalized = keyword.toLowerCase();
  return node.label.toLowerCase().includes(normalized)
    || node.children.some((child) => menuNodeMatchesSearch(child, normalized));
};

const isMenuGroupExpanded = (id: number) => expandedMenuGroupIds.value.includes(id);

const displayMenuGroups = computed(() => {
  const keyword = menuSearchQuery.value.trim();
  if (!keyword) return menuTreeData.value;
  return menuTreeData.value
    .map((node) => {
      const children = node.children.filter((child) => menuNodeMatchesSearch(child, keyword));
      if (!menuNodeMatchesSearch(node, keyword) && children.length === 0) return null;
      return {
        ...node,
        children: children.length > 0 ? children : node.children,
      };
    })
    .filter((node): node is MenuTreeNode => Boolean(node));
});

const totalMenuCount = computed(() => flattenMenuIds(menuTreeData.value, []).length);

const selectedMenuCount = computed(() => {
  const selected = new Set(selectedMenuIds.value);
  return flattenMenuIds(menuTreeData.value, []).filter((id) => selected.has(id)).length;
});

const menuGroupStats = computed<Record<number, { selected: number; total: number }>>(() => {
  const selected = new Set(selectedMenuIds.value);
  const stats: Record<number, { selected: number; total: number }> = {};

  const walk = (node: MenuTreeNode) => {
    const ids = flattenMenuIds([node], []);
    stats[node.id] = {
      selected: ids.filter((id) => selected.has(id)).length,
      total: ids.length,
    };
    node.children.forEach(walk);
  };

  menuTreeData.value.forEach(walk);
  return stats;
});

const selectedMenuGroup = computed(() => (
  findMenuNode(menuTreeData.value, selectedMenuGroupId.value) || menuTreeData.value[0] || null
));

const selectedGroupStats = computed(() => {
  if (!selectedMenuGroup.value) return { selected: 0, total: 0 };
  return menuGroupStats.value[selectedMenuGroup.value.id] || { selected: 0, total: 0 };
});

const selectedMenuSections = computed<MenuSection[]>(() => {
  const group = selectedMenuGroup.value;
  if (!group) return [];
  if (group.children.length === 0) {
    return [{ id: group.id, label: group.label, items: [group] }];
  }
  return group.children.map((child) => ({
    id: child.id,
    label: child.label,
    items: child.children.length > 0 ? child.children : [child],
  }));
});

const fetchTenants = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/tenants');
    if (res.data.code === 200) {
      tenants.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchTenants();
  fetchTenantKeys();
});
onActivated(() => {
  fetchTenants();
});

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
  fetchCreateMenus();
};

const openEditModal = (row: Tenant) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.adminUsername = '';
  formData.adminPassword = '';
  showModal.value = true;
};

const openMenuModal = async (row: Tenant) => {
  currentTenantId.value = row.id;
  showMenuModal.value = true;
  await fetchTenantMenus(row.id);
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.adminUsername = '';
  formData.adminPassword = '';
  createMenuTreeData.value = [];
};

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const resetMenuForm = () => {
  menuTreeData.value = [];
  selectedMenuIds.value = [];
  selectedMenuGroupId.value = null;
  expandedMenuGroupIds.value = [];
  menuSearchQuery.value = '';
  currentTenantId.value = null;
};

const saveTenant = async () => {
  if (!formData.name || (!isEditing.value && !formData.code)) {
    notifyWarning(t('message.required'));
    return;
  }

  try {
    if (isEditing.value && currentId.value) {
      const res: any = await request.put(`/tenants/${currentId.value}`, {
        name: formData.name
      });
      if (res.data.code === 200) {
        notifySuccess();
        showModal.value = false;
        fetchTenants();
      }
      return;
    }

    const menuIds = collectCheckedMenuIds(createMenuTreeRef.value);
    const res: any = await request.post('/tenants', {
      code: formData.code,
      name: formData.name,
      adminUsername: formData.adminUsername || undefined,
      adminPassword: formData.adminPassword || undefined,
      menuIds: menuIds.length ? menuIds : undefined
    });
    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchTenants();
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchTenantMenus = async (tenantId: number) => {
  try {
    const res: any = await request.get(`/tenants/${tenantId}/menus`);
    if (res.data.code === 200) {
      menuTreeData.value = buildMenuTree(res.data.data || []);
      selectedMenuIds.value = collectEnabledIds(res.data.data || []);
      selectedMenuGroupId.value = menuTreeData.value[0]?.id || null;
      expandedMenuGroupIds.value = menuTreeData.value.map((node) => node.id);
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchCreateMenus = async () => {
  try {
    const res: any = await request.get('/menus');
    if (res.data.code === 200) {
      createMenuTreeData.value = buildMenuTree(res.data.data || []);
      const allIds = collectAllMenuIds(createMenuTreeData.value, []);
      nextTick(() => {
        createMenuTreeRef.value?.setCheckedKeys(allIds, false);
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const isMenuNodeVisibleInTenantConfig = (node: { key?: string; path?: string }) => (
  node.key !== 'dashboard' && node.path !== '/'
);

const buildMenuTree = (nodes: any[]): MenuTreeNode[] => {
  return nodes
    .filter((node) => isMenuNodeVisibleInTenantConfig(node))
    .map((node) => ({
      id: node.id,
      key: node.key,
      path: node.path,
      label: labelFromMenu(node),
      children: node.children ? buildMenuTree(node.children) : []
    }));
};

const collectEnabledIds = (nodes: any[], ids: number[] = []) => {
  nodes.forEach((node) => {
    if (node.enabled) {
      ids.push(node.id);
    }
    if (node.children && node.children.length) {
      collectEnabledIds(node.children, ids);
    }
  });
  return ids;
};

const collectAllMenuIds = (nodes: any[], ids: number[] = []) => {
  nodes.forEach((node) => {
    ids.push(node.id);
    if (node.children && node.children.length) {
      collectAllMenuIds(node.children, ids);
    }
  });
  return ids;
};

const collectMenuIdsWithAncestors = (
  nodes: MenuTreeNode[],
  selected: Set<number>,
  ids: Set<number> = new Set(),
) => {
  nodes.forEach((node) => {
    const beforeSize = ids.size;
    if (selected.has(node.id)) {
      ids.add(node.id);
    }
    collectMenuIdsWithAncestors(node.children, selected, ids);
    if (ids.size > beforeSize) {
      ids.add(node.id);
    }
  });
  return Array.from(ids);
};

const collectCheckedMenuIds = (tree?: InstanceType<typeof ElTree>) => {
  const checkedKeys = (tree?.getCheckedKeys(false) || []) as number[];
  const halfCheckedKeys = (tree?.getHalfCheckedKeys() || []) as number[];
  return Array.from(new Set([...checkedKeys, ...halfCheckedKeys]));
};

const selectMenuGroup = (node: MenuTreeNode) => {
  selectedMenuGroupId.value = node.id;
};

const toggleMenuGroupExpanded = (node: MenuTreeNode) => {
  selectMenuGroup(node);
  if (node.children.length === 0) return;
  const expanded = new Set(expandedMenuGroupIds.value);
  if (expanded.has(node.id)) {
    expanded.delete(node.id);
  } else {
    expanded.add(node.id);
  }
  expandedMenuGroupIds.value = Array.from(expanded);
};

const isMenuChecked = (id: number) => selectedMenuIds.value.includes(id);

const updateSelectedMenuIds = (ids: number[], checked: boolean) => {
  const selected = new Set(selectedMenuIds.value);
  ids.forEach((id) => {
    if (checked) {
      selected.add(id);
      return;
    }
    selected.delete(id);
  });
  selectedMenuIds.value = Array.from(selected);
};

const toggleMenuNode = (node: MenuTreeNode, checked: boolean) => {
  updateSelectedMenuIds(flattenMenuIds([node], []), checked);
};

const handleMenuCheckChange = (node: MenuTreeNode, checked: string | number | boolean) => {
  toggleMenuNode(node, Boolean(checked));
};

const selectAllMenus = () => {
  selectedMenuIds.value = flattenMenuIds(menuTreeData.value, []);
};

const selectSelectedMenuGroupMenus = () => {
  if (!selectedMenuGroup.value) return;
  updateSelectedMenuIds(flattenMenuIds([selectedMenuGroup.value], []), true);
};

const clearSelectedMenuGroupMenus = () => {
  if (!selectedMenuGroup.value) return;
  updateSelectedMenuIds(flattenMenuIds([selectedMenuGroup.value], []), false);
};

const resetDefaultMenus = async () => {
  selectAllMenus();
  await saveMenuConfig();
};

const saveMenuConfig = async () => {
  if (!currentTenantId.value) {
    return;
  }
  const menuIds = collectMenuIdsWithAncestors(menuTreeData.value, new Set(selectedMenuIds.value));

  try {
    const res: any = await request.put(`/tenants/${currentTenantId.value}/menus`, { menuIds });
    if (res.data.code === 200) {
      notifySuccess();
      if (currentTenantId.value === authStore.tenantId) {
        window.dispatchEvent(new Event('menu:refresh'));
      }
      showMenuModal.value = false;
    }
  } catch (error) {
    notifyError(error);
  }
};

const toggleStatus = (row: Tenant) => {
  const nextEnabled = !row.enabled;
  const actionLabel = nextEnabled ? t('action.enable') : t('action.disable');

  ElMessageBox.confirm(
    `${t('action.confirm')} ${actionLabel} "${row.name}"?`,
    t('action.confirm'),
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.put(`/tenants/${row.id}/status`, { enabled: nextEnabled });
      if (res.data.code === 200) {
        notifySuccess();
        fetchTenants();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};

const handleSwitch = async (row: Tenant) => {
  try {
    const res: any = await request.post('/tenants/switch', { tenantCode: row.code });
    if (res.data.code === 200) {
      const newToken = res.data?.data?.token;
      if (typeof newToken === 'string' && newToken) {
        setTokens(newToken, res.data?.data?.authPayload);
      }
      notifySuccess();
      router.push('/');
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = (row: Tenant) => {
  if (row.code === 'default') {
    notifyWarning(t('message.defaultTenantDeleteDisabled'));
    return;
  }
  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} "${row.name}"?`,
    t('action.confirm'),
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.delete(`/tenants/${row.id}`);
      if (res.data.code === 200) {
        notifySuccess();
        fetchTenants();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};

const labelFromMenu = (node: { key?: string; title?: string }) => {
  if (node.key) {
    const normalizedKey = normalizeMenuKey(node.key);
    const translated = t(normalizedKey);
    if (translated !== normalizedKey) {
      return translated;
    }
  }
  return node.title || '-';
};

const formatTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
};
</script>

<style scoped>
.tenant-toolbar {
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

.table-filters {
  display: grid;
  grid-template-columns: 220px;
  align-items: center;
  justify-content: start;
  gap: 12px;
  min-width: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: flex-end;
  margin-left: 0;
}

:deep(.tenant-toolbar__search) {
  width: 220px;
}

.status-tags {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.menu-tree-wrapper {
  width: 100%;
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 8px;
  background: #fff;
  box-sizing: border-box;
}

:deep(.tenant-menu-dialog .el-dialog__body) {
  padding: 14px;
  background: #f8fafc;
}

:deep(.tenant-menu-dialog .el-dialog__footer) {
  padding: 12px 18px;
  border-top: 1px solid #ebeef5;
}

.tenant-menu-layout {
  height: min(560px, calc(100vh - 220px));
  min-height: 420px;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 12px;
}

.tenant-menu-sidebar,
.tenant-menu-editor {
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.tenant-menu-sidebar {
  display: flex;
  flex-direction: column;
  padding: 8px;
}

.tenant-menu-search {
  flex: 0 0 auto;
  margin-bottom: 8px;
}

.tenant-menu-tree {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tenant-menu-node {
  width: 100%;
  border: 0;
  background: transparent;
  color: #4b5563;
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.tenant-menu-node:hover {
  background: #eef5ff;
}

.tenant-menu-node.is-active {
  color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.14);
  font-weight: 600;
}

.tenant-menu-node--root {
  min-height: 36px;
  padding: 0 8px;
  border-radius: 8px;
  font-size: 14px;
}

.tenant-menu-node--child {
  min-height: 32px;
  padding: 0 8px 0 24px;
  border-radius: 8px;
  font-size: 13px;
}

.tenant-menu-node__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-menu-node__count {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.tenant-menu-node__arrow {
  color: #909399;
  font-size: 12px;
}

.tenant-menu-node__dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.45;
}

.tenant-menu-editor {
  display: flex;
  flex-direction: column;
}

.tenant-menu-toolbar {
  flex: 0 0 auto;
  min-height: 46px;
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tenant-menu-current {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.tenant-menu-current strong {
  flex: 0 0 auto;
  font-size: 15px;
}

.tenant-menu-current span {
  min-width: 0;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-menu-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.tenant-menu-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
}

.tenant-menu-section {
  margin-bottom: 14px;
}

.tenant-menu-section__title {
  min-height: 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 700;
}

.tenant-menu-section__title span:last-child {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.tenant-menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
}

.tenant-menu-check {
  min-height: 38px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px 9px;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  background: #fff;
  font-size: 13px;
}

.tenant-menu-check span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-menu-check :deep(.el-checkbox) {
  height: 18px;
}

.tenant-menu-footer {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 1280px) {
  .tenant-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.tenant-toolbar__search) {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .table-filters {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }

  :deep(.tenant-toolbar__search) {
    width: 100% !important;
  }
}
</style>
