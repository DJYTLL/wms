<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ pageTitle }}</div>
      <ErpSaleListToolbar :show-action-placeholder="false">
        <template #filters>
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="customerFilter"
            :placeholder="$t('field.customer')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerSearchLoading"
            :automatic-dropdown="false"
            @clear="handleSearch"
            @change="handleSearch"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
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
      </ErpSaleListToolbar>
    </div>

    <SaleReturnApprovedDeferredPanel
      v-if="showDeferredPanel"
      :rows="tableData"
      :columns="visibleColumns"
      :loading="loading"
      :empty-text="$t('table.empty')"
      :total="total"
      :page="page"
      :size="size"
      :can-copy="canCopy"
      :format-status="formatStatus"
      :status-tag-type="statusTagType"
      :format-finance-status="formatFinanceStatus"
      :finance-status-tag-type="financeStatusTagType"
      :format-date-time="formatDateTime"
      :get-customer-name="getCustomerName"
      :row-class-name="rowClassName"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
      @view="openViewPage"
      @copy="handleCopy"
      @red-flush="handleRedFlush"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import type { ErpDataTableColumn } from '@/components/ErpDataTable.vue';
import ErpSaleListToolbar from './ErpSaleListToolbar.vue';
import request from '@/utils/request';
import { createInflightRequestDeduper } from '@/composables/inflightRequestDeduperCore';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import { buildSaleListRequestKey, getCachedSaleListRequest, invalidateSaleListRequestCache } from './saleListWarmupCache';
import { markErpNavigationPerf } from '@/utils/erpNavigationPerfTrace';
import { loadSaleReturnApprovedDeferredPanel } from './saleListDeferredPanelLoaders';
import { waitForSaleListFirstPaint } from './saleListFirstPaint';
import { useSaleListCustomerSearch } from './useSaleListCustomerSearch';
markErpNavigationPerf('sale-order-list:setup', {
  page: 'return-approved'
});

const SaleReturnApprovedDeferredPanel = defineAsyncComponent({
  loader: loadSaleReturnApprovedDeferredPanel
});

interface SaleReturn {
  id: number;
  orderNo?: string;
  customerId?: number;
  customerName?: string;
  status: string;
  totalAmount?: number;
  refundStatus?: string;
  refundUnpaidAmount?: number;
  createdAt?: string;
}

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { notifyError, notifySuccess } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const listRequestDeduper = createInflightRequestDeduper();
const {
  customerFilter,
  customerOptions,
  customerSearchLoading,
  searchCustomers,
  resetCustomerSearch
} = useSaleListCustomerSearch(notifyError);

const searchQuery = ref('');
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<SaleReturn[]>([]);
const showDeferredPanel = ref(false);
const pageSizeSyncReady = ref(false);
const pendingRouteRefresh = ref(false);

const defaultColumns = ['orderNo', 'customer', 'status', 'totalAmount', 'refundStatus', 'createdAt'];
const columnSettings = useColumnSettings('erp-sale-return-approved', defaultColumns);
const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined;
  return key ? t(key) : t('page.erpSaleReturnApproved');
});

const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const canCopy = computed(() => (
  hasPermission('erp-sale-return-approved:copy')
  && hasPermission('erp-sale-return-draft:add')
));

const columns = computed<ErpDataTableColumn[]>(() => [
  { key: 'index', label: t('table.index'), width: 70, minWidth: 56, resizable: false, configurable: false },
  { key: 'orderNo', label: t('field.orderNo'), prop: 'orderNo', width: 160, minWidth: 56 },
  { key: 'customer', label: t('field.customer'), width: 160, minWidth: 56 },
  { key: 'status', label: t('field.status'), width: 120, minWidth: 56 },
  { key: 'totalAmount', label: t('field.totalAmount'), prop: 'totalAmount', width: 140, minWidth: 56 },
  { key: 'refundStatus', label: t('field.refundStatus'), width: 150, minWidth: 56 },
  { key: 'createdAt', label: t('field.createdTime'), width: 180, minWidth: 56, nowrap: true },
  { key: 'actions', label: t('table.actions'), width: 300, minWidth: 180, stickyRight: true, resizable: false, configurable: false }
]);

