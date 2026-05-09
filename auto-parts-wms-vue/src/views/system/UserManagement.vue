<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.userManagement') }}</div>
      <div class="user-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="table-search user-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search user-toolbar__search--narrow"
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
        <el-table :data="filteredData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
        <el-table-column type="index" :label="$t('table.index')" width="60" />
        
        <!-- 头像列 -->
        <el-table-column v-if="canShow('avatar')" :label="$t('field.avatar')" width="70" align="center">
          <template #default="{ row }">
            <img 
              :src="row.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
              class="avatar-img" 
              alt="avatar"
            />
          </template>
        </el-table-column>

        <el-table-column v-if="canShow('username')" prop="username" :label="$t('field.username')" min-width="120" show-overflow-tooltip />
        <el-table-column v-if="canShow('displayName')" prop="displayName" :label="$t('field.name')" min-width="120" show-overflow-tooltip />
        
        <!-- 角色列 -->
        <el-table-column v-if="canShow('roles')" :label="$t('field.roles')" min-width="150">
          <template #default="{ row }">
            <div class="role-tags">
              <el-tag 
                v-for="role in (row.roles || [])" 
                :key="role.id" 
                size="small" 
                effect="plain"
              >
                {{ role.name }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <!-- 联系方式列 -->
        <el-table-column v-if="canShow('contact')" :label="$t('field.contact')" min-width="180">
          <template #default="{ row }">
            <div v-if="row.email" class="contact-item">
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
              {{ row.email }}
            </div>
            <div v-if="row.phone" class="contact-item">
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
              {{ row.phone }}
            </div>
          </template>
        </el-table-column>

        <!-- 状态列：包含 Enabled 和安全状态 -->
        <el-table-column v-if="canShow('status')" :label="$t('field.status')" width="140">
          <template #default="{ row }">
            <div class="status-column">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
              <!-- 额外的安全状态提示 -->
              <el-tooltip content="Account Locked" v-if="!row.accountNonLocked">
                <span class="status-dot locked"></span>
              </el-tooltip>
              <el-tooltip content="Account Expired" v-if="!row.accountNonExpired">
                <span class="status-dot expired"></span>
              </el-tooltip>
              <el-tooltip content="Credentials Expired" v-if="!row.credentialsNonExpired">
                <span class="status-dot cred-expired"></span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <!-- 时间信息 -->
        <el-table-column v-if="canShow('loginTime')" :label="$t('field.loginTime')" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="time-info">
              <div v-if="row.lastLoginAt"><small>{{ formatDate(row.lastLoginAt) }}</small></div>
              <div v-else><small>{{ $t('message.neverLoggedIn') }}</small></div>
              <div style="color: #999;"><small>{{ $t('field.createdTime') }}: {{ formatDate(row.createdAt) }}</small></div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column :label="$t('table.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditModal(row)">
              {{ $t('action.edit') }}
            </el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">
              {{ $t('action.resetPassword') }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              {{ $t('action.delete') }}
            </el-button>
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
      <el-form :model="formData" label-width="120px" class="user-form">
        <!-- 基本信息 -->
        <h4 class="form-section-title">{{ $t('section.basicInfo') }}</h4>
        <div class="form-row">
          <el-form-item :label="$t('field.username')" required class="half-width">
            <el-input v-model="formData.username" :disabled="isEditing" placeholder="username" />
          </el-form-item>
          <el-form-item :label="$t('field.name')" required class="half-width">
            <el-input v-model="formData.displayName" placeholder="Real Name" />
          </el-form-item>
        </div>

        <el-form-item v-if="!isEditing" :label="$t('field.password')" required>
          <el-input v-model="formData.password" type="password" show-password placeholder="Enter password" />
        </el-form-item>

        <!-- 联系信息 -->
        <h4 class="form-section-title">{{ $t('section.contact') }}</h4>
        <div class="form-row">
          <el-form-item :label="$t('field.email')" class="half-width">
            <el-input v-model="formData.email" placeholder="example@mail.com" />
          </el-form-item>
          <el-form-item :label="$t('field.phone')" class="half-width">
            <el-input v-model="formData.phone" placeholder="Phone Number" />
          </el-form-item>
        </div>
        <el-form-item :label="$t('field.avatar')">
          <el-input v-model="formData.avatarUrl" placeholder="https://..." />
        </el-form-item>

        <!-- 角色分配 -->
        <h4 class="form-section-title">{{ $t('field.roles') }}</h4>
        <el-form-item :label="$t('field.roles')">
          <el-select 
            v-model="formData.roleIds" 
            multiple 
            placeholder="Select roles"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>

        <!-- 账户状态 (Advanced) -->
        <h4 class="form-section-title">{{ $t('section.accountStatus') }}</h4>
        <div class="status-grid">
          <el-form-item :label="$t('field.status')" label-width="80px">
            <el-switch 
              v-model="formData.enabled" 
              :active-text="$t('status.active')" 
              :inactive-text="$t('status.inactive')" 
              inline-prompt
            />
          </el-form-item>
          <el-form-item :label="$t('field.unlock')" label-width="100px">
            <el-switch v-model="formData.accountNonLocked" />
          </el-form-item>
          <el-form-item :label="$t('field.accountValid')" label-width="100px">
            <el-switch v-model="formData.accountNonExpired" />
          </el-form-item>
          <el-form-item :label="$t('field.credentialsValid')" label-width="100px">
            <el-switch v-model="formData.credentialsNonExpired" />
          </el-form-item>
        </div>

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
import request from '@/utils/request';
import { ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';

// --- 类型定义 ---
interface Role {
  id: number;
  name: string;
}

interface SysUser {
  id: number;
  username: string;
  displayName: string;
  email?: string;
  phone?: string;
  avatarUrl?: string;
  roles?: Role[]; 
  roleIds?: number[]; 
  enabled: boolean;
  accountNonExpired: boolean;
  accountNonLocked: boolean;
  credentialsNonExpired: boolean;
  lastLoginAt?: string;
  createdAt?: string;
  password?: string; // 仅前端表单使用
}

// --- 初始化 ---
const { t } = useI18n();
const authStore = useAuthStore();

// --- 状态 ---
const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const showModal = ref(false);
const isEditing = ref(false);

const userList = ref<SysUser[]>([]);
const roleList = ref<Role[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();
const defaultColumns = ['avatar', 'username', 'displayName', 'roles', 'contact', 'status', 'loginTime'];
const { isVisible, fetchTenantKeys } = useColumnSettings('user-management', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  avatar: 'column:user-management:avatar',
  username: 'column:user-management:username',
  displayName: 'column:user-management:displayName',
  roles: 'column:user-management:roles',
  contact: 'column:user-management:contact',
  status: 'column:user-management:status',
  loginTime: 'column:user-management:loginTime'
};


// 表单数据，包含 API 所有字段
const formData = reactive<{
  username: string;
  password?: string; 
  displayName: string;
  email: string;
  phone: string;
  avatarUrl: string;
  roleIds: number[];
  enabled: boolean;
  accountNonExpired: boolean;
  accountNonLocked: boolean;
  credentialsNonExpired: boolean;
}>({
  username: '',
  password: '',
  displayName: '',
  email: '',
  phone: '',
  avatarUrl: '',
  roleIds: [],
  enabled: true,
  accountNonExpired: true,
  accountNonLocked: true,
  credentialsNonExpired: true
});
const currentId = ref<number | null>(null);

// --- 计算属性 ---
const filteredData = computed(() => userList.value);

// --- API 辅助方法 ---
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString();
};

// --- 数据加载 ---
const fetchUsers = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') {
      params.enabled = statusFilter.value === 'enabled';
    }

    const res: any = await request.get('/users/page', { params });
    if (res.data.code === 200) {
      userList.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
      // 并行获取角色
      await Promise.all(userList.value.map(async (user: SysUser) => {
        try {
          const roleRes: any = await request.get(`/users/${user.id}/roles`);
          if (roleRes.data.code === 200) user.roles = roleRes.data.data;
        } catch (e) { notifyError(e); }
      }));
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const fetchRoles = async () => {
  try {
    const res: any = await request.get('/users/role-options');
    if (res.data.code === 200) roleList.value = res.data.data;
  } catch (error) { notifyError(error); }
};

onMounted(() => {
  fetchUsers();
  fetchRoles();
  bindPageSizeSync(size, fetchUsers);
  fetchTenantKeys();
});

onActivated(() => {
  fetchUsers();
  fetchRoles();
});

// --- 操作方法 ---
const handleSearch = () => {
  page.value = 1;
  fetchUsers();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchUsers();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchUsers();
};

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
};

const openEditModal = async (row: SysUser) => {
  isEditing.value = true;
  currentId.value = row.id;
  
  // 回显基本字段
  formData.username = row.username;
  formData.displayName = row.displayName;
  formData.email = row.email || '';
  formData.phone = row.phone || '';
  formData.avatarUrl = row.avatarUrl || '';
  formData.enabled = row.enabled;
  formData.accountNonExpired = row.accountNonExpired;
  formData.accountNonLocked = row.accountNonLocked;
  formData.credentialsNonExpired = row.credentialsNonExpired;
  
  formData.password = ''; // 编辑不回显密码
  
  showModal.value = true;

  // 回显角色
  try {
    const res: any = await request.get(`/users/${row.id}/roles`);
    if (res.data.code === 200) {
      formData.roleIds = res.data.data.map((r: any) => r.id);
    }
  } catch (e) {
    if (row.roles && Array.isArray(row.roles)) {
      formData.roleIds = row.roles.map(r => r.id);
    }
  }
};

const resetForm = () => {
  formData.username = '';
  formData.password = '';
  formData.displayName = '';
  formData.email = '';
  formData.phone = '';
  formData.avatarUrl = '';
  formData.roleIds = [];
  
  // Reset booleans to default true
  formData.enabled = true;
  formData.accountNonExpired = true;
  formData.accountNonLocked = true;
  formData.credentialsNonExpired = true;
};

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const saveData = async () => {
  if (!formData.username || !formData.displayName) {
    notifyWarning(t('message.required'));
    return;
  }

  if (!isEditing.value && !formData.password) {
    notifyWarning(t('message.passwordRequired'));
    return;
  }

  try {
    const url = isEditing.value && currentId.value 
      ? `/users/${currentId.value}` 
      : '/users';
    
    // Axios method
    const method = isEditing.value ? request.put : request.post;

    const body: any = { ...formData };
    if (isEditing.value) delete body.password; // 编辑时不传空密码

    const res: any = await method(url, body);
    
    if (res.data.code === 200) {
      const userId = isEditing.value ? currentId.value : res.data.data.id;

      // 保存角色
      await request.put(`/users/${userId}/roles`, { roleIds: formData.roleIds });

      notifySuccess();
      showModal.value = false;
      fetchUsers();
    }
  } catch (error) {
    // 拦截器已处理错误提示
    notifyError(error);
  }
};

const handleDelete = (row: SysUser) => {
  if (row.username === 'admin') {
    notifyWarning(t('message.superAdminDeleteDisabled'));
    return;
  }
  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} "${row.username}"?`,
    'Warning',
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.delete(`/users/${row.id}`);
      if (res.data.code === 200) {
        notifySuccess();
        fetchUsers();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};

const handleResetPassword = (row: SysUser) => {
  ElMessageBox.prompt(t('message.inputNewPassword'), t('action.resetPassword'), {
    confirmButtonText: t('action.confirm'),
    cancelButtonText: t('action.cancel'),
    inputPattern: /.+/,
    inputErrorMessage: t('message.passwordEmpty')
  }).then(async ({ value }) => {
    try {
      const res: any = await request.post(`/users/${row.id}/reset-password`, { newPassword: value });
      if (res.data.code === 200) {
        notifySuccess();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};
</script>

<style scoped>
.user-toolbar {
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

:deep(.user-toolbar__search--wide) {
  width: 220px;
}

:deep(.user-toolbar__search--narrow) {
  width: 140px;
}

.role-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
  margin-bottom: 2px;
}

.status-column {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.status-dot.locked { background-color: #ff9800; border: 1px solid #fff; box-shadow: 0 0 0 1px #ff9800; }
.status-dot.expired { background-color: #9c27b0; }
.status-dot.cred-expired { background-color: #607d8b; }

.time-info {
  font-size: 12px;
  line-height: 1.4;
}

/* Form Styles */
.user-form {
  padding: 0 10px;
}

.form-section-title {
  margin: 16px 0 12px;
  font-size: 14px;
  color: #909399;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 4px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.half-width {
  flex: 1;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

@media (max-width: 1280px) {
  .user-toolbar {
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

  :deep(.user-toolbar__search--wide) {
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

  :deep(.user-toolbar__search--wide),
  :deep(.user-toolbar__search--narrow) {
    width: 100% !important;
  }
}
</style>
