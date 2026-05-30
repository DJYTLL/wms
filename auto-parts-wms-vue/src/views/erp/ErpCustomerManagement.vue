<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCustomerManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions">
          <div class="erp-basic-filters erp-basic-filters--6">
            <el-input
              v-model="nameQuery"
              placeholder="名称"
              class="table-search erp-basic-field--narrow"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="codeQuery"
              placeholder="编码"
              class="table-search erp-basic-field--narrow"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="contactQuery"
              :placeholder="$t('field.contactPerson')"
              class="table-search erp-basic-field--narrow"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="phoneQuery"
              :placeholder="$t('field.contactInfo')"
              class="table-search erp-basic-field--narrow"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-select v-model="categoryFilter" :placeholder="$t('field.customerCategory')" class="table-search erp-basic-field--narrow" clearable>
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button @click="handleReset">{{ $t('action.resetDefault') }}</el-button>
            <el-button v-permission="'erp-customer:import'" @click="openCustomerImportDialog">{{ $t('action.import') }}</el-button>
            <el-button v-permission="'erp-customer:import'" @click="openCustomerImportHistoryDrawer">导入结果</el-button>
            <el-button type="primary" v-permission="'erp-customer:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-customer-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('category')" :label="$t('field.customerCategory')" min-width="140" column-key="category">
            <template #default="{ row }">
              {{ getCategoryName(row.categoryId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('contact')" prop="contact" :label="$t('field.contactPerson')" min-width="120" />
          <ErpDataTableColumn v-if="canShowContactInfo" :label="$t('field.contactInfo')" min-width="220">
            <template #default="{ row }">
              {{ formatCustomerContactInfo(row) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('counterpartySubjectId')" :label="$t('field.counterpartySubject')" min-width="160">
            <template #default="{ row }">
              {{ getCounterpartySubjectName(row.counterpartySubjectId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('email')" prop="email" :label="$t('field.email')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-customer:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-customer:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <ErpCustomerEditDialog
      v-model="showModal"
      :mode="customerDialogMode"
      :initial-value="selectedCustomer"
      :category-options="categoryOptions"
      :settlement-method-options="settlementMethodOptions"
      :receipt-method-options="receiptMethodOptions"
      :delivery-method-options="deliveryMethodOptions"
      :counterparty-subject-options="counterpartySubjectOptions"
      :next-code="nextCustomerCode"
      :submitting="customerDialogSaving"
      @submit="handleCustomerDialogSubmit"
      @closed="handleCustomerDialogClosed"
    />

    <el-dialog v-model="showCustomerImportDialog" title="导入客户档案" width="720px">
      <el-form label-position="top">
        <el-form-item label="来源名称">
          <el-input v-model="customerImportSourceName" placeholder="例如：2026-05-客户档案表" />
        </el-form-item>
        <el-form-item label="Excel 文件">
          <input
            ref="customerImportInputRef"
            type="file"
            accept=".xls,.xlsx"
            @change="handleCustomerImportFile"
          />
          <div style="margin-top: 8px; color: var(--el-text-color-secondary);">
            {{ customerImportFile?.name || '请选择 .xls 或 .xlsx 文件' }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCustomerImportDialog = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" :loading="customerImportSubmitting" @click="submitCustomerImport">{{ $t('action.import') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="showCustomerImportHistoryDrawer" title="客户导入结果" size="70%">
      <div class="supplier-import-drawer">
        <div class="supplier-import-drawer__toolbar">
          <el-button v-permission="'erp-customer:import'" @click="loadCustomerImportBatches">刷新批次</el-button>
        </div>
        <el-table :data="customerImportBatches" v-loading="customerImportHistoryLoading" style="width: 100%">
          <el-table-column prop="batchNo" label="批次号" min-width="170" />
          <el-table-column prop="sourceName" label="来源" min-width="150" />
          <el-table-column prop="totalCount" label="总行数" width="90" />
          <el-table-column prop="successCount" label="成功" width="90" />
          <el-table-column prop="failedCount" label="失败" width="90" />
          <el-table-column prop="status" label="状态" width="130" />
          <el-table-column prop="createdAt" label="导入时间" min-width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewCustomerImportBatchItems(row)">查看明细</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedCustomerImportBatch" class="supplier-import-drawer__detail">
          <div class="supplier-import-drawer__detail-title">
            当前批次：{{ selectedCustomerImportBatch.batchNo }}
          </div>
          <el-table :data="customerImportBatchItems" v-loading="customerImportItemsLoading" style="width: 100%">
            <el-table-column prop="rowNo" label="行号" width="80" />
            <el-table-column prop="sourceCode" label="编码" min-width="120" />
            <el-table-column prop="sourceName" label="名称" min-width="180" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="warningMessage" label="提示" min-width="180" />
            <el-table-column prop="errorMessage" label="异常原因" min-width="180" />
            <el-table-column prop="suggestion" label="建议处理" min-width="180" />
            <el-table-column prop="matchedStrategy" label="识别策略" width="120" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import ErpCustomerEditDialog from '@/components/ErpCustomerEditDialog.vue';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { getCachedCustomerCategories, getCachedEnabledDeliveryMethods, getCachedEnabledReceiptMethods, getCachedEnabledSettlementMethods } from '@/composables/erpBaseDataCache';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

interface ErpCustomer {
  id: number;
  code: string;
  name: string;
  categoryId?: number;
  defaultSettlementMethodCode?: string;
  defaultReceiptMethodCode?: string;
  deliveryMethodCode?: string;
  contact?: string;
  phone?: string;
  mobile?: string;
  email?: string;
  address?: string;
  contacts?: string | ContactItem[];
  counterpartySubjectId?: number;
  enabled: boolean;
  remark?: string;
}

interface OptionItem {
  id: number;
  name: string;
  isDefault?: boolean;
}

interface CodeOptionItem {
  id: number;
  code: string;
  name: string;
  isDefault?: boolean;
}

interface ContactItem {
  name?: string;
  phone?: string;
  mobile?: string;
  email?: string;
  remark?: string;
  isPrimary?: boolean;
}

interface CustomerDialogSubmitPayload {
  code: string;
  name: string;
  categoryId: number;
  defaultSettlementMethodCode?: string;
  defaultReceiptMethodCode?: string;
  deliveryMethodCode?: string;
  contact?: string;
  phone?: string;
  mobile?: string;
  email?: string;
  address?: string;
  contacts?: string;
  counterpartySubjectId?: number;
  enabled: boolean;
  remark?: string;
  contactInfo?: string;
}

interface CounterpartySubjectOption {
  id: number;
  name: string;
}

interface CounterpartyPendingDoc {
  docType: string;
  docId: number;
  orderNo: string;
  status: string;
  routeKey: string;
}

interface CounterpartyRebindCheck {
  allowed: boolean;
  blockingReasons: string[];
  pendingDocs: CounterpartyPendingDoc[];
}

interface CustomerImportResult {
  batchId: number;
  batchNo: string;
  status: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
}

interface CustomerImportBatchSummary {
  id: number;
  batchNo: string;
  sourceName?: string;
  importMode?: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  status: string;
  summary?: string;
  createdBy?: string;
  createdAt?: string;
}

interface CustomerImportItemView {
  id: number;
  rowNo: number;
  sourceCode?: string;
  sourceName?: string;
  status: string;
  warningMessage?: string;
  errorMessage?: string;
  suggestion?: string;
  matchedStrategy?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const nameQuery = ref('');
const codeQuery = ref('');
const contactQuery = ref('');
const phoneQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const categoryFilter = ref<number | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<ErpCustomer[]>([]);
const allTableData = ref<ErpCustomer[]>([]);
const showModal = ref(false);
const customerDialogMode = ref<'create' | 'edit'>('create');
const customerDialogSaving = ref(false);
const currentId = ref<number | null>(null);
const nextCustomerCode = ref('');
const selectedCustomer = ref<ErpCustomer | null>(null);
const originalCounterpartySubjectId = ref<number | null>(null);

const categoryOptions = ref<OptionItem[]>([]);
const settlementMethodOptions = ref<CodeOptionItem[]>([]);
const receiptMethodOptions = ref<CodeOptionItem[]>([]);
const deliveryMethodOptions = ref<CodeOptionItem[]>([]);
const counterpartySubjectOptions = ref<CounterpartySubjectOption[]>([]);
const customerImportInputRef = ref<HTMLInputElement | null>(null);
const showCustomerImportDialog = ref(false);
const showCustomerImportHistoryDrawer = ref(false);
const customerImportSourceName = ref('');
const customerImportFile = ref<File | null>(null);
const customerImportSubmitting = ref(false);
const customerImportHistoryLoading = ref(false);
const customerImportItemsLoading = ref(false);
const customerImportBatches = ref<CustomerImportBatchSummary[]>([]);
const selectedCustomerImportBatch = ref<CustomerImportBatchSummary | null>(null);
const customerImportBatchItems = ref<CustomerImportItemView[]>([]);
const activeCustomerImportBatchId = ref<number | null>(null);
const customerImportPollingTimer = ref<number | null>(null);

const defaultColumns = ['code', 'name', 'category', 'contact', 'contactInfo', 'phone', 'mobile', 'counterpartySubjectId', 'email', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-customer', defaultColumns);

const canShow = (key: string) => isVisible(key);
const canShowContactInfo = computed(() => (
  isVisible('contactInfo') || isVisible('phone') || isVisible('mobile')
));

const getCategoryName = (id?: number) => categoryOptions.value.find(item => item.id === id)?.name || '-';
const getCounterpartySubjectName = (id?: number) => counterpartySubjectOptions.value.find(item => item.id === id)?.name || '-';

const extractErrorMessage = (error: unknown) => {
  if (!error) return '';
  if (typeof error === 'string') return error;
  if (error instanceof Error) return error.message || '';
  const maybeMessage = (error as any)?.response?.data?.message || (error as any)?.response?.data?.errorMessage;
  return typeof maybeMessage === 'string' ? maybeMessage : '';
};

const shouldShowRebindGuidance = (message: string) => (
  message.includes('不能改绑往来主体') || message.includes('未完成销售单')
  || message.includes('未完成销售退货单') || message.includes('未完成收款单')
  || message.includes('未完成应收')
);

const showRebindBlockedMessage = async (message: string) => {
  await ElMessageBox.alert(
    [
      `<div>${message}</div>`,
      '<div style="margin-top: 8px;">请先处理该客户名下未完成的销售、退货、收款或应收业务，再重新改绑往来主体。</div>'
    ].join(''),
    '客户改绑受限',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了'
    }
  );
};

const getDocTypeLabel = (docType?: string) => {
  switch (docType) {
    case 'SALE_ORDER':
      return '未完成销售单';
    case 'SALE_RETURN':
      return '未完成销售退货单';
    case 'RECEIPT':
      return '未完成收款单';
    case 'ACCOUNTS_RECEIVABLE':
      return '未完成应收单';
    default:
      return docType || '未完成业务';
  }
};

const buildRebindCheckHtml = (check: CounterpartyRebindCheck) => {
  const sections: string[] = [];
  if (check.blockingReasons?.length) {
    sections.push([
      '<div style="margin-bottom: 12px;">',
      '<div style="font-weight: 600; margin-bottom: 6px;">阻塞原因</div>',
      ...check.blockingReasons.map(reason => `<div style="margin-top: 4px;">${reason}</div>`),
      '</div>'
    ].join(''));
  }

  if (check.pendingDocs?.length) {
    const groupedDocs = check.pendingDocs.reduce<Record<string, CounterpartyPendingDoc[]>>((acc, doc) => {
      const key = doc.docType || 'UNKNOWN';
      if (!acc[key]) acc[key] = [];
      acc[key].push(doc);
      return acc;
    }, {});
    sections.push(Object.entries(groupedDocs).map(([docType, docs]) => [
      '<div style="margin-bottom: 12px;">',
      `<div style="font-weight: 600; margin-bottom: 6px;">${getDocTypeLabel(docType)}</div>`,
      ...docs.map(doc => `<div style="margin-top: 4px; padding-left: 8px;">${doc.orderNo || '-'}（${doc.status || '-'}）</div>`),
      '</div>'
    ].join('')).join(''));
  }

  return sections.join('') || '<div>当前没有阻塞原因</div>';
};

const ensureCustomerRebindAllowed = async (targetSubjectId?: number) => {
  if (customerDialogMode.value !== 'edit' || !currentId.value) return true;
  if (originalCounterpartySubjectId.value === (targetSubjectId || null)) return true;

  const res: any = await request.get(`/erp/customers/${currentId.value}/counterparty-subject-check`, {
    params: {
      targetSubjectId: targetSubjectId || undefined
    }
  });
  const check = res.data?.data as CounterpartyRebindCheck | undefined;
  if (check?.allowed !== false) {
    return true;
  }

  await ElMessageBox.alert(
    buildRebindCheckHtml(check),
    '客户改绑受限',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了'
    }
  );
  return false;
};

const fetchCategories = async () => {
  try {
    categoryOptions.value = await getCachedCustomerCategories(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchSettlementMethods = async () => {
  try {
    settlementMethodOptions.value = await getCachedEnabledSettlementMethods(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchReceiptMethods = async () => {
  try {
    receiptMethodOptions.value = await getCachedEnabledReceiptMethods(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchDeliveryMethods = async () => {
  try {
    deliveryMethodOptions.value = await getCachedEnabledDeliveryMethods(tenantCacheKey.value);
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

const applySearch = () => {
  let filtered = allTableData.value.slice();
  if (statusFilter.value !== 'all') filtered = filtered.filter(row => row.enabled === (statusFilter.value === 'enabled'));
  if (categoryFilter.value) filtered = filtered.filter(row => row.categoryId === categoryFilter.value);
  filtered = filterByFuzzyKeyword(filtered, nameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, codeQuery.value, row => [row.code]);
  filtered = filterByFuzzyKeyword(filtered, contactQuery.value, row => [
    row.contact,
    ...getCustomerContacts(row).map(item => item.name)
  ]);
  filtered = filterByFuzzyKeyword(filtered, phoneQuery.value, row => [
    formatCustomerContactInfo(row),
    ...getCustomerContactTokens(row)
  ]);
  total.value = filtered.length;
  const start = (page.value - 1) * size.value;
  tableData.value = filtered.slice(start, start + size.value);
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/erp/customers');
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
  contactQuery.value = '';
  phoneQuery.value = '';
  statusFilter.value = 'all';
  categoryFilter.value = null;
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
  customerDialogMode.value = 'create';
  currentId.value = null;
  selectedCustomer.value = null;
  originalCounterpartySubjectId.value = null;
  showModal.value = true;
  fetchNextCustomerCode();
};

const openEditModal = (row: ErpCustomer) => {
  customerDialogMode.value = 'edit';
  currentId.value = row.id;
  originalCounterpartySubjectId.value = row.counterpartySubjectId || null;
  selectedCustomer.value = { ...row };
  showModal.value = true;
};

const handleCustomerDialogClosed = () => {
  selectedCustomer.value = null;
  originalCounterpartySubjectId.value = null;
};

const parseContacts = (raw?: unknown) => {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw as ContactItem[];
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
};

const getCustomerContacts = (row: ErpCustomer) => parseContacts(row.contacts);

const uniqueContactTokens = (values: Array<string | undefined>) => {
  const seen = new Set<string>();
  return values
    .map(value => value?.trim())
    .filter((value): value is string => Boolean(value))
    .filter((value) => {
      if (seen.has(value)) {
        return false;
      }
      seen.add(value);
      return true;
    });
};

const isMobileLike = (value?: string) => Boolean(value?.trim().match(/^1\d{10}$/));

const isTruncatedMobilePrefix = (value: string, values: string[]) => {
  const normalized = value.trim();
  if (!/^\d{7,8}$/.test(normalized)) {
    return false;
  }
  return values.some(item => isMobileLike(item) && item.trim().startsWith(normalized));
};

const normalizeCustomerContactTokens = (values: Array<string | undefined>) => {
  const uniqueValues = uniqueContactTokens(values);
  return uniqueValues.filter(value => !isTruncatedMobilePrefix(value, uniqueValues));
};

const getCustomerContactTokens = (row: ErpCustomer) => normalizeCustomerContactTokens([
  row.phone,
  row.mobile,
  ...getCustomerContacts(row).flatMap(item => [item.phone, item.mobile])
]);

const formatCustomerContactInfo = (row: ErpCustomer) => {
  const tokens = getCustomerContactTokens(row);
  return tokens.length ? tokens.join(' / ') : '-';
};

const fetchNextCustomerCode = async () => {
  try {
    const res: any = await request.get('/erp/customers/next-code');
    if (res.data.code === 200) {
      nextCustomerCode.value = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleCustomerDialogSubmit = async (payload: CustomerDialogSubmitPayload) => {
  customerDialogSaving.value = true;
  try {
    const allowed = await ensureCustomerRebindAllowed(payload.counterpartySubjectId);
    if (!allowed) return;
    const normalizedPayload = {
      ...payload,
      counterpartySubjectId: payload.counterpartySubjectId || undefined
    };
    const res: any = customerDialogMode.value === 'edit' && currentId.value
      ? await request.put(`/erp/customers/${currentId.value}`, normalizedPayload)
      : await request.post('/erp/customers', normalizedPayload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      handleCustomerDialogClosed();
      fetchList();
    }
  } catch (error) {
    const message = extractErrorMessage(error);
    if (shouldShowRebindGuidance(message)) {
      await showRebindBlockedMessage(message);
      return;
    }
    notifyError(error);
  } finally {
    customerDialogSaving.value = false;
  }
};

const handleDelete = async (row: ErpCustomer) => {
  try {
    await request.delete(`/erp/customers/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const resetCustomerImportForm = () => {
  customerImportSourceName.value = '';
  customerImportFile.value = null;
  if (customerImportInputRef.value) {
    customerImportInputRef.value.value = '';
  }
};

const openCustomerImportDialog = () => {
  resetCustomerImportForm();
  showCustomerImportDialog.value = true;
};

const openCustomerImportHistoryDrawer = async () => {
  showCustomerImportHistoryDrawer.value = true;
  await loadCustomerImportBatches();
};

const handleCustomerImportFile = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  customerImportFile.value = input.files?.[0] || null;
};

const loadCustomerImportBatches = async () => {
  customerImportHistoryLoading.value = true;
  try {
    const res: any = await request.get('/erp/customers/import-batches');
    if (res.data.code === 200) {
      customerImportBatches.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    customerImportHistoryLoading.value = false;
  }
};

const viewCustomerImportBatchItems = async (batch: CustomerImportBatchSummary) => {
  selectedCustomerImportBatch.value = batch;
  customerImportItemsLoading.value = true;
  try {
    const res: any = await request.get(`/erp/customers/import-batches/${batch.id}/items`);
    if (res.data.code === 200) {
      customerImportBatchItems.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    customerImportItemsLoading.value = false;
  }
};

const stopCustomerImportPolling = () => {
  if (customerImportPollingTimer.value != null) {
    window.clearTimeout(customerImportPollingTimer.value);
    customerImportPollingTimer.value = null;
  }
  activeCustomerImportBatchId.value = null;
};

const pollCustomerImportBatch = async (batchId: number) => {
  await loadCustomerImportBatches();
  const batch = customerImportBatches.value.find(item => item.id === batchId) || null;
  if (!batch) {
    stopCustomerImportPolling();
    return;
  }
  selectedCustomerImportBatch.value = batch;
  await viewCustomerImportBatchItems(batch);
  if (batch.status === 'PROCESSING') {
    customerImportPollingTimer.value = window.setTimeout(() => {
      void pollCustomerImportBatch(batchId);
    }, 1500);
    return;
  }
  stopCustomerImportPolling();
  notifySuccess(batch.summary || `导入完成：成功 ${batch.successCount} 行，失败 ${batch.failedCount} 行`);
  fetchList();
};

const startCustomerImportPolling = (batchId: number) => {
  stopCustomerImportPolling();
  activeCustomerImportBatchId.value = batchId;
  customerImportPollingTimer.value = window.setTimeout(() => {
    void pollCustomerImportBatch(batchId);
  }, 1000);
};

const submitCustomerImport = async () => {
  if (!customerImportFile.value) {
    notifyError('请先选择要导入的 Excel 文件');
    return;
  }
  customerImportSubmitting.value = true;
  try {
    const formData = new FormData();
    formData.append('file', customerImportFile.value);
    const trimmedSourceName = customerImportSourceName.value.trim();
    if (trimmedSourceName) {
      formData.append('sourceName', trimmedSourceName);
    }
    const res: any = await request.post('/erp/customers/import', formData);
    if (res.data.code === 200) {
      const result = res.data.data as CustomerImportResult;
      notifySuccess(`导入任务已创建：批次 ${result.batchNo}`);
      showCustomerImportDialog.value = false;
      await openCustomerImportHistoryDrawer();
      resetCustomerImportForm();
      startCustomerImportPolling(result.batchId);
    }
  } catch (error) {
    notifyError(error);
  } finally {
    customerImportSubmitting.value = false;
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
  fetchCategories();
  fetchSettlementMethods();
  fetchReceiptMethods();
  fetchDeliveryMethods();
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
