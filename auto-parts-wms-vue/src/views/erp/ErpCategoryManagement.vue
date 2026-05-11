<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCategoryManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--2">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="table-search erp-basic-field--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" v-permission="'erp-category:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" />
          <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="140" />
          <el-table-column v-if="canShow('parent')" :label="$t('field.parentCategory')" min-width="140">
            <template #default="{ row }">
              {{ getParentName(row.parentId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('level')" prop="level" :label="$t('field.level')" width="100" />
          <el-table-column v-if="canShow('sort')" prop="sortNo" :label="$t('field.sortNo')" width="120" />
          <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-category:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-category:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="640px" @closed="resetForm">
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item :label="$t('field.parentCategory')">
          <el-select v-model="formData.parentId" clearable style="width: 100%">
            <el-option :label="$t('filter.all')" :value="null" />
            <el-option v-for="item in parentCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.level')">
          <el-input v-model="formData.level" type="number" />
        </el-form-item>
        <el-form-item :label="$t('field.sortNo')">
          <el-input v-model="formData.sortNo" type="number" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

interface OptionItem {
  id: number;
  name: string;
}

interface ErpCategory {
  id: number;
  code: string;
  name: string;
  parentId?: number | null;
  level?: number;
  sortNo?: number;
  enabled: boolean;
  remark?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<ErpCategory[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);

const categoryOptions = ref<OptionItem[]>([]);

const defaultColumns = ['code', 'name', 'parent', 'level', 'sort', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-category', defaultColumns);

const formData = reactive({
  code: '',
  name: '',
  parentId: null as number | null,
  level: undefined as number | undefined,
  sortNo: undefined as number | undefined,
  enabled: true,
  remark: ''
});

const canShow = (key: string) => isVisible(key);
const parentCategoryOptions = computed(() => categoryOptions.value.filter(item => item.id !== currentId.value));

const getParentName = (id?: number | null) => {
  if (!id) return '-';
  return categoryOptions.value.find(item => item.id === id)?.name || '-';
};

const fetchCategories = async () => {
  try {
    const res: any = await request.get('/erp/categories');
    categoryOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') params.enabled = statusFilter.value === 'enabled';

    const res: any = await request.get('/erp/categories/page', { params });
    if (res.data.code === 200) {
      tableData.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchList();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchList();
};

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
};

const openEditModal = (row: ErpCategory) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.parentId = row.parentId ?? null;
  formData.level = row.level;
  formData.sortNo = row.sortNo;
  formData.enabled = row.enabled;
  formData.remark = row.remark || '';
  showModal.value = true;
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.parentId = null;
  formData.level = undefined;
  formData.sortNo = undefined;
  formData.enabled = true;
  formData.remark = '';
};

const saveData = async () => {
  if (!formData.code || !formData.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = { ...formData };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/categories/${currentId.value}`, payload)
      : await request.post('/erp/categories', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
      fetchCategories();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpCategory) => {
  try {
    await request.delete(`/erp/categories/${row.id}`);
    notifySuccess();
    fetchList();
    fetchCategories();
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchCategories();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchCategories();
  fetchList();
});
</script>
