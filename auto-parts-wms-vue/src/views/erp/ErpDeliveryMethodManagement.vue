<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpDeliveryMethodManagement') }}</h2>
      <div class="table-toolbar">
        <div class="table-filters">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="table-search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
        </div>
        <el-button type="primary" v-permission="'erp-delivery-method:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" />
          <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="160">
            <template #default="{ row }">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" type="warning" style="margin-left: 6px">
                {{ $t('field.isDefault') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('sort')" prop="sortNo" :label="$t('field.sortNo')" min-width="120" />
          <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-delivery-method:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-delivery-method:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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
        <el-form-item :label="$t('field.sortNo')">
          <el-input-number v-model="formData.sortNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.isDefault')">
          <el-switch v-model="formData.isDefault" />
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
import { ref, reactive, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

interface DeliveryMethod {
  id: number;
  code: string;
  name: string;
  sortNo?: number;
  enabled: boolean;
  isDefault?: boolean;
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
const tableData = ref<DeliveryMethod[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);

const defaultColumns = ['code', 'name', 'sort', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-delivery-method', defaultColumns);

const formData = reactive({
  code: '',
  name: '',
  sortNo: 0,
  enabled: true,
  isDefault: false,
  remark: ''
});

const canShow = (key: string) => isVisible(key);

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') params.enabled = statusFilter.value === 'enabled';

    const res: any = await request.get('/erp/delivery-methods/page', { params });
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

const openEditModal = (row: DeliveryMethod) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.sortNo = row.sortNo || 0;
  formData.enabled = row.enabled;
  formData.isDefault = Boolean(row.isDefault);
  formData.remark = row.remark || '';
  showModal.value = true;
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.sortNo = 0;
  formData.enabled = true;
  formData.isDefault = false;
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
      ? await request.put(`/erp/delivery-methods/${currentId.value}`, payload)
      : await request.post('/erp/delivery-methods', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: DeliveryMethod) => {
  try {
    await request.delete(`/erp/delivery-methods/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchList();
});
</script>
