<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ pageTitle }}</div>
      <ErpSaleListToolbar>
        <template #filters>
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <FuzzyProductSelect
            v-model="customerFilter"
            :options="customerOptions"
            :placeholder="$t('field.customer')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            @change="handleSearch"
          />
          <!-- <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search" clearable :disabled="statusLocked" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="" />
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select> -->
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="x"
            format="YYYY-MM-DD HH:mm:ss"
            :start-placeholder="$t('field.startTime')"
            :end-placeholder="$t('field.endTime')"
            class="erp-toolbar__date-range table-date-range table-date-range--compact"
            @change="handleSearch"
          />
        </template>
        <template #actions>
            <el-button
              v-if="canCreate"
              type="primary"
              @click="openCreatePage"
            >
              {{ $t('action.add') }}
            </el-button>
        </template>
      </ErpSaleListToolbar>
    </div>

    <div class="table-card" :class="{ 'sale-approved-card': isApprovedPage }">
      <div class="table-body">
        <ErpDataTable
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="rowClassName"
         table-key="erp-sale-return-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
          <ErpDataTableColumn v-if="canShow('customer')" :label="$t('field.customer')" min-width="160" column-key="customer">
            <template #default="{ row }">
              {{ getCustomerName(row.customerId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('totalAmount')" prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('refundStatus')" :label="$t('field.refundStatus')" min-width="150" column-key="refundStatus">
            <template #default="{ row }">
              <el-tag :type="financeStatusTagType(row.refundStatus)" size="small">
                {{ formatFinanceStatus(row.refundStatus, row.refundUnpaidAmount) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="300" fixed="right" column-key="actions">
            <template #default="{ row }">
              <template v-if="isApprovedPage">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="openViewPage(row)"
                >
                  {{ $t('action.view') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale-return-approved:print'"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  v-if="canCopy"
                  link
                  type="primary"
                  size="small"
                  @click="handleCopy(row)"
                >
                  {{ $t('action.copy') }}
                </el-button>
                <el-button
                  v-if="row.status === 'APPROVED'"
                  link
                  type="danger"
                  size="small"
                  v-permission="'erp-sale-return-approved:redflush'"
                  @click="handleRedFlush(row)"
                >
                  {{ $t('action.redFlush') }}
                </el-button>
              </template>
              <template v-else>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale-return-draft:edit'"
                  @click="openEditPage(row)"
                >
                  {{ $t('action.edit') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale-return-draft:print'"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="success"
                  size="small"
                  v-permission="'erp-sale-return-draft:approve'"
                  @click="handleApprove(row)"
                >
                  {{ $t('action.approve') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="danger"
                  size="small"
                  v-permission="'erp-sale-return-draft:delete'"
                  @click="handleDelete(row)"
                >
                  {{ $t('action.delete') }}
                </el-button>
              </template>
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

    <PrintPreviewDialog
      v-model="printDialogVisible"
      :doc-type="printDocType"
      :doc-id="printDocId"
      :title="$t('page.erpSaleReturnPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import { getCachedCustomers, invalidateErpBaseDataCache } from '@/composables/erpBaseDataCache';
import { createInflightRequestDeduper } from '@/composables/inflightRequestDeduperCore';
import { useAuthStore } from '@/stores/auth';
import ErpSaleListToolbar from './ErpSaleListToolbar.vue';

const PrintPreviewDialog = defineAsyncComponent(() => import('@/components/PrintPreviewDialog.vue'));

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface SaleReturnItem {
  id?: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qty?: number;
  price?: number;
  taxRate?: number;
  remark?: string;
  sortNo?: number;
}

interface SaleReturn {
  id: number;
  orderNo?: string;
  customerId?: number;
  status: string;
  totalAmount?: number;
  refundStatus?: string;
  refundUnpaidAmount?: number;
  createdAt?: string;
}

const props = defineProps<{
  workspace?: 'draft' | 'approved'
}>();

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();

const searchQuery = ref('');
const statusFilter = ref('');
const statusLocked = ref(false);
const customerFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<SaleReturn[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const initializedRoutePath = ref('');
const pageSizeSyncReady = ref(false);
const pendingRouteRefresh = ref(false);
const listRequestDeduper = createInflightRequestDeduper();

const customerOptions = ref<OptionItem[]>([]);

const isSaleReturnRoute = computed(() => route.path.startsWith('/erp/sale-returns'));
const currentWorkspace = computed<'draft' | 'approved'>(() => {
  if (props.workspace) return props.workspace;
  if (route.path.includes('/erp/sale-returns/approved')) return 'approved';
  return 'draft';
});
const isCurrentWorkspaceRoute = computed(() => {
  if (!isSaleReturnRoute.value) return false;
  if (props.workspace === 'draft') return route.path === '/erp/sale-returns/draft';
  if (props.workspace === 'approved') return route.path === '/erp/sale-returns/approved';
  return route.path === '/erp/sale-returns/draft' || route.path === '/erp/sale-returns/approved';
});
const isDraftPage = computed(() => currentWorkspace.value === 'draft');
const isApprovedPage = computed(() => currentWorkspace.value === 'approved');
const printDocType = computed(() => isApprovedPage.value ? 'SALE_RETURN_APPROVED' : 'SALE_RETURN_DRAFT');
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const statusOptions = computed(() => {
  const base = [
    { value: 'DRAFT', label: t('status.draft') },
    { value: 'APPROVED', label: t('status.approved') },
    { value: 'RED_FLUSHED', label: t('status.redFlushed') }
  ];
  if (isApprovedPage.value) {
    base.unshift({ value: 'APPROVED,RED_FLUSHED', label: `${t('status.approved')}/${t('status.redFlushed')}` });
  }
  return base;
});

const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined;
  return key ? t(key) : t('page.erpSaleReturnManagement');
});

const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canCreate = computed(() => {
  const defaultStatus = route.meta.defaultStatus as string | undefined;
  if (defaultStatus === 'APPROVED') {
    return false;
  }
  if (statusLocked.value && statusFilter.value === 'APPROVED') {
    return false;
  }
  return hasPermission('erp-sale-return-draft:add');
});

const canCopy = computed(() => isApprovedPage.value
  && hasPermission('erp-sale-return-approved:copy')
  && hasPermission('erp-sale-return-draft:add'));

const defaultColumns = ['orderNo', 'customer', 'status', 'totalAmount', 'refundStatus', 'createdAt'];
const draftColumnSettings = useColumnSettings('erp-sale-return-draft', defaultColumns);
const approvedColumnSettings = useColumnSettings('erp-sale-return-approved', defaultColumns);

const isVisible = (key: string) => (
  isApprovedPage.value
    ? approvedColumnSettings.isVisible(key)
    : draftColumnSettings.isVisible(key)
);

const fetchCurrentTenantKeys = () => (
  isApprovedPage.value
    ? approvedColumnSettings.fetchTenantKeys()
    : draftColumnSettings.fetchTenantKeys()
);

const canShow = (key: string) => isVisible(key);

const statusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const formatStatus = (status: string) => {
  const mapping: Record<string, string> = {
    DRAFT: t('status.draft'),
    APPROVED: t('status.approved'),
    RED_FLUSHED: t('status.redFlushed')
  };
  return mapping[status] || status;
};

const financeStatusTagType = (status?: string) => {
  if (status === 'SETTLED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  if (status === 'OPEN') return 'warning';
  return 'info';
};

const formatFinanceStatus = (status?: string, unpaidAmount?: number) => {
  if (!status) return '-';
  if (status === 'SETTLED') return t('status.settled');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  if (status === 'OPEN') {
    const unpaid = Math.abs(Number(unpaidAmount || 0));
    return unpaid > 0 ? `待付 ${formatAmount(unpaid)}` : '待付';
  }
  return status;
};

const formatAmount = (value?: number) => {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
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

const getCustomerName = (id?: number) => customerOptions.value.find(item => item.id === id)?.name || '-';

const fetchCustomers = async () => {
  if (!isCurrentWorkspaceRoute.value || customerOptions.value.length > 0) {
    return;
  }
  try {
    customerOptions.value = await getCachedCustomers(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
  if (!isCurrentWorkspaceRoute.value) {
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value) params.status = statusFilter.value;
    if (customerFilter.value) params.customerId = customerFilter.value;
    if (dateRange.value && dateRange.value.length === 2) {
      const start = Number(dateRange.value[0]);
      const end = Number(dateRange.value[1]);
      params.startAt = start;
      params.endAt = end;
    }

    const endpoint = isApprovedPage.value ? '/erp/sale-returns/approved/page' : '/erp/sale-returns/draft/page';
    const requestKey = `${endpoint}?${JSON.stringify(params)}`;
    const res: any = await listRequestDeduper.run(requestKey, () => (
      request.get(endpoint, { params })
    ));
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

const applyRouteStatus = () => {
  if (!isSaleReturnRoute.value) {
    statusLocked.value = false;
    statusFilter.value = '';
    return;
  }
  const defaultStatus = route.meta.defaultStatus as string | undefined;
  const lockStatus = route.meta.lockStatus === true;
  statusLocked.value = lockStatus;
  if (defaultStatus) {
    if (defaultStatus === 'APPROVED') {
      statusFilter.value = 'APPROVED,RED_FLUSHED';
    } else {
      statusFilter.value = defaultStatus;
    }
    tableData.value = [];
    total.value = 0;
    return;
  }
  if (!lockStatus) {
    statusFilter.value = '';
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const runRouteRefresh = () => {
  if (!isCurrentWorkspaceRoute.value) {
    return;
  }
  initializedRoutePath.value = route.fullPath;
  applyRouteStatus();
  void fetchCustomers();
  void fetchCurrentTenantKeys();
  handleSearch();
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

const openCreatePage = () => {
  const query = isDraftPage.value
    ? { from: 'draft', returnTo: route.path }
    : { returnTo: route.path };
  router.push({ path: '/erp/sale-returns/draft/create', query });
};

const openEditPage = (row: SaleReturn) => {
  const query = isDraftPage.value
    ? { from: 'draft', returnTo: route.path }
    : { returnTo: route.path };
  router.push({ path: `/erp/sale-returns/draft/${row.id}/edit`, query });
};

const openViewPage = (row: SaleReturn) => {
  router.push({
    path: `/erp/sale-returns/approved/${row.id}`,
    query: { mode: 'view', from: 'approved', returnTo: route.path }
  });
};

const handleApprove = async (row: SaleReturn) => {
  try {
    await request.post(`/erp/sale-returns/draft/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleRedFlush = async (row: SaleReturn) => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('message.confirmRedFlush'),
      t('action.redFlush'),
      {
        inputPlaceholder: t('placeholder.required'),
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel')
      }
    );
    if (!value || !String(value).trim()) {
      return;
    }
    await request.post(`/erp/sale-returns/approved/${row.id}/red-flush`, null, {
      params: { reason: String(value).trim() }
    });
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleCopy = async (row: SaleReturn) => {
  try {
    await ElMessageBox.confirm(
      t('message.confirmCopyOrder'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.copy'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
  } catch {
    return;
  }
  try {
    const createRes: any = await request.post(`/erp/sale-returns/approved/${row.id}/copy`);
    if (createRes.data.code === 200) {
      const data = createRes.data.data || {};
      const newId = data.order?.id || data.id;
      notifySuccess();
      if (newId) {
        await router.push({
          path: `/erp/sale-returns/draft/${newId}/edit`,
          query: { from: 'draft', returnTo: '/erp/sale-returns/draft' }
        });
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: SaleReturn) => {
  try {
    await ElMessageBox.confirm(
      t('message.deleteConfirm'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.delete'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
    await request.delete(`/erp/sale-returns/draft/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const openPrintPage = (row: SaleReturn) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const rowClassName = ({ row }: { row: SaleReturn }) => {
  if (row.status === 'RED_FLUSHED') return 'row-red-flushed';
  return '';
};

bindPageSizeSync(size, fetchList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: () => {
    pageSizeSyncReady.value = true;
    if (pendingRouteRefresh.value) {
      pendingRouteRefresh.value = false;
      runRouteRefresh();
    }
  }
});

watch(
  () => authStore.tenantId,
  (nextTenantId, prevTenantId) => {
    if (nextTenantId === prevTenantId) {
      return;
    }
    invalidateErpBaseDataCache(prevTenantId ?? undefined);
    customerOptions.value = [];
  }
);

watch(
  () => route.fullPath,
  () => {
    if (!isCurrentWorkspaceRoute.value) {
      return;
    }
    if (!pageSizeSyncReady.value) {
      pendingRouteRefresh.value = true;
      return;
    }
    runRouteRefresh();
  },
  { flush: 'sync', immediate: true }
);
</script>

<style scoped>
:deep(.row-red-flushed > td) {
  background-color: #fff1f0 !important;
}

:deep(.row-red-flushed:hover > td) {
  background-color: #fff1f0 !important;
}

.sale-approved-card .table-body {
  max-height: 100%;
  overflow: auto;
}

</style>
