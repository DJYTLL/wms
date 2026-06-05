<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCounterpartySubjectManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions">
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
          <ErpDataTableColumn v-if="canShow('customerCount')" prop="customerCount" :label="$t('field.customerCount')" width="110" />
          <ErpDataTableColumn v-if="canShow('supplierCount')" prop="supplierCount" :label="$t('field.supplierCount')" width="110" />
          <ErpDataTableColumn v-if="canShow('bindingStatus')" label="绑定状态" min-width="120">
            <template #default="{ row }">
              {{ (row.customerCount || 0) + (row.supplierCount || 0) > 0 ? '已绑定' : '未绑定' }}
            </template>
          </ErpDataTableColumn>
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
              <el-button link type="primary" size="small" @click="openDetail(row)">绑定明细</el-button>
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

    <el-drawer v-model="detailVisible" title="主体绑定明细" size="55%">
      <div class="detail-section">
        <div class="detail-section__title">供应商</div>
        <ErpDataTable :data="detailSuppliers" stripe :empty-text="$t('table.empty')" table-key="erp-counterparty-subject-suppliers">
          <ErpDataTableColumn prop="code" :label="$t('field.code')" min-width="140" />
          <ErpDataTableColumn prop="name" :label="$t('field.name')" min-width="180" />
          <ErpDataTableColumn prop="contact" :label="$t('field.contactPerson')" min-width="120" />
          <ErpDataTableColumn label="解绑校验" min-width="220">
            <template #default="{ row }">
              <el-tag v-if="row.unbindCheck?.allowed" type="success" size="small">可解绑</el-tag>
              <el-button v-else link type="warning" size="small" @click="showUnbindCheck(row, 'supplier')">查看阻塞原因</el-button>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="attemptUnbind(row, 'supplier')">解绑</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>
      <div class="detail-section">
        <div class="detail-section__title">客户</div>
        <ErpDataTable :data="detailCustomers" stripe :empty-text="$t('table.empty')" table-key="erp-counterparty-subject-customers">
          <ErpDataTableColumn prop="code" :label="$t('field.code')" min-width="140" />
          <ErpDataTableColumn prop="name" :label="$t('field.name')" min-width="180" />
          <ErpDataTableColumn prop="contact" :label="$t('field.contactPerson')" min-width="120" />
          <ErpDataTableColumn label="解绑校验" min-width="220">
            <template #default="{ row }">
              <el-tag v-if="row.unbindCheck?.allowed" type="success" size="small">可解绑</el-tag>
              <el-button v-else link type="warning" size="small" @click="showUnbindCheck(row, 'customer')">查看阻塞原因</el-button>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="attemptUnbind(row, 'customer')">解绑</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';
import { waitForErpFirstPaint } from './erpFirstPaint';

