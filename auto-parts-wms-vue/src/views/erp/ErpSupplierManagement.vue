<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpSupplierManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions">
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
              :placeholder="$t('field.contactInfo')"
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
            <el-button v-permission="'erp-supplier:import'" @click="openImportDialog">{{ $t('action.import') }}</el-button>
            <el-button v-permission="'erp-supplier:import'" @click="openImportHistoryDrawer">{{ supplierImportHistoryButtonText }}</el-button>
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
          <ErpDataTableColumn v-if="canShowContactInfo" prop="contactInfo" :label="$t('field.contactInfo')" min-width="220" />
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

    <ErpSupplierEditDialog
      v-if="showModal"
      v-model="showModal"
      :mode="supplierDialogMode"
      :initial-value="selectedSupplier"
      :supplier-type-options="supplierTypeOptions"
      :counterparty-subject-options="counterpartySubjectOptions"
      :settlement-method-options="settlementMethodOptions"
      :payment-method-options="paymentMethodOptions"
      :next-code="nextSupplierCode"
      :submitting="supplierDialogSaving"
      @submit="handleSupplierDialogSubmit"
      @closed="handleDialogClosed"
    />

    <el-dialog v-if="showImportDialog" v-model="showImportDialog" title="导入供应商历史表" width="960px">
      <el-form label-position="top">
        <el-form-item label="来源名称">
          <el-input v-model="importSourceName" placeholder="例如：2026-05-供应商历史表" />
        </el-form-item>
        <el-form-item label="Excel 文件">
          <input
            ref="importFileInputRef"
            type="file"
            accept=".xls,.xlsx"
            @change="handleImportFileChange"
          />
          <div style="margin-top: 8px; color: var(--el-text-color-secondary);">
            {{ importFile?.name || '请选择 .xls 或 .xlsx 文件' }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showImportDialog = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" :loading="importSubmitting" @click="submitSupplierImport">{{ $t('action.import') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-if="showImportHistoryDrawer" v-model="showImportHistoryDrawer" title="供应商导入结果" size="70%">
      <div class="supplier-import-drawer">
        <div class="supplier-import-drawer__toolbar">
          <el-button v-permission="'erp-supplier:import'" @click="loadImportBatches">刷新批次</el-button>
        </div>
        <el-table :data="importBatches" v-loading="importHistoryLoading" style="width: 100%">
          <el-table-column prop="batchNo" label="批次号" min-width="170" />
          <el-table-column prop="sourceName" label="来源" min-width="150" />
          <el-table-column prop="totalCount" label="总行数" width="90" />
          <el-table-column prop="successCount" label="成功" width="90" />
          <el-table-column prop="failedCount" label="失败" width="90" />
          <el-table-column prop="uncategorizedCount" label="未分类" width="90" />
          <el-table-column prop="settlementUnmatchedCount" label="结算未匹配" width="110" />
          <el-table-column prop="pendingSubjectMergeCount" label="待归并主体" width="110" />
          <el-table-column prop="createdAt" label="导入时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewImportBatchItems(row)">查看明细</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedImportBatch" class="supplier-import-drawer__detail">
          <div class="supplier-import-drawer__detail-title">
            当前批次：{{ selectedImportBatch.batchNo }}
          </div>
          <el-table :data="importBatchItems" v-loading="importItemsLoading" style="width: 100%">
            <el-table-column prop="rowNo" label="行号" width="80" />
            <el-table-column prop="sourceCode" label="编码" min-width="120" />
            <el-table-column prop="sourceName" label="名称" min-width="180" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column prop="warningMessage" label="提示" min-width="180" />
            <el-table-column prop="errorMessage" label="异常原因" min-width="180" />
            <el-table-column prop="suggestion" label="建议处理" min-width="180" />
            <el-table-column prop="matchedStrategy" label="识别策略" width="110" />
            <el-table-column prop="enterpriseMatch" label="企业匹配" min-width="140" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated, onBeforeUnmount, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import ErpSupplierEditDialog from '@/components/ErpSupplierEditDialog.vue';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { getCachedEnabledPaymentMethods, getCachedEnabledSettlementMethods, invalidateErpBaseDataResourceCache } from '@/composables/erpBaseDataCache';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';
import { waitForErpFirstPaint } from './erpFirstPaint';

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
  contacts?: SupplierContactItem[] | string;
  enabled: boolean;
  blacklisted?: boolean;
  recentTransactionAt?: string;
  createdAt?: string;
  updatedAt?: string;
  remark?: string;
}

