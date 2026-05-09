<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.roleManagement') }}</div>
      <div class="role-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="table-search role-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search role-toolbar__search--narrow"
              @change="handleSearch"
            >
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
          </div>
          <div class="table-actions">
            <el-button type="primary" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="filteredData" style="width: 100%" v-loading="loading" :empty-text="$t('table.empty')">
        <el-table-column type="index" :label="$t('table.index')" width="80" />
        <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="150" />
        <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="150">
          <template #default="{ row }">
            <code class="code-badge">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column v-if="canShow('description')" prop="description" :label="$t('field.description')" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :disabled="!isReservedRole(row.code)"
              content="保留角色不允许通过角色管理接口修改"
              placement="top"
            >
              <el-button
                link
                type="primary"
                size="small"
                :disabled="isReservedRole(row.code)"
                @click="openEditModal(row)"
              >
                {{ $t('action.edit') }}
              </el-button>
            </el-tooltip>
            <el-tooltip
              :disabled="!isReservedRole(row.code)"
              content="保留角色不允许通过角色管理接口删除"
              placement="top"
            >
              <el-button
                link
                type="danger"
                size="small"
                :disabled="isReservedRole(row.code)"
                @click="handleDelete(row)"
              >
                {{ $t('action.delete') }}
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        </el-table>
      </div>
      <div class="table-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 弹窗表单 -->
    <el-dialog
      v-model="showModal"
      :title="isEditing ? $t('action.edit') : $t('action.add')"
      width="600px"
      @closed="resetForm"
      top="5vh"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" placeholder="Ex: Administrator" />
        </el-form-item>
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" placeholder="Ex: admin" />
        </el-form-item>
        <el-form-item :label="$t('field.description')">
          <el-input v-model="formData.description" type="textarea" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        
        <!-- 权限分配树 -->
        <el-form-item :label="$t('field.permissions')">
          <div class="permission-tree-box">
            <el-tree
              ref="treeRef"
              :data="permissionTreeData"
              show-checkbox
              node-key="id"
              :props="{ label: 'label', children: 'children' }"
              default-expand-all
            />
          </div>
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
import { ref, computed, reactive, nextTick, onMounted, onActivated, watch } from 'vue';
import { ElMessageBox, type ElTree } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '@/stores/auth';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

// --- 类型定义 ---
interface Permission {
  id: number;
  code: string;
  name: string;
  enabled: boolean;
}

interface Role {
  id: number;
  name: string;
  code: string;
  description: string;
  enabled: boolean;
  permissionIds?: number[];
}

// --- 初始化 ---
const { t } = useI18n();
const authStore = useAuthStore();
// --- 状态 ---
const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const showModal = ref(false);
const isEditing = ref(false);
const treeRef = ref<InstanceType<typeof ElTree>>();

