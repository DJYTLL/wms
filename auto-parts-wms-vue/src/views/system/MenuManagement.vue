<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.menuManagement') }}</div>
      <div class="menu-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchKeyword"
              :placeholder="$t('action.search')"
              class="table-search menu-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search menu-toolbar__search--narrow"
              @change="handleSearch"
            >
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
            <el-select
              v-model="sortField"
              :placeholder="$t('sort.by')"
              class="table-search menu-toolbar__search--narrow"
              @change="applySort"
            >
              <el-option :label="$t('menu.sort')" value="sort" />
              <el-option :label="$t('menu.title')" value="title" />
              <el-option :label="$t('menu.code')" value="code" />
            </el-select>
            <el-select
              v-model="sortOrder"
              :placeholder="$t('sort.order')"
              class="table-search menu-toolbar__search--narrow"
              @change="applySort"
            >
              <el-option :label="$t('sort.asc')" value="asc" />
              <el-option :label="$t('sort.desc')" value="desc" />
            </el-select>
          </div>
          <div class="table-actions">
            <el-button @click="expandAll">{{ $t('action.expandAll') }}</el-button>
            <el-button @click="collapseAll">{{ $t('action.collapseAll') }}</el-button>
            <el-button type="primary" @click="openCreateRoot">{{ $t('menu.addRoot') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable
          ref="tableRef"
          v-loading="loading"
          :data="filteredMenuData"
          row-key="id"
          :tree-props="{ children: 'children' }"
          :expand-row-keys="expandedRowKeys"
          height="100%"
          :empty-text="$t('table.empty')"
         table-key="menu-management">
        <ErpDataTableColumn v-if="canShow('title')" prop="title" :label="$t('menu.title')" min-width="200" />
        <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('menu.code')" min-width="160" />
        <ErpDataTableColumn v-if="canShow('path')" prop="path" :label="$t('menu.path')" min-width="200" />
        <ErpDataTableColumn v-if="canShow('permissionCode')" prop="permissionCode" :label="$t('menu.permission')" min-width="200" />
        <ErpDataTableColumn v-if="canShow('sort')" prop="sort" :label="$t('menu.sort')" width="100" />
        <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
            </el-tag>
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn :label="$t('table.actions')" width="220" fixed="right" column-key="actions">
          <template #default="{ row }">
            <el-button link size="small" @click.stop="openCreateChild(row)">
              {{ $t('menu.addChild') }}
            </el-button>
            <el-button link size="small" @click.stop="openEdit(row)">
              {{ $t('action.edit') }}
            </el-button>
            <el-button link size="small" type="danger" @click.stop="handleDelete(row)">
              {{ $t('action.delete') }}
            </el-button>
          </template>
        </ErpDataTableColumn>
        </ErpDataTable>
      </div>
    </div>

    <el-dialog
      v-model="showModal"
      :title="isEditing ? $t('action.edit') : $t('menu.formCreate')"
      width="640px"
      @closed="resetForm"
    >
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('menu.code')" required>
          <el-input v-model="formData.code" placeholder="menu-code" />
        </el-form-item>
        <el-form-item :label="$t('menu.title')" required>
          <el-input v-model="formData.title" placeholder="Menu Title" />
        </el-form-item>
        <el-form-item :label="$t('menu.parent')">
          <el-select v-model="formData.parentId" clearable :placeholder="$t('menu.parent')" style="width: 100%">
            <el-option :label="$t('menu.parentNone')" :value="null" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('menu.path')">
          <el-input v-model="formData.path" placeholder="/path" />
        </el-form-item>
        <el-form-item :label="$t('menu.i18n')">
          <el-input v-model="formData.i18nKey" placeholder="menu-key" />
        </el-form-item>
        <el-form-item :label="$t('menu.permission')">
          <el-input v-model="formData.permissionCode" placeholder="permission:code" />
        </el-form-item>
        <el-form-item :label="$t('menu.sort')">
          <el-input-number v-model="formData.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('menu.enabled')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-form-item :label="$t('menu.icon')">
          <el-input v-model="formData.icon" type="textarea" :rows="4" placeholder="<svg ... />" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveMenu">{{ $t('action.save') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';

interface MenuItem {
  id: number;
  code: string;
  parentId?: number | null;
  title: string;
  i18nKey?: string | null;
  path?: string | null;
  icon?: string | null;
  permissionCode?: string | null;
  sort?: number | null;
  enabled: boolean;
  children?: MenuItem[];
}

const { t } = useI18n();
const authStore = useAuthStore();
const menuTreeData = ref<MenuItem[]>([]);
const loading = ref(false);
const searchKeyword = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const sortField = ref<'sort' | 'title' | 'code'>('sort');
const sortOrder = ref<'asc' | 'desc'>('asc');
const expandedRowKeys = ref<number[]>([]);
const tableRef = ref();
const showModal = ref(false);
const isEditing = ref(false);
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const defaultColumns = ['title', 'code', 'path', 'permissionCode', 'sort', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('menu-management', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  title: 'column:menu-management:title',
  code: 'column:menu-management:code',
  path: 'column:menu-management:path',
  permissionCode: 'column:menu-management:permissionCode',
  sort: 'column:menu-management:sort',
  status: 'column:menu-management:status'
};


const formData = reactive({
  id: null as number | null,
  code: '',
  parentId: null as number | null,
  title: '',
  i18nKey: '',
  path: '',
  icon: '',
  permissionCode: '',
  sort: 0,
  enabled: true
});

const fetchMenus = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/menus/all');
    if (res.data.code === 200) {
      menuTreeData.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchMenus();
  fetchTenantKeys();
});

const flattenMenus = (items: MenuItem[], acc: Array<{ id: number; label: string }> = []) => {
  items.forEach((item) => {
    acc.push({ id: item.id, label: item.title });
    if (item.children && item.children.length) {
      flattenMenus(item.children, acc);
    }
  });
  return acc;
};

const parentOptions = computed(() => {
  const options = flattenMenus(menuTreeData.value, []);
  if (isEditing.value && formData.id) {
    return options.filter((item) => item.id !== formData.id);
  }
  return options;
});

const matchesFilters = (menu: MenuItem, keyword: string) => {
  const haystack = [
    menu.title,
    menu.code,
    menu.path,
    menu.permissionCode,
    menu.i18nKey
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();

  const keywordMatched = !keyword || haystack.includes(keyword);
  const statusMatched =
    statusFilter.value === 'all' ||
    (statusFilter.value === 'enabled' && menu.enabled) ||
    (statusFilter.value === 'disabled' && !menu.enabled);

  return keywordMatched && statusMatched;
};

const sortMenus = (items: MenuItem[]): MenuItem[] => {
  const sorted = [...items].sort((a, b) => {
    const direction = sortOrder.value === 'asc' ? 1 : -1;
    if (sortField.value === 'sort') {
      const left = typeof a.sort === 'number' ? a.sort : 0;
      const right = typeof b.sort === 'number' ? b.sort : 0;
      return (left - right) * direction;
    }
    const left = (a[sortField.value] || '').toString().toLowerCase();
    const right = (b[sortField.value] || '').toString().toLowerCase();
    return left.localeCompare(right) * direction;
  });

  return sorted.map((item): MenuItem => ({
    ...item,
    children: item.children ? sortMenus(item.children) : undefined
  }));
};

const filterMenus = (items: MenuItem[], keyword: string): MenuItem[] => {
  const result: MenuItem[] = [];
  items.forEach((item) => {
    const matched = matchesFilters(item, keyword);
    const children = item.children ? filterMenus(item.children, keyword) : [];

    if (matched || children.length > 0) {
      result.push({
        ...item,
        children: children.length > 0 ? children : undefined
      });
    }
  });
  return result;
};

const filteredMenuData = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  const filtered = filterMenus(menuTreeData.value, keyword);
  return sortMenus(filtered);
});

const collectExpandableKeys = (items: MenuItem[]): number[] => {
  const keys: number[] = [];
  items.forEach((item) => {
    if (item.children && item.children.length) {
      keys.push(item.id);
      keys.push(...collectExpandableKeys(item.children));
    }
  });
  return keys;
};

const visitTreeRows = (items: MenuItem[], visitor: (item: MenuItem) => void) => {
  items.forEach((item) => {
    if (item.children && item.children.length) {
      visitor(item);
      visitTreeRows(item.children, visitor);
    }
  });
};

const handleSearch = () => {
  if (searchKeyword.value || statusFilter.value !== 'all') {
    expandedRowKeys.value = collectExpandableKeys(filteredMenuData.value);
  }
};

const applySort = () => {
  if (expandedRowKeys.value.length) {
    expandedRowKeys.value = collectExpandableKeys(filteredMenuData.value);
  }
};

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const expandAll = async () => {
  const keys = collectExpandableKeys(filteredMenuData.value);
  expandedRowKeys.value = keys;
  await nextTick();
  visitTreeRows(filteredMenuData.value, (item) => {
    tableRef.value?.toggleRowExpansion(item, true);
  });
};

const collapseAll = async () => {
  expandedRowKeys.value = [];
  await nextTick();
  visitTreeRows(filteredMenuData.value, (item) => {
    tableRef.value?.toggleRowExpansion(item, false);
  });
};

watch(filteredMenuData, (value) => {
  if (searchKeyword.value || statusFilter.value !== 'all') {
    expandedRowKeys.value = collectExpandableKeys(value);
  }
});

const openCreateRoot = () => {
  isEditing.value = false;
  resetForm();
  formData.parentId = null;
  showModal.value = true;
};

const openCreateChild = (menu: MenuItem) => {
  isEditing.value = false;
  resetForm();
  formData.parentId = menu.id;
  showModal.value = true;
};

const openEdit = (menu: MenuItem) => {
  isEditing.value = true;
  formData.id = menu.id;
  formData.code = menu.code || '';
  formData.parentId = menu.parentId ?? null;
  formData.title = menu.title || '';
  formData.i18nKey = menu.i18nKey || '';
  formData.path = menu.path || '';
  formData.icon = menu.icon || '';
  formData.permissionCode = menu.permissionCode || '';
  formData.sort = menu.sort ?? 0;
  formData.enabled = menu.enabled;
  showModal.value = true;
};

const resetForm = () => {
  formData.id = null;
  formData.code = '';
  formData.parentId = null;
  formData.title = '';
  formData.i18nKey = '';
  formData.path = '';
  formData.icon = '';
  formData.permissionCode = '';
  formData.sort = 0;
  formData.enabled = true;
};

const saveMenu = async () => {
  if (!formData.code || !formData.title) {
    notifyWarning(t('message.required'));
    return;
  }
  const payload = {
    code: formData.code,
    parentId: formData.parentId,
    title: formData.title,
    i18nKey: formData.i18nKey || undefined,
    path: formData.path || undefined,
    icon: formData.icon || undefined,
    permissionCode: formData.permissionCode || undefined,
    sort: formData.sort,
    enabled: formData.enabled
  };

  try {
    if (isEditing.value && formData.id) {
      const res: any = await request.put(`/menus/${formData.id}`, payload);
      if (res.data.code === 200) {
        notifySuccess();
        showModal.value = false;
        fetchMenus();
        window.dispatchEvent(new Event('menu:refresh'));
      }
      return;
    }

    const res: any = await request.post('/menus', payload);
    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchMenus();
      window.dispatchEvent(new Event('menu:refresh'));
    }
  } catch (error) {
    console.error('Save menu failed', error);
    notifyError(error);
  }
};

const handleDelete = (menu: MenuItem) => {
  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} \"${menu.title}\"?`,
    t('action.confirm'),
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.delete(`/menus/${menu.id}`);
      if (res.data.code === 200) {
        notifySuccess();
        fetchMenus();
        window.dispatchEvent(new Event('menu:refresh'));
      }
    } catch (error) {
      console.error('Delete menu failed', error);
      notifyError(error);
    }
  });
};
</script>

<style scoped>
.menu-toolbar {
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
  grid-template-columns: 220px 140px 140px 140px;
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

:deep(.menu-toolbar__search--wide) {
  width: 220px;
}

:deep(.menu-toolbar__search--narrow) {
  width: 140px;
}

@media (max-width: 1280px) {
  .menu-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px 140px 140px 140px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.menu-toolbar__search--wide) {
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
    flex-wrap: wrap;
  }

  :deep(.menu-toolbar__search--wide),
  :deep(.menu-toolbar__search--narrow) {
    width: 100% !important;
  }
}
</style>