const visibleColumns = computed(() => columns.value.filter((column) => {
  if (column.key === 'index' || column.key === 'actions') return true;
  return columnSettings.isVisible(column.key);
}));

const statusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const formatStatus = (status: string) => {
  const mapping: Record<string, string> = {
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

const formatAmount = (value?: number) => {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
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

const getCustomerName = (id?: number, name?: string) => name || customerOptions.value.find((item) => item.id === id)?.name || '-';

const buildListParams = () => {
  const params: Record<string, any> = {
    page: page.value,
    size: size.value,
    status: 'APPROVED,RED_FLUSHED'
  };
  if (searchQuery.value) params.keyword = searchQuery.value.trim();
  if (customerFilter.value) params.customerId = customerFilter.value;
  if (dateRange.value && dateRange.value.length === 2) {
    params.startAt = Number(dateRange.value[0]);
    params.endAt = Number(dateRange.value[1]);
  }
  return params;
};

const fetchList = async () => {
  markErpNavigationPerf('sale-order-list:list-fetch-start', {
    page: 'return-approved'
  });
  loading.value = true;
  const params = buildListParams();
  const requestKey = buildSaleListRequestKey('/erp/sale-returns/approved/page', params);
  try {
    const res: any = await getCachedSaleListRequest(requestKey, () => (
      listRequestDeduper.run(requestKey, () => request.get('/erp/sale-returns/approved/page', { params }))
    ));
    if (res.data.code === 200) {
      tableData.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
    markErpNavigationPerf('sale-order-list:list-fetch-end', {
      page: 'return-approved',
      rows: tableData.value.length,
      total: total.value
    });
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

const runRouteRefresh = () => {
  void columnSettings.fetchTenantKeys();
  handleSearch();
};

const openViewPage = (row: SaleReturn) => {
  router.push({
    path: `/erp/sale-returns/approved/${row.id}`,
    query: { mode: 'view', from: 'approved', returnTo: route.path }
  });
};

const handleRedFlush = async (row: SaleReturn) => {
  try {
    const { value } = await ElMessageBox.prompt(t('message.confirmRedFlush'), t('action.redFlush'), {
      inputPlaceholder: t('placeholder.required'),
      confirmButtonText: t('action.confirm'),
      cancelButtonText: t('action.cancel')
    });
    if (!value || !String(value).trim()) return;
    await request.post(`/erp/sale-returns/approved/${row.id}/red-flush`, null, {
      params: { reason: String(value).trim() }
    });
    notifySuccess();
    invalidateSaleListRequestCache();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') notifyError(error);
  }
};

const handleCopy = async (row: SaleReturn) => {
  try {
    await ElMessageBox.confirm(t('message.confirmCopyOrder'), t('action.confirm'), {
      confirmButtonText: t('action.copy'),
      cancelButtonText: t('action.cancel'),
      type: 'warning'
    });
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

const rowClassName = ({ row }: { row: SaleReturn }) => (row.status === 'RED_FLUSHED' ? 'row-red-flushed' : '');

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

onMounted(async () => {
  markErpNavigationPerf('sale-order-list:mounted', {
    page: 'return-approved'
  });
  await waitForSaleListFirstPaint();
  if (pageSizeSyncReady.value) {
    runRouteRefresh();
  } else {
    pendingRouteRefresh.value = true;
  }
  showDeferredPanel.value = true;
});

watch(
  () => authStore.tenantId,
  (nextTenantId, prevTenantId) => {
    if (nextTenantId === prevTenantId) return;
    resetCustomerSearch();
  }
);

watch(
  () => route.fullPath,
  () => {
    if (route.path !== '/erp/sale-returns/approved') return;
    if (!pageSizeSyncReady.value) {
      pendingRouteRefresh.value = true;
      return;
    }
    runRouteRefresh();
  }
);
</script>

<style scoped>
:deep(.row-red-flushed > td) {
  background-color: #fff1f0 !important;
}

:deep(.row-red-flushed:hover > td) {
  background-color: #fff1f0 !important;
}

</style>