interface SupplierContactItem {
  name?: string;
  phone?: string;
  mobile?: string;
  wechat?: string;
  email?: string;
  remark?: string;
  isPrimary?: boolean;
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

interface SupplierImportResultItem {
  rowNo: number;
  code?: string;
  name?: string;
  status: string;
  errorField?: string;
  errorMessage?: string;
  suggestion?: string;
  warningMessage?: string;
  matchedStrategy?: string;
}

interface SupplierImportResult {
  batchId: number;
  batchNo: string;
  status: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  items?: SupplierImportResultItem[];
}

interface SupplierImportBatchSummary {
  id: number;
  batchNo: string;
  sourceName?: string;
  importMode?: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  uncategorizedCount: number;
  settlementUnmatchedCount: number;
  pendingSubjectMergeCount: number;
  status: string;
  summary?: string;
  createdBy?: string;
  createdAt?: string;
}

interface SupplierImportItemView {
  id: number;
  rowNo: number;
  sourceCode?: string;
  sourceName?: string;
  matchedSupplierId?: number;
  supplierTypeName?: string;
  settlementMethodName?: string;
  enterpriseMatch?: string;
  priceLevel?: string;
  status: string;
  errorField?: string;
  errorMessage?: string;
  suggestion?: string;
  warningMessage?: string;
  matchedStrategy?: string;
  createdAt?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess } = useApiError();
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
const firstPaintReady = ref(false);
const tableData = ref<ErpSupplier[]>([]);
const allTableData = ref<ErpSupplier[]>([]);
const showModal = ref(false);
const supplierDialogMode = ref<'create' | 'edit'>('create');
const currentId = ref<number | null>(null);
const selectedSupplier = ref<ErpSupplier | null>(null);
const nextSupplierCode = ref('');
const supplierDialogSaving = ref(false);
const originalCounterpartySubjectId = ref<number | undefined>(undefined);
const settlementMethodOptions = ref<CodeOptionItem[]>([]);
const paymentMethodOptions = ref<CodeOptionItem[]>([]);
const supplierTypeOptions = ref<CodeOptionItem[]>([]);
const counterpartySubjectOptions = ref<CounterpartySubjectOption[]>([]);
const showImportDialog = ref(false);
const importSubmitting = ref(false);
const importSourceName = ref('');
const importFileInputRef = ref<HTMLInputElement | null>(null);
const importFile = ref<File | null>(null);
const showImportHistoryDrawer = ref(false);
const importHistoryLoading = ref(false);
const importItemsLoading = ref(false);
const importBatches = ref<SupplierImportBatchSummary[]>([]);
const importBatchItems = ref<SupplierImportItemView[]>([]);
const selectedImportBatch = ref<SupplierImportBatchSummary | null>(null);
const activeImportBatchId = ref<number | null>(null);
const importPollingTimer = ref<number | null>(null);

const defaultColumns = [
  'code',
  'name',
  'supplierTypeId',
  'region',
  'contact',
  'contactInfo',
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

const canShow = (key: string) => isVisible(key);
const canShowContactInfo = computed(() => (
  isVisible('contactInfo') || isVisible('phone') || isVisible('mobile')
));
const supplierImportHistoryButtonText = computed(() => importBatches.value.length ? '导入结果' : '导入历史');

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

const extractErrorMessage = (error: unknown) => {
  if (!error) return '';
  if (typeof error === 'string') return error;
  if (error instanceof Error) return error.message || '';
  const maybeMessage = (error as any)?.response?.data?.message || (error as any)?.response?.data?.errorMessage;
  return typeof maybeMessage === 'string' ? maybeMessage : '';
};

const shouldShowRebindGuidance = (message: string) => (
  message.includes('不能改绑往来主体') || message.includes('未完成采购单')
  || message.includes('未完成采购退货单') || message.includes('未完成付款单')
  || message.includes('未完成应付')
);

const showRebindBlockedMessage = async (message: string) => {
  await ElMessageBox.alert(
    [
      `<div>${message}</div>`,
      '<div style="margin-top: 8px;">请先处理该供应商名下未完成的采购、退货、付款或应付业务，再重新改绑往来主体。</div>'
    ].join(''),
    '供应商改绑受限',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了'
    }
  );
};

const getDocTypeLabel = (docType?: string) => {
  switch (docType) {
    case 'PURCHASE_ORDER':
      return '未完成采购单';
    case 'PURCHASE_RETURN':
      return '未完成采购退货单';
    case 'PAYMENT':
      return '未完成付款单';
    case 'ACCOUNTS_PAYABLE':
      return '未完成应付单';
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

const parseSupplierContacts = (raw?: unknown): SupplierContactItem[] => {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw as SupplierContactItem[];
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed as SupplierContactItem[] : [];
    } catch {
      return [];
    }
  }
  return [];
};

const ensureSupplierRebindAllowed = async (targetSubjectId?: number) => {
  if (supplierDialogMode.value !== 'edit' || !currentId.value) return true;
  if (originalCounterpartySubjectId.value === targetSubjectId) return true;

  const res: any = await request.get(`/erp/suppliers/${currentId.value}/counterparty-subject-check`, {
    params: {
      targetSubjectId
    }
  });
  const check = res.data?.data as CounterpartyRebindCheck | undefined;
  if (check?.allowed !== false) {
    return true;
  }

  await ElMessageBox.alert(
    buildRebindCheckHtml(check),
    '供应商改绑受限',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了'
    }
  );
  return false;
};