const roleList = ref<Role[]>([]);
const permissionList = ref<Permission[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const { bindPageSizeSync } = useSystemConfig();
const canUseTenantPermissions = computed(() => authStore.hasPermission('tenant:view'));
const { notifyError, notifySuccess, notifyWarning } = useApiError();

const defaultColumns = ['name', 'code', 'description', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('role-management', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  name: 'column:role-management:name',
  code: 'column:role-management:code',
  description: 'column:role-management:description',
  status: 'column:role-management:status'
};
const reservedRoleCodes = new Set(['admin', 'super_admin']);


const formData = reactive<Omit<Role, 'id'>>({
  name: '',
  code: '',
  description: '',
  enabled: true
});
const currentId = ref<number | null>(null);

// --- 计算属性 ---
const filteredData = computed(() => roleList.value);

// 构建权限树数据：根据 code 前缀手动分组
const permissionTreeData = computed(() => {
  const groupMap: Record<string, any[]> = {};
  
  const visiblePermissions = canUseTenantPermissions.value
    ? permissionList.value
    : permissionList.value.filter(p => !p.code.startsWith('tenant:'));

  visiblePermissions.forEach(p => {
    const groupName = p.code.includes(':') ? (p.code.split(':')[0] || 'Other') : 'Other';
    const formattedGroup = groupName.charAt(0).toUpperCase() + groupName.slice(1);
    
    if (!groupMap[formattedGroup]) {
      groupMap[formattedGroup] = [];
    }
    groupMap[formattedGroup].push({
      id: p.id,
      label: `${p.name} (${p.code})`
    });
  });

  return Object.keys(groupMap).map(group => ({
    id: `group-${group}`,
    label: group,
    children: groupMap[group]
  }));
});

// --- 数据加载 ---
const fetchRoles = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') {
      params.enabled = statusFilter.value === 'enabled';
    }

    const res: any = await request.get('/roles/page', { params });
    if (res.data.code === 200) {
      roleList.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const fetchPermissions = async () => {
  try {
    const res: any = await request.get('/permissions');
    if (res.data.code === 200) {
      permissionList.value = res.data.data;
    }
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchRoles();
  fetchPermissions();
  bindPageSizeSync(size, fetchRoles);
  fetchTenantKeys();
});

onActivated(() => {
  // 当组件被激活时，刷新数据以确保权限树和角色列表是最新的
  fetchRoles();
  fetchPermissions();
});

// --- 方法 ---
const handleSearch = () => {
  page.value = 1;
  fetchRoles();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchRoles();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchRoles();
};

const isReservedRole = (code?: string) => reservedRoleCodes.has((code || '').trim().toLowerCase());

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
  nextTick(() => {
    treeRef.value?.setCheckedKeys([], false);
  });
};

const openEditModal = async (row: Role) => {
  if (isReservedRole(row.code)) {
    notifyWarning('保留角色不允许通过角色管理接口修改');
    return;
  }

  isEditing.value = true;
  currentId.value = row.id;
  formData.name = row.name;
  formData.code = row.code;
  formData.description = row.description || '';
  formData.enabled = row.enabled;
  
  showModal.value = true;

  // 获取该角色的权限列表
  try {
    const res: any = await request.get(`/roles/${row.id}/permissions`);
    if (res.data.code === 200) {
      const assignedIds = res.data.data.map((p: any) => p.id);
      nextTick(() => {
        treeRef.value?.setCheckedKeys(assignedIds, false);
      });
    }
  } catch (e) {
    console.error('Failed to load role permissions', e);
  }
};

const resetForm = () => {
  formData.name = '';
  formData.code = '';
  formData.description = '';
  formData.enabled = true;
};

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const saveData = async () => {
  if (!formData.name || !formData.code) {
    notifyWarning(t('message.required'));
    return;
  }

  if (isEditing.value && isReservedRole(formData.code)) {
    notifyWarning('保留角色不允许通过角色管理接口修改');
    return;
  }

  try {
    const url = isEditing.value && currentId.value 
      ? `/roles/${currentId.value}` 
      : '/roles';
    
    const method = isEditing.value ? request.put : request.post;

    // 1. 保存角色基本信息
    const res: any = await method(url, {
      name: formData.name,
      code: formData.code,
      description: formData.description,
      enabled: formData.enabled
    });
    
    if (res.data.code === 200) {
      const roleId = isEditing.value ? currentId.value : res.data.data.id;
      
      // 2. 批量设置权限
      const checkedKeys = treeRef.value?.getCheckedKeys(true) || [];
      const permissionIds = checkedKeys.filter(k => typeof k === 'number') as number[];
      
      await request.put(`/roles/${roleId}/permissions`, { permissionIds });

      notifySuccess();
      showModal.value = false;
      fetchRoles();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = (row: Role) => {
  if (isReservedRole(row.code)) {
    notifyWarning('保留角色不允许通过角色管理接口删除');
    return;
  }

  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} "${row.name}"?`,
    'Warning',
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.delete(`/roles/${row.id}`);
      if (res.data.code === 200) {
        notifySuccess();
        fetchRoles();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};
</script>

<style scoped>
.role-toolbar {
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
  grid-template-columns: 220px 140px;
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

:deep(.role-toolbar__search--wide) {
  width: 220px;
}

:deep(.role-toolbar__search--narrow) {
  width: 140px;
}

.code-badge {
  background: #fff8e1;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #b45309;
}

.permission-tree-box {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
}

@media (max-width: 1280px) {
  .role-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px 140px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.role-toolbar__search--wide) {
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

  :deep(.role-toolbar__search--wide),
  :deep(.role-toolbar__search--narrow) {
    width: 100% !important;
  }
}
</style>
