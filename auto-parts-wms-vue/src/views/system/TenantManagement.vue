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
        <el-table :data="filteredTenants" style="width: 100%" height="100%" v-loading="loading" :empty-text="$t('table.empty')">
        <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.tenantCode')" min-width="160" />
        <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.tenantName')" min-width="200" />
        <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="120">
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
        </el-table-column>
        <el-table-column v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.actions')" width="340" fixed="right">
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
        </el-table-column>
        </el-table>
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
      width="640px"
      @closed="resetMenuForm"
    >
      <div class="menu-actions">
        <el-button @click="selectAllMenus">{{ $t('action.selectAll') }}</el-button>
        <el-button type="primary" plain @click="resetDefaultMenus">
          {{ $t('action.resetDefault') }}
        </el-button>
      </div>
      <div class="menu-tree-wrapper">
        <el-tree
          ref="menuTreeRef"
          :data="menuTreeData"
          show-checkbox
          check-strictly
          node-key="id"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showMenuModal = false">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveMenuConfig">{{ $t('action.save') }}</el-button>
        </span>
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
const menuTreeRef = ref<InstanceType<typeof ElTree>>();
const menuTreeData = ref<any[]>([]);
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
      const enabledIds = collectEnabledIds(res.data.data || []);
      nextTick(() => {
        menuTreeRef.value?.setCheckedKeys(enabledIds, false);
      });
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

const buildMenuTree = (nodes: any[]): any[] => {
  return nodes.map((node) => ({
    id: node.id,
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

const collectCheckedMenuIds = (tree?: InstanceType<typeof ElTree>) => {
  const checkedKeys = (tree?.getCheckedKeys(false) || []) as number[];
  const halfCheckedKeys = (tree?.getHalfCheckedKeys() || []) as number[];
  return Array.from(new Set([...checkedKeys, ...halfCheckedKeys]));
};

const selectAllMenus = () => {
  const allIds = collectAllMenuIds(menuTreeData.value, []);
  nextTick(() => {
    menuTreeRef.value?.setCheckedKeys(allIds, false);
  });
};

const resetDefaultMenus = async () => {
  selectAllMenus();
  await nextTick();
  await saveMenuConfig();
};

const saveMenuConfig = async () => {
  if (!currentTenantId.value) {
    return;
  }
  const checkedKeys = (menuTreeRef.value?.getCheckedKeys(false) || []) as number[];
  const halfCheckedKeys = (menuTreeRef.value?.getHalfCheckedKeys() || []) as number[];
  const menuIds = Array.from(new Set([...checkedKeys, ...halfCheckedKeys]));

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

.menu-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
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
