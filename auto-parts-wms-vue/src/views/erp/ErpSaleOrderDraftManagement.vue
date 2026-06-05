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
            :shortcuts="dateRangeShortcuts"
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
            v-permission="'erp-sale-draft:add'"
            @click="openCreatePage"
          >
            {{ $t('action.add') }}
          </el-button>
        </template>
      </ErpSaleListToolbar>
    </div>

    <SaleOrderDraftDeferredPanel
      v-if="showDeferredPanel"
      :rows="tableData"
      :columns="visibleColumns"
      :loading="loading"
      :empty-text="$t('table.empty')"
      :total="total"
      :page="page"
      :size="size"
      :summary="summary"
      :can-show-profit="canShowProfit"
      :format-amount="formatAmount"
      :format-finance-status="formatFinanceStatus"
      :finance-status-tag-type="financeStatusTagType"
      :format-date-time="formatDateTime"
      :get-customer-name="getCustomerName"
      :summary-label="summaryLabel"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
      @edit="openEditPage"
      @approve="handleApprove"
      @delete="handleDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue';
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
import { markErpNavigationPerf } from '@/utils/erpNavigationPerfTrace';
import { buildSaleListRequestKey, getCachedSaleListRequest, invalidateSaleListRequestCache } from './saleListWarmupCache';
import { loadSaleOrderDraftDeferredPanel } from './saleListDeferredPanelLoaders';
import { waitForSaleListFirstPaint } from './saleListFirstPaint';
import { useSaleListCustomerSearch } from './useSaleListCustomerSearch';
markErpNavigationPerf('sale-order-list:setup', {
  page: 'draft'
});

const SaleOrderDraftDeferredPanel = defineAsyncComponent({
  loader: loadSaleOrderDraftDeferredPanel
});

interface SaleOrder {
  id: number;
  orderNo?: string;
  customerId?: number;
  customerName?: string;
  status: string;
  totalAmount?: number;
  totalAmountInclTax?: number;
  netSaleAmount?: number;
  netGrossProfit?: number;
  cumulativeReturnAmount?: number;
  receivableStatus?: string;
  receivableUnpaidAmount?: number;
  createdAt?: string;
}

type SummaryMode = 'page' | 'range';

interface SaleOrderSummary {
  saleAmountTotal: number;
  returnAmountTotal: number;
  netSaleAmountTotal: number;
  netGrossProfitTotal: number;
  summaryMode: SummaryMode;
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
const tableData = ref<SaleOrder[]>([]);
const showProfitColumn = ref(false);
const showDeferredPanel = ref(false);
const pageSizeSyncReady = ref(false);
const pendingRouteRefresh = ref(false);
const summary = reactive<SaleOrderSummary>({
  saleAmountTotal: 0,
  returnAmountTotal: 0,
  netSaleAmountTotal: 0,
  netGrossProfitTotal: 0,
  summaryMode: 'page'
});

const columnPageKey = 'erp-sale-draft';
const draftColumns = ['orderNo', 'customer', 'totalAmount', 'netSaleAmount', 'netGrossProfit', 'receivableStatus', 'createdAt'];
const columnSettings = useColumnSettings(columnPageKey, draftColumns);
const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined;
  return key ? t(key) : t('page.erpSaleOrderDraft');
});

const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const canCreate = computed(() => hasPermission('erp-sale-draft:add'));
const canViewProfit = computed(() => (
  hasPermission(`column:${columnPageKey}:netGrossProfit`)
  && (hasPermission('erp-product:cost:view') || hasPermission('erp-product:cost:edit'))
));
const canShowProfit = computed(() => showProfitColumn.value && canViewProfit.value && columnSettings.isVisible('netGrossProfit'));

const columns = computed<ErpDataTableColumn[]>(() => [
  { key: 'index', label: t('table.index'), width: 70, minWidth: 56, resizable: false, configurable: false },
  { key: 'orderNo', label: t('field.orderNo'), prop: 'orderNo', width: 160, minWidth: 56 },
  { key: 'customer', label: t('field.customer'), width: 160, minWidth: 56 },
  { key: 'totalAmount', label: t('field.totalAmount'), prop: 'totalAmount', width: 140, minWidth: 56 },
  { key: 'netSaleAmount', label: t('field.netSaleAmount'), width: 140, minWidth: 56 },
  { key: 'netGrossProfit', label: t('field.netGrossProfit'), width: 140, minWidth: 56 },
  { key: 'receivableStatus', label: t('field.receivableStatus'), width: 150, minWidth: 56 },
  { key: 'createdAt', label: t('field.createdTime'), width: 180, minWidth: 56, nowrap: true },
  { key: 'actions', label: t('table.actions'), width: 260, minWidth: 180, stickyRight: true, resizable: false, configurable: false }
]);

