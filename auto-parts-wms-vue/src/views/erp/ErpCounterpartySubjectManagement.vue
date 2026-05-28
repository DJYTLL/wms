<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCounterpartySubjectManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--4">
            <el-input v-model="nameQuery" :placeholder="$t('field.name')" class="table-search erp-basic-field--narrow" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
            <el-input v-model="regionQuery" :placeholder="$t('field.region')" class="table-search erp-basic-field--narrow" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
            <el-input v-model="creditCodeQuery" :placeholder="$t('field.unifiedCreditCode')" class="table-search erp-basic-field--narrow" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
            <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSearch">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button @click="handleReset">{{ $t('action.resetDefault') }}</el-button>
            <el-button type="primary" v-permission="'erp-counterparty-subject:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-counterparty-subject-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('region')" prop="region" :label="$t('field.region')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('unifiedCreditCode')" prop="unifiedCreditCode" :label="$t('field.unifiedCreditCode')" min-width="220" />
          <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('remark')" prop="remark" :label="$t('field.remark')" min-width="200" />
          <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('updatedAt')" prop="updatedAt" :label="$t('field.updatedTime')" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-counterparty-subject:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-counterparty-subject:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>
      <div class="table-pagination">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total" :current-page="page" :page-size="size" :page-sizes="[10, 20, 50, 100]" @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </div>

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="680px" @closed="resetForm">
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('field.name')" required><el-input v-model="formData.name" /></el-form-item>
        <el-form-item :label="$t('field.region')"><el-input v-model="formData.region" /></el-form-item>
        <el-form-item :label="$t('field.unifiedCreditCode')"><el-input v-model="formData.unifiedCreditCode" /></el-form-item>
        <el-form-item :label="$t('field.status')"><el-switch v-model="formData.enabled" /></el-form-item>
        <el-form-item :label="$t('field.remark')"><el-input v-model="formData.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

interface ErpCounterpartySubject {
  id: number;
  name: string;
  region?: string;
  unifiedCreditCode?: string;
  enabled: boolean;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();

const nameQuery = ref('');
const regionQuery = ref('');
const creditCodeQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<ErpCounterpartySubject[]>([]);
const allTableData = ref<ErpCounterpartySubject[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);

const defaultColumns = ['name', 'region', 'unifiedCreditCode', 'status', 'remark', 'createdAt', 'updatedAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-counterparty-subject', defaultColumns);

const formData = reactive({
  name: '',
  region: '',
  unifiedCreditCode: '',
  enabled: true,
  remark: ''
});

const canShow = (key: string) => isVisible(key);

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

const applySearch = () => {
  let filtered = allTableData.value.slice();
  if (statusFilter.value !== 'all') {
    filtered = filtered.filter(row => row.enabled === (statusFilter.value === 'enabled'));
  }
  filtered = filterByFuzzyKeyword(filtered, nameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, regionQuery.value, row => [row.region]);
  filtered = filterByFuzzyKeyword(filtered, creditCodeQuery.value, row => [row.unifiedCreditCode]);
  total.value = filtered.length;
  const start = (page.value - 1) * size.value;
  tableData.value = filtered.slice(start, start + size.value);
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/erp/counterparty-subjects');
    if (res.data.code === 200) {
      allTableData.value = res.data.data || [];
      applySearch();
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

const handleReset = () => {
  nameQuery.value = '';
  regionQuery.value = '';
  creditCodeQuery.value = '';
  statusFilter.value = 'all';
  handleSearch();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  applySearch();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  applySearch();
};

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
};

const openEditModal = (row: ErpCounterpartySubject) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.name = row.name;
  formData.region = row.region || '';
  formData.unifiedCreditCode = row.unifiedCreditCode || '';
  formData.enabled = row.enabled;
  formData.remark = row.remark || '';
  showModal.value = true;
};

const resetForm = () => {
  formData.name = '';
  formData.region = '';
  formData.unifiedCreditCode = '';
  formData.enabled = true;
  formData.remark = '';
};

const saveData = async () => {
  if (!formData.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = {
      name: formData.name,
      region: formData.region || undefined,
      unifiedCreditCode: formData.unifiedCreditCode || undefined,
      enabled: formData.enabled,
      remark: formData.remark || undefined
    };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/counterparty-subjects/${currentId.value}`, payload)
      : await request.post('/erp/counterparty-subjects', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpCounterpartySubject) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `${t('message.deleteConfirm')} ${row.name}`,
      t('action.delete'),
      {
        inputPlaceholder: t('action.deleteReason'),
        inputPattern: /^(?=.*\\S).{2,500}$/,
        inputErrorMessage: t('message.deleteReasonMin'),
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel'),
        type: 'warning',
        closeOnClickModal: false
      }
    );

    await request.delete(`/erp/counterparty-subjects/${row.id}`, {
      data: { reason: String(value).trim() },
      skipDeleteReasonPrompt: true
    } as any);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

bindPageSizeSync(size, fetchList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: () => {
    pageSizeSyncReady.value = true;
    if (pendingInitialLoad.value) {
      pendingInitialLoad.value = false;
      fetchList();
    }
  }
});

onMounted(() => {
  fetchTenantKeys();
  if (pageSizeSyncReady.value) {
    fetchList();
  } else {
    pendingInitialLoad.value = true;
  }
});

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchList();
});
</script>