interface ErpCounterpartySubject {
  id: number;
  name: string;
  region?: string;
  unifiedCreditCode?: string;
  customerCount?: number;
  supplierCount?: number;
  enabled: boolean;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface CounterpartySubjectMember {
  id: number;
  code: string;
  name: string;
  contact?: string;
  unbindCheck?: {
    allowed: boolean;
    blockingReasons: string[];
    pendingDocs: Array<{
      docType: string;
      docId: number;
      orderNo: string;
      status: string;
      routeKey: string;
    }>;
  };
}

interface PendingDocItem {
  docType: string;
  docId: number;
  orderNo: string;
  status: string;
  routeKey: string;
}

const { t } = useI18n();
const router = useRouter();
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
const firstPaintReady = ref(false);
const tableData = ref<ErpCounterpartySubject[]>([]);
const allTableData = ref<ErpCounterpartySubject[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const detailVisible = ref(false);
const currentDetailSubjectId = ref<number | null>(null);
const detailCustomers = ref<CounterpartySubjectMember[]>([]);
const detailSuppliers = ref<CounterpartySubjectMember[]>([]);

const defaultColumns = ['name', 'region', 'unifiedCreditCode', 'customerCount', 'supplierCount', 'bindingStatus', 'status', 'remark', 'createdAt', 'updatedAt'];
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

const openDetail = async (row: ErpCounterpartySubject) => {
  detailVisible.value = true;
  currentDetailSubjectId.value = row.id;
  try {
    const res: any = await request.get(`/erp/counterparty-subjects/${row.id}/detail`);
    if (res.data.code === 200) {
      detailCustomers.value = res.data.data?.customers || [];
      detailSuppliers.value = res.data.data?.suppliers || [];
    }
  } catch (error) {
    notifyError(error);
  }
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

const buildUnbindCheckHtml = (docs: PendingDocItem[], reasons: string[]) => {
  const sections: string[] = [];
  if (reasons.length) {
    sections.push([
      '<div style="margin-bottom: 12px;">',
      '<div style="font-weight: 600; margin-bottom: 6px;">阻塞原因</div>',
      ...reasons.map(reason => `<div style="margin-top: 4px;">${reason}</div>`),
      '</div>'
    ].join(''));
  }

  if (docs.length) {
    const groupedDocs = docs.reduce<Record<string, Array<{ doc: PendingDocItem; index: number }>>>((acc, doc, index) => {
      const key = doc.docType || 'UNKNOWN';
      if (!acc[key]) {
        acc[key] = [];
      }
      acc[key].push({ doc, index });
      return acc;
    }, {});

    const groupedHtml = Object.entries(groupedDocs).map(([docType, items]) => [
      '<div style="margin-bottom: 12px;">',
      `<div style="font-weight: 600; margin-bottom: 6px;">${getDocTypeLabel(docType)}</div>`,
      ...items.map(({ doc, index }) => {
        const label = `${doc.orderNo || doc.docType || '未命名单据'}（${doc.status || '-'}）`;
        return `<div style="margin-top: 4px; padding-left: 8px;"><a href="#" data-doc-index="${index}">${label}</a></div>`;
      }),
      '</div>'
    ].join(''));
    sections.push(groupedHtml.join(''));
  }

  if (!sections.length) {
    sections.push('<div>当前没有阻塞原因</div>');
  }

  return sections.join('');
};

const showUnbindCheck = async (member: CounterpartySubjectMember, type: 'supplier' | 'customer') => {
  const reasons = member.unbindCheck?.blockingReasons || [];
  const docs = member.unbindCheck?.pendingDocs || [];
  const html = buildUnbindCheckHtml(docs, reasons);
  try {
    await ElMessageBox.alert(html, `${type === 'supplier' ? '供应商' : '客户'}解绑校验`, {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了',
      callback: () => undefined
    });
  } finally {
    requestAnimationFrame(() => {
      document.querySelectorAll('.el-message-box a[data-doc-index]').forEach((link) => {
        link.addEventListener('click', (event) => {
          event.preventDefault();
          const index = Number((event.currentTarget as HTMLElement).getAttribute('data-doc-index'));
          const doc = docs[index];
          if (doc) {
            router.push({ name: doc.routeKey, params: { id: doc.docId } });
          }
        }, { once: true });
      });
    });
  }
};

const attemptUnbind = async (member: CounterpartySubjectMember, type: 'supplier' | 'customer') => {
  if (!currentDetailSubjectId.value) return;
  try {
    const checkUrl = type === 'supplier'
      ? `/erp/counterparty-subjects/${currentDetailSubjectId.value}/bind-supplier/${member.id}/check`
      : `/erp/counterparty-subjects/${currentDetailSubjectId.value}/bind-customer/${member.id}/check`;
    const res: any = await request.get(checkUrl);
    const check = res.data?.data;
    member.unbindCheck = check;
    if (!check?.allowed) {
      await showUnbindCheck(member, type);
      return;
    }
    await ElMessageBox.confirm(`确认解绑${type === 'supplier' ? '供应商' : '客户'}“${member.name}”吗？`, '确认解绑', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    });
    const unbindUrl = type === 'supplier'
      ? `/erp/counterparty-subjects/${currentDetailSubjectId.value}/bind-supplier/${member.id}`
      : `/erp/counterparty-subjects/${currentDetailSubjectId.value}/bind-customer/${member.id}`;
    await request.delete(unbindUrl);
    notifySuccess();
    await fetchList();
    await openDetail({ id: currentDetailSubjectId.value } as ErpCounterpartySubject);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
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
    if (pendingInitialLoad.value && firstPaintReady.value) {
      pendingInitialLoad.value = false;
      fetchList();
    }
  }
});

onMounted(async () => {
  await waitForErpFirstPaint();
  firstPaintReady.value = true;
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

<style scoped>
.detail-section {
  margin-bottom: 16px;
}

.detail-section__title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