const visibleColumns = computed(() => columns.value.filter((column) => {
  if (column.key === 'index' || column.key === 'actions') return true;
  if (column.key === 'netGrossProfit') return canShowProfit.value;
  return columnSettings.isVisible(column.key);
}));

const hasSelectedDateRange = computed(() => (
  Array.isArray(dateRange.value) && dateRange.value.length === 2 && !!dateRange.value[0] && !!dateRange.value[1]
));

const dateRangeShortcuts = computed(() => {
  const now = new Date();
  const buildShortcutRange = (year: number, monthIndex: number) => {
    const start = new Date(year, monthIndex, 1, 0, 0, 0, 0);
    const end = new Date(year, monthIndex + 1, 0, 23, 59, 59, 999);
    return [start, end];
  };
  return [
    { text: t('field.thisMonth'), value: buildShortcutRange(now.getFullYear(), now.getMonth()) },
    { text: t('field.lastMonth'), value: buildShortcutRange(now.getFullYear(), now.getMonth() - 1) }
  ];
});

const isTypingTarget = (target: EventTarget | null) => {
  if (!target || !(target instanceof HTMLElement)) return false;
  const tag = target.tagName.toLowerCase();
  if (tag === 'input' || tag === 'textarea' || tag === 'select') return true;
  return target.isContentEditable;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (!canViewProfit.value || isTypingTarget(event.target)) return;
  if (event.key && event.key.toLowerCase() === 'u') {
    if (event.repeat) return;
    event.preventDefault();
    showProfitColumn.value = !showProfitColumn.value;
  }
};

