<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpPurchaseOrderApproved') }}</div>
      <div class="erp-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="erp-toolbar__search erp-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <FuzzyProductSelect
              v-model="supplierFilter"
              :options="supplierOptions"
              :placeholder="$t('field.supplier')"
              class="erp-toolbar__search erp-toolbar__search--wide"
              @change="handleSearch"
            />
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              value-format="x"
              format="YYYY-MM-DD HH:mm:ss"
              :start-placeholder="$t('field.startTime')"
              :end-placeholder="$t('field.endTime')"
              class="erp-toolbar__date-range table-date-range--compact"
              @change="handleSearch"
            />
          </div>
          <div class="table-actions"></div>
        </div>
      </div>
    </div>

    <div class="table-card purchase-approved-card">
      <div class="table-body">
        <ErpDataTable
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="rowClassName"
         table-key="erp-purchase-order-approved">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="170" />
          <ErpDataTableColumn v-if="canShow('supplier')" :label="$t('field.supplier')" min-width="170" show-overflow-tooltip column-key="supplier">
            <template #default="{ row }">
              {{ getSupplierName(row.supplierId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.orderTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('totalAmount')" prop="totalAmount" :label="$t('field.totalAmount')" min-width="130" align="right">
            <template #default="{ row }">
              <span class="amount-text">{{ formatAmount(row.totalAmount) }}</span>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="220" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openViewPage(row)">{{ $t('action.view') }}</el-button>
              <el-button link type="primary" size="small" v-permission="'erp-purchase-approved:print'" @click="openPrintPage(row)">
                {{ $t('action.print') }}
              </el-button>
              <el-button v-if="canCopy" link type="primary" size="small" @click="handleCopy(row)">
                {{ $t('action.copy') }}
              </el-button>
              <el-button
                v-if="row.status === 'APPROVED'"
                link
                type="warning"
                size="small"
                v-permission="'erp-purchase-approved:unapprove'"
                @click="handleUnapprove(row)"
              >
                {{ $t('action.unapprove') }}
              </el-button>
              <el-button
                v-if="row.status === 'APPROVED'"
                link
                type="danger"
                size="small"
                v-permission="'erp-purchase-approved:cancel'"
                @click="handleCancel(row)"
              >
                {{ $t('action.redFlush') }}
              </el-button>
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
      doc-type="PURCHASE_ORDER_APPROVED"
      :doc-id="printDocId"
      :title="$t('page.erpPurchaseOrderPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useRoute } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { getCachedSuppliers, invalidateErpBaseDataCache } from '@/composables/erpBaseDataCache';
import { createInflightRequestDeduper } from '@/composables/inflightRequestDeduperCore';
import { useAuthStore } from '@/stores/auth';

interface OptionItem {
  id: number;
  name: string;
}

interface PurchaseOrder {
  id: number;
  orderNo?: string;
  supplierId?: number;
  status: string;
  totalAmount?: number;
  createdAt?: string;
}

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();

const searchQuery = ref('');
const supplierFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<PurchaseOrder[]>([]);
const supplierOptions = ref<OptionItem[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const initializedRoutePath = ref('');
const pageSizeSyncReady = ref(false);
const pendingRouteRefresh = ref(false);
const listRequestDeduper = createInflightRequestDeduper();

const defaultColumns = ['orderNo', 'supplier', 'status', 'totalAmount', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-purchase-approved', defaultColumns);
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');
const isCurrentWorkspaceRoute = computed(() => route.path === '/erp/purchase-orders/approved');

const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canCopy = computed(() =>
  hasPermission('erp-purchase-approved:copy')
  && hasPermission('erp-purchase-draft:add')
);

const canShow = (key: string) => isVisible(key);

const formatStatus = (status: string) => {
  const mapping: Record<string, string> = {
    DRAFT: t('status.draft'),
    APPROVED: t('status.approved'),
    CANCELLED: t('status.cancelled')
  };
  return mapping[status] || status;
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

const formatAmount = (value?: number | string) => Number(value || 0).toFixed(2);

const getSupplierName = (id?: number) => supplierOptions.value.find(item => item.id === id)?.name || '-';

const fetchSuppliers = async () => {
  if (!isCurrentWorkspaceRoute.value || supplierOptions.value.length > 0) {
    return;
  }
  try {
    supplierOptions.value = await getCachedSuppliers(tenantCacheKey.value);
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
      size: size.value,
      status: 'APPROVED,CANCELLED'
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (supplierFilter.value) params.supplierId = supplierFilter.value;
    if (dateRange.value?.length === 2) {
      params.startAt = Number(dateRange.value[0]);
      params.endAt = Number(dateRange.value[1]);
    }
    const requestKey = `/erp/purchase-orders/approved/page?${JSON.stringify(params)}`;
    const res: any = await listRequestDeduper.run(requestKey, () => (
      request.get('/erp/purchase-orders/approved/page', { params })
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

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const runRouteRefresh = () => {
  if (!isCurrentWorkspaceRoute.value) {
    return;
  }
  const isFirstInitForCurrentPath = initializedRoutePath.value !== route.fullPath;
  initializedRoutePath.value = route.fullPath;
  if (isFirstInitForCurrentPath) {
    tableData.value = [];
    total.value = 0;
  }
  void fetchSuppliers();
  void fetchTenantKeys();
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

const openViewPage = (row: PurchaseOrder) => {
  router.push({
    path: `/erp/purchase-orders/approved/${row.id}`,
    query: {
      mode: 'view',
      returnTo: '/erp/purchase-orders/approved',
      from: 'approved'
    }
  });
};

const openPrintPage = (row: PurchaseOrder) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const handleCancel = async (row: PurchaseOrder) => {
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
    if (!value || !String(value).trim()) return;
    await request.post(`/erp/purchase-orders/approved/${row.id}/cancel`, { reason: String(value).trim() });
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleCopy = async (row: PurchaseOrder) => {
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
    const createRes: any = await request.post(`/erp/purchase-orders/approved/${row.id}/copy`);
    if (createRes.data.code === 200) {
      const data = createRes.data.data || {};
      const newId = data.order?.id || data.id;
      notifySuccess();
      if (newId) {
        await router.push({
          path: `/erp/purchase-orders/draft/${newId}/edit`,
          query: {
            from: 'draft',
            returnTo: '/erp/purchase-orders/draft'
          }
        });
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleUnapprove = async (row: PurchaseOrder) => {
  try {
    await ElMessageBox.confirm(
      t('message.confirmUnapprove'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.unapprove'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
    await request.post(`/erp/purchase-orders/approved/${row.id}/unapprove`);
    notifySuccess();
    await router.push({
      path: `/erp/purchase-orders/draft/${row.id}/edit`,
      query: {
        from: 'draft',
        returnTo: '/erp/purchase-orders/draft'
      }
    });
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const rowClassName = ({ row }: { row: PurchaseOrder }) => {
  if (row.status === 'CANCELLED') return 'row-red-flushed';
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
    supplierOptions.value = [];
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
:deep(.erp-toolbar__search--wide) {
  width: 220px;
}

:deep(.erp-toolbar__date-range) {
  width: 380px;
}

:deep(.table-date-range--compact) {
  flex: 0 0 380px;
}

:deep(.table-date-range--compact.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.table-date-range--compact .el-range-input) {
  width: 132px;
}

:deep(.row-red-flushed > td) {
  background-color: #fff1f0 !important;
}

:deep(.row-red-flushed:hover > td) {
  background-color: #fff1f0 !important;
}

.purchase-approved-card .table-body {
  max-height: 100%;
  overflow: auto;
}

.erp-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-sizing: border-box;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.table-filters {
  display: grid;
  grid-template-columns: 220px 220px 380px;
  align-items: center;
  justify-content: start;
  gap: 12px;
  min-width: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
}

.amount-text {
  color: #1677ff;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .erp-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: 1fr;
  }

  .table-filters {
    grid-template-columns: 200px 200px 360px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.erp-toolbar__search--wide) {
    width: 200px;
  }

  :deep(.erp-toolbar__date-range) {
    width: 360px;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 360px;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 360px !important;
    min-width: 360px !important;
  }
}

@media (max-width: 768px) {
  .table-filters {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }

  :deep(.erp-toolbar__search--wide),
  :deep(.erp-toolbar__date-range) {
    width: 100%;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 100%;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
