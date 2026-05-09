<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.permissionManagement') }}</div>
      <div class="permission-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="table-search permission-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search permission-toolbar__search--narrow"
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
        <el-table 
          v-loading="loading"
          :data="treeData" 
          style="width: 100%" 
          height="100%"
          row-key="id"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          default-expand-all
          :empty-text="$t('table.empty')"
        >
        <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="200" />
        <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="150">
          <template #default="{ row }">
            <code v-if="!row.isGroup" class="code-badge">{{ row.code }}</code>
            <span v-else style="color: #909399; font-size: 12px;">(Group)</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canShow('description')" prop="description" :label="$t('field.description')" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="100">
          <template #default="{ row }">
            <el-tag v-if="!row.isGroup" :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <div v-if="!row.isGroup">
              <el-button link type="primary" size="small" @click="openEditModal(row)">
                {{ $t('action.edit') }}
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">
                {{ $t('action.delete') }}
              </el-button>
            </div>
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
import { ref, computed, reactive, onMounted, onActivated, watch } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';

// --- 类型定义 ---
interface Permission {
  id: number;
  code: string;
  name: string;
  description: string;
  enabled: boolean;
}

// --- 初始化 ---
const { t } = useI18n();
// --- 状态 ---
const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const showModal = ref(false);
const isEditing = ref(false);

const permissionList = ref<Permission[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();
const authStore = useAuthStore();
const defaultColumns = ['name', 'code', 'description', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('permission-management', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  name: 'column:permission-management:name',
  code: 'column:permission-management:code',
  description: 'column:permission-management:description',
  status: 'column:permission-management:status'
};


const formData = reactive<Omit<Permission, 'id'>>({
  name: '',
  code: '',
  description: '',
  enabled: true
});
const currentId = ref<number | null>(null);

// --- 计算属性 ---
const filteredData = computed(() => permissionList.value);

// 构建树形数据
const treeData = computed(() => {
  const groups: Record<string, any> = {};
  const flatList = filteredData.value;

  flatList.forEach(p => {
    const groupName = p.code.includes(':') ? (p.code.split(':')[0] || 'Other') : 'Other';
    const formattedGroup = groupName.charAt(0).toUpperCase() + groupName.slice(1);
    
    if (!groups[formattedGroup]) {
      groups[formattedGroup] = {
        id: `group-${formattedGroup}`, // 虚拟ID
        name: formattedGroup,
        code: '',
        description: '',
        enabled: true,
        isGroup: true,
        children: []
      };
    }
    groups[formattedGroup].children.push({ ...p, isGroup: false });
  });

  return Object.values(groups);
});

// --- 数据加载 ---
const fetchPermissions = async () => {
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

    const res: any = await request.get('/permissions/page', { params });
    if (res.data.code === 200) {
      permissionList.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchPermissions();
  bindPageSizeSync(size, fetchPermissions);
  fetchTenantKeys();
});

onActivated(() => {
  fetchPermissions();
});

// --- 方法 ---
const handleSearch = () => {
  page.value = 1;
  fetchPermissions();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchPermissions();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchPermissions();
};

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
};

const openEditModal = (row: Permission) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.name = row.name;
  formData.code = row.code;
  formData.description = row.description || '';
  formData.enabled = row.enabled;
  showModal.value = true;
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

  try {
    const url = isEditing.value && currentId.value 
      ? `/permissions/${currentId.value}` 
      : '/permissions';
    
    const method = isEditing.value ? request.put : request.post;
    const res: any = await method(url, formData);
    
    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchPermissions();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = (row: Permission) => {
  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} "${row.name}"?`,
    'Warning',
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.delete(`/permissions/${row.id}`);
      if (res.data.code === 200) {
        notifySuccess();
        fetchPermissions();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};
</script>

<style scoped>
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

:deep(.permission-toolbar__search--wide) {
  width: 220px;
}

:deep(.permission-toolbar__search--narrow) {
  width: 140px;
}

.code-badge {
  background: #f5f5f7;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #d63384;
}

@media (max-width: 1280px) {
  .permission-toolbar {
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

  :deep(.permission-toolbar__search--wide) {
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

  :deep(.permission-toolbar__search--wide),
  :deep(.permission-toolbar__search--narrow) {
    width: 100% !important;
  }
}
</style>
