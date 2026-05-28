<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpSupplierManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--6">
            <el-input
              v-model="nameQuery"
              :placeholder="$t('field.name')"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="codeQuery"
              :placeholder="$t('field.code')"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="shortNameQuery"
              :placeholder="$t('field.shortName')"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="contactQuery"
              :placeholder="$t('field.contactPerson')"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="phoneQuery"
              :placeholder="$t('field.phone')"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="supplierTypeFilter"
              :placeholder="$t('field.supplierType')"
              class="table-search erp-basic-field--narrow"
              clearable
            >
              <el-option
                v-for="item in supplierTypeOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <el-select
              v-model="businessScopeFilter"
              :placeholder="$t('field.businessScope')"
              class="table-search erp-basic-field--narrow"
              clearable
            >
              <el-option label="供应商" value="SUPPLIER" />
              <el-option label="客户兼供应商" value="CUSTOMER_SUPPLIER" />
            </el-select>
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search erp-basic-field--narrow"
            >
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
              <el-option :label="$t('status.blacklisted')" value="blacklisted" />
            </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button @click="handleReset">{{ $t('action.resetDefault') }}</el-button>
            <el-button type="primary" v-permission="'erp-supplier:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-supplier-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" fixed="left" />
          <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" fixed="left" />
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="160" fixed="left" />
          <ErpDataTableColumn v-if="canShow('supplierTypeId')" :label="$t('field.supplierType')" min-width="140">
            <template #default="{ row }">
              {{ formatSupplierType(row.supplierTypeId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('region')" prop="region" :label="$t('field.region')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('contact')" prop="contact" :label="$t('field.contactPerson')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('phone')" prop="phone" :label="$t('field.phone')" min-width="130" />
          <ErpDataTableColumn v-if="canShow('mobile')" prop="mobile" :label="$t('field.mobile')" min-width="130" />
          <ErpDataTableColumn v-if="canShow('wechat')" prop="wechat" :label="$t('field.wechat')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('purchaser')" prop="purchaser" :label="$t('field.purchaser')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('businessScope')" :label="$t('field.businessScope')" min-width="140">
            <template #default="{ row }">
              {{ formatBusinessScope(row.businessScope) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('counterpartySubjectId')" :label="$t('field.counterpartySubject')" min-width="160">
            <template #default="{ row }">
              {{ formatCounterpartySubject(row.counterpartySubjectId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('email')" prop="email" :label="$t('field.email')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('taxNo')" prop="taxNo" :label="$t('field.taxNo')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('address')" prop="address" :label="$t('field.openingAddress')" min-width="220" />
          <ErpDataTableColumn v-if="canShow('bankAccount')" prop="bankAccount" :label="$t('field.bankAccount')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('status')" :label="$t('field.status')" width="120" column-key="status">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row)" size="small">
                {{ formatStatus(row) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('recentTransactionAt')" prop="recentTransactionAt" :label="$t('field.recentTransactionTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.recentTransactionAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('updatedAt')" prop="updatedAt" :label="$t('field.updatedTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-supplier:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-supplier:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
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

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="700px" @closed="resetForm">
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" :placeholder="$t('placeholder.autoGenerated')" :disabled="!isEditing" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item :label="$t('field.shortName')">
          <el-input v-model="formData.shortName" />
        </el-form-item>
        <el-form-item :label="$t('field.supplierType')">
          <el-select v-model="formData.supplierTypeId" clearable style="width: 100%">
            <el-option
              v-for="item in supplierTypeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.region')">
          <el-input v-model="formData.region" />
        </el-form-item>
        <el-form-item :label="$t('field.contactPerson')">
          <el-input v-model="formData.contact" />
        </el-form-item>
        <el-form-item :label="$t('field.phone')">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item :label="$t('field.mobile')">
          <el-input v-model="formData.mobile" />
        </el-form-item>
        <el-form-item :label="$t('field.contactInfo')">
          <el-input v-model="formData.contactInfo" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="$t('field.wechat')">
          <el-input v-model="formData.wechat" />
        </el-form-item>
        <el-form-item :label="$t('field.purchaser')">
          <el-input v-model="formData.purchaser" />
        </el-form-item>
        <el-form-item :label="$t('field.businessScope')">
          <el-select v-model="formData.businessScope" style="width: 100%">
            <el-option label="供应商" value="SUPPLIER" />
            <el-option label="客户兼供应商" value="CUSTOMER_SUPPLIER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.counterpartySubject')">
          <el-select
            v-model="formData.counterpartySubjectId"
            clearable
            filterable
            style="width: 100%"
            :placeholder="$t('placeholder.selectCounterpartySubject')"
          >
            <el-option
              v-for="item in counterpartySubjectOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.email')">
          <el-input v-model="formData.email" />
        </el-form-item>
        <el-form-item :label="$t('field.openingAddress')">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item :label="$t('field.taxNo')">
          <el-input v-model="formData.taxNo" />
        </el-form-item>
        <el-form-item :label="$t('field.bankName')">
          <el-input v-model="formData.bankName" />
        </el-form-item>
        <el-form-item :label="$t('field.bankAccount')">
          <el-input v-model="formData.bankAccount" />
        </el-form-item>
        <el-form-item :label="$t('field.sourceCreatedBy')">
          <el-input v-model="formData.sourceCreatedBy" />
        </el-form-item>
        <el-form-item :label="$t('field.sourceCreatedAt')">
          <el-input v-model="formData.sourceCreatedAt" disabled :placeholder="$t('placeholder.importedSourceCreatedAt')" />
        </el-form-item>
        <el-form-item :label="$t('field.defaultSettlementMethod')">
          <el-select v-model="formData.defaultSettlementMethodCode" clearable style="width: 100%">
            <el-option
              v-for="item in settlementMethodOptions"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.defaultPaymentMethod')">
          <el-select v-model="formData.defaultPaymentMethodCode" clearable style="width: 100%">
            <el-option
              v-for="item in paymentMethodOptions"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-radio-group v-model="formData.status">
            <el-radio value="enabled">{{ $t('status.active') }}</el-radio>
            <el-radio value="disabled">{{ $t('status.inactive') }}</el-radio>
            <el-radio value="blacklisted">{{ $t('status.blacklisted') }}</el-radio>
          </el-radio-group>
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
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { getCachedEnabledPaymentMethods, getCachedEnabledSettlementMethods } from '@/composables/erpBaseDataCache';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

type SupplierStatus = 'enabled' | 'disabled' | 'blacklisted';

interface ErpSupplier {
  id: number;
  code: string;
  name: string;
  shortName?: string;
  supplierTypeId?: number;
  region?: string;
  contact?: string;
  phone?: string;
  mobile?: string;
  wechat?: string;
  purchaser?: string;
  contactInfo?: string;
  businessScope?: string;
  counterpartySubjectId?: number;
  sourceCreatedBy?: string;
  sourceCreatedAt?: string;
  email?: string;
  address?: string;
  taxNo?: string;
  bankName?: string;
  bankAccount?: string;
  defaultSettlementMethodCode?: string;
  defaultPaymentMethodCode?: string;
  enabled: boolean;
  blacklisted?: boolean;
  recentTransactionAt?: string;
  createdAt?: string;
  updatedAt?: string;
  remark?: string;
}

interface CodeOptionItem {
  id: number;
  code: string;
  name: string;
  isDefault?: boolean;
}

interface CounterpartySubjectOption {
  id: number;
  name: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const nameQuery = ref('');
const codeQuery = ref('');
const shortNameQuery = ref('');
const contactQuery = ref('');
const phoneQuery = ref('');
const supplierTypeFilter = ref<number | undefined>(undefined);
const businessScopeFilter = ref<string>('');
const statusFilter = ref<'all' | SupplierStatus>('all');
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<ErpSupplier[]>([]);
const allTableData = ref<ErpSupplier[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const settlementMethodOptions = ref<CodeOptionItem[]>([]);
const paymentMethodOptions = ref<CodeOptionItem[]>([]);
const supplierTypeOptions = ref<CodeOptionItem[]>([]);
const counterpartySubjectOptions = ref<CounterpartySubjectOption[]>([]);

const defaultColumns = [
  'code',
  'name',
  'supplierTypeId',
  'region',
  'contact',
  'phone',
  'mobile',
  'wechat',
  'purchaser',
  'businessScope',
  'counterpartySubjectId',
  'email',
  'taxNo',
  'address',
  'bankAccount',
  'status',
  'recentTransactionAt',
  'createdAt',
  'updatedAt'
];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-supplier', defaultColumns);

const formData = reactive({
  code: '',
  name: '',
  shortName: '',
  supplierTypeId: undefined as number | undefined,
  region: '',
  contact: '',
  phone: '',
  mobile: '',
  wechat: '',
  purchaser: '',
  contactInfo: '',
  businessScope: 'SUPPLIER',
  counterpartySubjectId: undefined as number | undefined,
  sourceCreatedBy: '',
  sourceCreatedAt: '',
  email: '',
  address: '',
  taxNo: '',
  bankName: '',
  bankAccount: '',
  defaultSettlementMethodCode: '',
  defaultPaymentMethodCode: '',
  status: 'enabled' as SupplierStatus,
  remark: ''
});

const canShow = (key: string) => isVisible(key);

const formatSupplierType = (supplierTypeId?: number) => {
  if (!supplierTypeId) return '-';
  return supplierTypeOptions.value.find(item => item.id === supplierTypeId)?.name || String(supplierTypeId);
};

const formatBusinessScope = (scope?: string) => {
  if (scope === 'CUSTOMER_SUPPLIER') return t('option.customerSupplier');
  if (scope === 'SUPPLIER') return t('option.supplierOnly');
  return scope || '-';
};

const formatCounterpartySubject = (subjectId?: number) => {
  if (!subjectId) return '-';
  return counterpartySubjectOptions.value.find(item => item.id === subjectId)?.name || `#${subjectId}`;
};

const resolveStatus = (row: ErpSupplier): SupplierStatus => {
  if (row.blacklisted) return 'blacklisted';
  return row.enabled ? 'enabled' : 'disabled';
};

const formatStatus = (row: ErpSupplier) => {
  const status = resolveStatus(row);
  if (status === 'blacklisted') return t('status.blacklisted');
  return status === 'enabled' ? t('status.active') : t('status.inactive');
};

const statusTagType = (row: ErpSupplier) => {
  const status = resolveStatus(row);
  if (status === 'enabled') return 'success';
  if (status === 'blacklisted') return 'danger';
  return 'info';
};

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

const syncFormStatus = (row?: ErpSupplier) => {
  if (!row) {
    formData.status = 'enabled';
    return;
  }
  formData.status = resolveStatus(row);
};

const buildPayload = () => ({
  code: formData.code,
  name: formData.name,
  shortName: formData.shortName || undefined,
  supplierTypeId: formData.supplierTypeId,
  region: formData.region || undefined,
  contact: formData.contact || undefined,
  phone: formData.phone || undefined,
  mobile: formData.mobile || undefined,
  wechat: formData.wechat || undefined,
  purchaser: formData.purchaser || undefined,
  contactInfo: formData.contactInfo || undefined,
  businessScope: formData.businessScope || undefined,
  counterpartySubjectId: formData.counterpartySubjectId,
  sourceCreatedBy: formData.sourceCreatedBy || undefined,
  sourceCreatedAt: formData.sourceCreatedAt || undefined,
  email: formData.email || undefined,
  address: formData.address || undefined,
  taxNo: formData.taxNo || undefined,
  bankName: formData.bankName || undefined,
  bankAccount: formData.bankAccount || undefined,
  defaultSettlementMethodCode: formData.defaultSettlementMethodCode || undefined,
  defaultPaymentMethodCode: formData.defaultPaymentMethodCode || undefined,
  enabled: formData.status === 'enabled',
  blacklisted: formData.status === 'blacklisted',
  remark: formData.remark || undefined
});

const applyDefaultMethods = () => {
  if (!formData.defaultSettlementMethodCode && settlementMethodOptions.value.length) {
    const defaultItem = settlementMethodOptions.value.find(item => item.isDefault) ?? settlementMethodOptions.value[0];
    if (defaultItem) {
      formData.defaultSettlementMethodCode = defaultItem.code;
    }
  }
  if (!formData.defaultPaymentMethodCode && paymentMethodOptions.value.length) {
    const defaultItem = paymentMethodOptions.value.find(item => item.isDefault) ?? paymentMethodOptions.value[0];
    if (defaultItem) {
      formData.defaultPaymentMethodCode = defaultItem.code;
    }
  }
};

const fetchSettlementMethods = async () => {
  try {
    settlementMethodOptions.value = await getCachedEnabledSettlementMethods(tenantCacheKey.value);
    if (showModal.value && !isEditing.value) {
      applyDefaultMethods();
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchPaymentMethods = async () => {
  try {
    paymentMethodOptions.value = await getCachedEnabledPaymentMethods(tenantCacheKey.value);
    if (showModal.value && !isEditing.value) {
      applyDefaultMethods();
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchSupplierTypes = async () => {
  try {
    const res: any = await request.get('/erp/supplier-types');
    if (res.data.code === 200) {
      supplierTypeOptions.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchCounterpartySubjects = async () => {
  try {
    const res: any = await request.get('/erp/counterparty-subjects');
    if (res.data.code === 200) {
      counterpartySubjectOptions.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchNextSupplierCode = async () => {
  try {
    const res: any = await request.get('/erp/suppliers/next-code');
    if (res.data.code === 200) {
      formData.code = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const applySearch = () => {
  let filtered = allTableData.value.slice();
  if (statusFilter.value !== 'all') filtered = filtered.filter(row => resolveStatus(row) === statusFilter.value);
  if (supplierTypeFilter.value) filtered = filtered.filter(row => row.supplierTypeId === supplierTypeFilter.value);
  if (businessScopeFilter.value) filtered = filtered.filter(row => (row.businessScope || 'SUPPLIER') === businessScopeFilter.value);
  filtered = filterByFuzzyKeyword(filtered, nameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, codeQuery.value, row => [row.code]);
  filtered = filterByFuzzyKeyword(filtered, shortNameQuery.value, row => [row.shortName]);
  filtered = filterByFuzzyKeyword(filtered, contactQuery.value, row => [row.contact]);
  filtered = filterByFuzzyKeyword(filtered, phoneQuery.value, row => [row.phone, row.mobile]);
  total.value = filtered.length;
  const start = (page.value - 1) * size.value;
  tableData.value = filtered.slice(start, start + size.value);
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/erp/suppliers');
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
  codeQuery.value = '';
  shortNameQuery.value = '';
  contactQuery.value = '';
  phoneQuery.value = '';
  supplierTypeFilter.value = undefined;
  businessScopeFilter.value = '';
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
  applyDefaultMethods();
  showModal.value = true;
  fetchNextSupplierCode();
};

const openEditModal = (row: ErpSupplier) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.shortName = row.shortName || '';
  formData.supplierTypeId = row.supplierTypeId;
  formData.region = row.region || '';
  formData.contact = row.contact || '';
  formData.phone = row.phone || '';
  formData.mobile = row.mobile || '';
  formData.wechat = row.wechat || '';
  formData.purchaser = row.purchaser || '';
  formData.contactInfo = row.contactInfo || '';
  formData.businessScope = row.businessScope || 'SUPPLIER';
  formData.counterpartySubjectId = row.counterpartySubjectId;
  formData.sourceCreatedBy = row.sourceCreatedBy || '';
  formData.sourceCreatedAt = row.sourceCreatedAt || '';
  formData.email = row.email || '';
  formData.address = row.address || '';
  formData.taxNo = row.taxNo || '';
  formData.bankName = row.bankName || '';
  formData.bankAccount = row.bankAccount || '';
  formData.defaultSettlementMethodCode = row.defaultSettlementMethodCode || '';
  formData.defaultPaymentMethodCode = row.defaultPaymentMethodCode || '';
  syncFormStatus(row);
  formData.remark = row.remark || '';
  showModal.value = true;
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.shortName = '';
  formData.supplierTypeId = undefined;
  formData.region = '';
  formData.contact = '';
  formData.phone = '';
  formData.mobile = '';
  formData.wechat = '';
  formData.purchaser = '';
  formData.contactInfo = '';
  formData.businessScope = 'SUPPLIER';
  formData.counterpartySubjectId = undefined;
  formData.sourceCreatedBy = '';
  formData.sourceCreatedAt = '';
  formData.email = '';
  formData.address = '';
  formData.taxNo = '';
  formData.bankName = '';
  formData.bankAccount = '';
  formData.defaultSettlementMethodCode = '';
  formData.defaultPaymentMethodCode = '';
  formData.status = 'enabled';
  formData.remark = '';
};

const saveData = async () => {
  if (!formData.code || !formData.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = buildPayload();
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/suppliers/${currentId.value}`, payload)
      : await request.post('/erp/suppliers', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpSupplier) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `确认删除供应商“${row.name}”吗？若该供应商已存在采购单、采购退货单、付款单或应付单等关联业务，将无法删除。`,
      t('action.delete'),
      {
        inputPlaceholder: t('action.deleteReason'),
        inputPattern: /^(?=.*\S).{2,500}$/,
        inputErrorMessage: t('message.deleteReasonMin'),
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel'),
        type: 'warning',
        closeOnClickModal: false
      }
    );

    await request.delete(`/erp/suppliers/${row.id}`, {
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
  fetchSettlementMethods();
  fetchPaymentMethods();
  fetchSupplierTypes();
  fetchCounterpartySubjects();
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