const fetchSettlementMethods = async () => {
  try {
    settlementMethodOptions.value = await getCachedEnabledSettlementMethods(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchPaymentMethods = async () => {
  try {
    paymentMethodOptions.value = await getCachedEnabledPaymentMethods(tenantCacheKey.value);
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
      nextSupplierCode.value = res.data.data || '';
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
  filtered = filterByFuzzyKeyword(filtered, contactQuery.value, row => [
    row.contact,
    row.contactInfo,
    ...parseSupplierContacts(row.contacts).map(item => item.name)
  ]);
  filtered = filterByFuzzyKeyword(filtered, phoneQuery.value, row => [
    row.contactInfo,
    row.phone,
    row.mobile,
    row.wechat,
    ...parseSupplierContacts(row.contacts).flatMap(item => [item.phone, item.mobile, item.wechat])
  ]);
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
  supplierDialogMode.value = 'create';
  currentId.value = null;
  selectedSupplier.value = null;
  originalCounterpartySubjectId.value = undefined;
  nextSupplierCode.value = '';
  showModal.value = true;
  fetchNextSupplierCode();
};

const openEditModal = (row: ErpSupplier) => {
  supplierDialogMode.value = 'edit';
  currentId.value = row.id;
  selectedSupplier.value = { ...row };
  originalCounterpartySubjectId.value = row.counterpartySubjectId;
  nextSupplierCode.value = '';
  showModal.value = true;
};

const handleDialogClosed = () => {
  selectedSupplier.value = null;
  currentId.value = null;
  nextSupplierCode.value = '';
  originalCounterpartySubjectId.value = undefined;
};

const handleSupplierDialogSubmit = async (payload: Omit<ErpSupplier, 'id' | 'recentTransactionAt' | 'createdAt' | 'updatedAt'>) => {
  supplierDialogSaving.value = true;
  try {
    const allowed = await ensureSupplierRebindAllowed(payload.counterpartySubjectId);
    if (!allowed) return;
    const res: any = supplierDialogMode.value === 'edit' && currentId.value
      ? await request.put(`/erp/suppliers/${currentId.value}`, payload)
      : await request.post('/erp/suppliers', payload);

    if (res.data.code === 200) {
      invalidateErpBaseDataResourceCache('suppliers', tenantCacheKey.value);
      notifySuccess();
      showModal.value = false;
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
    supplierDialogSaving.value = false;
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
    invalidateErpBaseDataResourceCache('suppliers', tenantCacheKey.value);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const openImportDialog = () => {
  resetImportForm();
  showImportDialog.value = true;
};

const openImportHistoryDrawer = async () => {
  showImportHistoryDrawer.value = true;
  await loadImportBatches();
};

const loadImportBatches = async () => {
  importHistoryLoading.value = true;
  try {
    const res: any = await request.get('/erp/suppliers/import-batches');
    if (res.data.code === 200) {
      importBatches.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    importHistoryLoading.value = false;
  }
};

const viewImportBatchItems = async (batch: SupplierImportBatchSummary) => {
  selectedImportBatch.value = batch;
  importItemsLoading.value = true;
  try {
    const res: any = await request.get(`/erp/suppliers/import-batches/${batch.id}/items`);
    if (res.data.code === 200) {
      importBatchItems.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    importItemsLoading.value = false;
  }
};

const stopImportPolling = () => {
  if (importPollingTimer.value != null) {
    window.clearTimeout(importPollingTimer.value);
    importPollingTimer.value = null;
  }
  activeImportBatchId.value = null;
};

const pollImportBatch = async (batchId: number) => {
  await loadImportBatches();
  const batch = importBatches.value.find(item => item.id === batchId) || null;
  if (!batch) {
    stopImportPolling();
    return;
  }
  selectedImportBatch.value = batch;
  await viewImportBatchItems(batch);
  if (batch.status === 'PROCESSING') {
    importPollingTimer.value = window.setTimeout(() => {
      void pollImportBatch(batchId);
    }, 1500);
    return;
  }
  stopImportPolling();
  notifySuccess(batch.summary || `导入完成：成功 ${batch.successCount} 行，失败 ${batch.failedCount} 行`);
  fetchList();
};

const startImportPolling = (batchId: number) => {
  stopImportPolling();
  activeImportBatchId.value = batchId;
  importPollingTimer.value = window.setTimeout(() => {
    void pollImportBatch(batchId);
  }, 1000);
};

const submitSupplierImport = async () => {
  if (!importFile.value) {
    notifyError('请先选择要导入的 Excel 文件');
    return;
  }
  importSubmitting.value = true;
  try {
    const formData = new FormData();
    formData.append('file', importFile.value);
    const trimmedSourceName = importSourceName.value.trim();
    if (trimmedSourceName) {
      formData.append('sourceName', trimmedSourceName);
    }
    const res: any = await request.post('/erp/suppliers/import', formData);
    if (res.data.code === 200) {
      const result = res.data.data as SupplierImportResult;
      notifySuccess(`导入任务已创建：批次 ${result.batchNo}`);
      showImportDialog.value = false;
      await openImportHistoryDrawer();
      resetImportForm();
      startImportPolling(result.batchId);
    }
  } catch (error) {
    notifyError(error);
  } finally {
    importSubmitting.value = false;
  }
};

const handleImportFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement | null;
  importFile.value = target?.files?.[0] || null;
};

const resetImportForm = () => {
  importSourceName.value = '';
  importFile.value = null;
  if (importFileInputRef.value) {
    importFileInputRef.value.value = '';
  }
};

bindPageSizeSync(size, fetchList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: () => {
    pageSizeSyncReady.value = true;
    if (pendingInitialLoad.value && firstPaintReady.value) {
      pendingInitialLoad.value = false;
      fetchList();
    }
  }
});

onMounted(async () => {
  await waitForErpFirstPaint();
  firstPaintReady.value = true;
  fetchSettlementMethods();
  fetchPaymentMethods();
  fetchSupplierTypes();
  fetchCounterpartySubjects();
  fetchTenantKeys();
  loadImportBatches();
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

onBeforeUnmount(() => {
  stopImportPolling();
});

watch(
  [nameQuery, codeQuery, shortNameQuery, contactQuery, phoneQuery, supplierTypeFilter, businessScopeFilter, statusFilter, allTableData, size],
  () => {
    page.value = 1;
    applySearch();
  }
);
</script>