const financeStatusTagType = (status?: string) => {
  if (status === 'SETTLED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  if (status === 'OPEN') return 'warning';
  return 'info';
};

const formatAmount = (value?: number | string) => {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
};

const formatFinanceStatus = (status?: string, unpaidAmount?: number) => {
  if (!status) return '-';
  if (status === 'SETTLED') return t('status.settled');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  if (status === 'OPEN') {
    const unpaid = Number(unpaidAmount || 0);
    return unpaid > 0 ? `${t('status.open')} ${formatAmount(unpaid)}` : t('status.open');
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

const resetSummary = (mode: SummaryMode = 'page') => {
  summary.saleAmountTotal = 0;
  summary.returnAmountTotal = 0;
  summary.netSaleAmountTotal = 0;
  summary.netGrossProfitTotal = 0;
  summary.summaryMode = mode;
};

const setSummary = (payload: Partial<SaleOrderSummary> & { summaryMode: SummaryMode }) => {
  summary.saleAmountTotal = Number(payload.saleAmountTotal || 0);
  summary.returnAmountTotal = Number(payload.returnAmountTotal || 0);
  summary.netSaleAmountTotal = Number(payload.netSaleAmountTotal || 0);
  summary.netGrossProfitTotal = Number(payload.netGrossProfitTotal || 0);
  summary.summaryMode = payload.summaryMode;
};

const buildListParams = () => {
  const params: Record<string, any> = {
    page: page.value,
    size: size.value,
    status: 'DRAFT'
  };
  if (searchQuery.value) params.keyword = searchQuery.value.trim();
  if (customerFilter.value) params.customerId = customerFilter.value;
  if (hasSelectedDateRange.value && dateRange.value) {
    params.startAt = Number(dateRange.value[0]);
    params.endAt = Number(dateRange.value[1]);
  }
  return params;
};

const updateCurrentPageSummary = () => {
  const next = tableData.value.reduce((acc, row) => {
    acc.saleAmountTotal += Number(row.totalAmountInclTax ?? row.totalAmount ?? 0);
    acc.returnAmountTotal += Number(row.cumulativeReturnAmount || 0);
    acc.netSaleAmountTotal += Number(row.netSaleAmount || 0);
    acc.netGrossProfitTotal += Number(row.netGrossProfit || 0);
    return acc;
  }, {
    saleAmountTotal: 0,
    returnAmountTotal: 0,
    netSaleAmountTotal: 0,
    netGrossProfitTotal: 0
  });
  setSummary({ ...next, summaryMode: 'page' });
};

const fetchRangeSummary = async () => {
  const params = buildListParams();
  delete params.page;
  delete params.size;
  const res: any = await request.get('/erp/sale-orders/draft/summary', { params });
  if (res.data.code === 200) {
    setSummary({
      saleAmountTotal: res.data.data?.saleAmountTotal,
      returnAmountTotal: res.data.data?.returnAmountTotal,
      netSaleAmountTotal: res.data.data?.netSaleAmountTotal,
      netGrossProfitTotal: res.data.data?.netGrossProfitTotal,
      summaryMode: 'range'
    });
    return;
  }
  resetSummary('range');
};

const summaryLabel = (key: 'saleAmount' | 'returnAmount' | 'netSaleAmount' | 'netGrossProfit') => {
  const labels: Record<typeof key, { page: string; range: string }> = {
    saleAmount: { page: t('field.currentPageSaleAmount'), range: t('field.rangeSaleAmount') },
    returnAmount: { page: t('field.currentPageReturnAmount'), range: t('field.rangeReturnAmount') },
    netSaleAmount: { page: t('field.currentPageNetSaleAmount'), range: t('field.rangeNetSaleAmount') },
    netGrossProfit: { page: t('field.currentPageNetGrossProfit'), range: t('field.rangeNetGrossProfit') }
  };
  return labels[key][summary.summaryMode];
};

const fetchList = async () => {
  markErpNavigationPerf('sale-order-list:list-fetch-start', {
    page: 'draft'
  });
  loading.value = true;
  const params = buildListParams();
  const requestKey = buildSaleListRequestKey('/erp/sale-orders/draft/page', params);
  try {
    const res: any = await getCachedSaleListRequest(requestKey, () => (
      listRequestDeduper.run(requestKey, () => request.get('/erp/sale-orders/draft/page', { params }))
    ));
    if (res.data.code === 200) {
      tableData.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
      if (hasSelectedDateRange.value) {
        await fetchRangeSummary();
      } else {
        updateCurrentPageSummary();
      }
    } else {
      resetSummary(hasSelectedDateRange.value ? 'range' : 'page');
    }
  } catch (error) {
    resetSummary(hasSelectedDateRange.value ? 'range' : 'page');
    notifyError(error);
  } finally {
    loading.value = false;
    markErpNavigationPerf('sale-order-list:list-fetch-end', {
      page: 'draft',
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
  resetSummary(hasSelectedDateRange.value ? 'range' : 'page');
  void columnSettings.fetchTenantKeys();
  handleSearch();
};

const openCreatePage = () => {
  router.push({ path: '/erp/sale-orders/draft/create', query: { from: 'draft', returnTo: route.path } });
};

const openEditPage = (row: SaleOrder) => {
  router.push({ path: `/erp/sale-orders/draft/${row.id}/edit`, query: { from: 'draft', returnTo: route.path } });
};

const handleApprove = async (row: SaleOrder) => {
  try {
    await ElMessageBox.confirm(t('message.confirmApproveDraftOrder', { orderNo: row.orderNo || '-' }), t('action.confirm'), {
      confirmButtonText: t('action.approve'),
      cancelButtonText: t('action.cancel'),
      type: 'warning'
    });
    await request.post(`/erp/sale-orders/draft/${row.id}/approve`);
    notifySuccess();
    invalidateSaleListRequestCache();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') notifyError(error);
  }
};

const handleDelete = async (row: SaleOrder) => {
  try {
    await ElMessageBox.confirm(t('message.deleteConfirm'), t('action.confirm'), {
      confirmButtonText: t('action.delete'),
      cancelButtonText: t('action.cancel'),
      type: 'warning'
    });
    await request.delete(`/erp/sale-orders/draft/${row.id}`, {
      data: { reason: '删除销售单草稿' },
      skipDeleteReasonPrompt: true
    } as any);
    notifySuccess();
    invalidateSaleListRequestCache();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') notifyError(error);
  }
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

onMounted(async () => {
  markErpNavigationPerf('sale-order-list:mounted', {
    page: 'draft'
  });
  await waitForSaleListFirstPaint();
  if (pageSizeSyncReady.value) {
    runRouteRefresh();
  } else {
    pendingRouteRefresh.value = true;
  }
  showDeferredPanel.value = true;
  window.addEventListener('keydown', handleKeydown);
});

onActivated(() => {
  window.removeEventListener('keydown', handleKeydown);
  window.addEventListener('keydown', handleKeydown);
});

onDeactivated(() => {
  window.removeEventListener('keydown', handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
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
    if (route.path !== '/erp/sale-orders/draft') return;
    if (!pageSizeSyncReady.value) {
      pendingRouteRefresh.value = true;
      return;
    }
    runRouteRefresh();
  }
);
</script>
