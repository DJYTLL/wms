<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ pageTitle }}</div>
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
          </div>
          <div class="table-actions">
            <el-button
              v-if="canCreate"
              type="primary"
              @click="openCreatePage"
            >
              {{ $t('action.add') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card" :class="{ 'sale-approved-card': isApprovedPage }">
      <div class="table-body">
        <ErpDataTable
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
         table-key="erp-purchase-return-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
          <ErpDataTableColumn v-if="canShow('supplier')" :label="$t('field.supplier')" min-width="160" column-key="supplier">
            <template #default="{ row }">
              {{ getSupplierName(row.supplierId) }}
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
                  v-if="canPrint"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-if="canCopy"
                  @click="handleCopy(row)"
                >
                  {{ $t('action.copy') }}
                </el-button>
                <el-button
                  v-if="row.status === 'APPROVED' && canCancel"
                  link
                  type="danger"
                  size="small"
                  @click="handleCancel(row)"
                >
                  {{ $t('action.cancel') }}
                </el-button>
              </template>
              <template v-else>
                <el-button
                  v-if="row.status === 'DRAFT' && canEdit"
                  link
                  type="primary"
                  size="small"
                  @click="openEditPage(row)"
                >
                  {{ $t('action.edit') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-if="canPrint"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT' && canApprove"
                  link
                  type="success"
                  size="small"
                  @click="handleApprove(row)"
                >
                  {{ $t('action.approve') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT' && canDelete"
                  link
                  type="danger"
                  size="small"
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
      :title="$t('page.erpPurchaseReturnPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';
import { useAuthStore } from '@/stores/auth';
import { getCachedSuppliers, invalidateErpBaseDataCache } from '@/composables/erpBaseDataCache';
import { createInflightRequestDeduper } from '@/composables/inflightRequestDeduperCore';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface PurchaseReturn {
  id: number;
  orderNo?: string;
  supplierId?: number;
  status: string;
  totalAmount?: number;
  createdAt?: string;
}

const props = defineProps<{
  workspace?: 'draft' | 'approved'
}>();

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();

const searchQuery = ref('');
const statusFilter = ref('');
const statusLocked = ref(false);
const supplierFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<PurchaseReturn[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const initializedRoutePath = ref('');
const pageSizeSyncReady = ref(false);
const pendingRouteRefresh = ref(false);
const listRequestDeduper = createInflightRequestDeduper();

const supplierOptions = ref<OptionItem[]>([]);

const isPurchaseReturnRoute = computed(() => route.path.startsWith('/erp/purchase-returns'));
const currentWorkspace = computed<'draft' | 'approved'>(() => {
  if (props.workspace) return props.workspace;
  if (route.path.includes('/erp/purchase-returns/approved')) return 'approved';
  return 'draft';
});
const isCurrentWorkspaceRoute = computed(() => {
  if (!isPurchaseReturnRoute.value) return false;
  if (props.workspace === 'draft') return route.path === '/erp/purchase-returns/draft';
  if (props.workspace === 'approved') return route.path === '/erp/purchase-returns/approved';
  return route.path === '/erp/purchase-returns/draft' || route.path === '/erp/purchase-returns/approved';
});
const isDraftPage = computed(() => currentWorkspace.value === 'draft');
const isApprovedPage = computed(() => currentWorkspace.value === 'approved');
const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const listEndpoint = computed(() => isApprovedPage.value ? '/erp/purchase-returns/approved/page' : '/erp/purchase-returns/draft/page');
const detailEndpoint = computed(() => isApprovedPage.value ? '/erp/purchase-returns/approved' : '/erp/purchase-returns/draft');
const printDocType = computed<'PURCHASE_RETURN_APPROVED' | 'PURCHASE_RETURN_DRAFT'>(() => isApprovedPage.value ? 'PURCHASE_RETURN_APPROVED' : 'PURCHASE_RETURN_DRAFT');
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');
const canCreate = computed(() => isDraftPage.value && hasPermission('erp-purchase-return-draft:add'));
const canEdit = computed(() => isDraftPage.value && hasPermission('erp-purchase-return-draft:edit'));
const canDelete = computed(() => isDraftPage.value && hasPermission('erp-purchase-return-draft:delete'));
const canApprove = computed(() => isDraftPage.value && hasPermission('erp-purchase-return-draft:approve'));
const canPrint = computed(() => isApprovedPage.value
  ? hasPermission('erp-purchase-return-approved:print')
  : hasPermission('erp-purchase-return-draft:print'));
const canCopy = computed(() => isApprovedPage.value
  && hasPermission('erp-purchase-return-approved:copy')
  && hasPermission('erp-purchase-return-draft:add'));
const canCancel = computed(() => isApprovedPage.value && hasPermission('erp-purchase-return-approved:cancel'));

const statusOptions = computed(() => ([
  { value: 'DRAFT', label: t('status.draft') },
  { value: 'APPROVED', label: t('status.approved') },
  { value: 'RED_FLUSHED', label: t('status.redFlushed') }
]));

const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined;
  return key ? t(key) : t('page.erpPurchaseReturnManagement');
});

const defaultColumns = ['orderNo', 'supplier', 'status', 'totalAmount', 'createdAt'];
const draftColumnSettings = useColumnSettings('erp-purchase-return-draft', defaultColumns);
const approvedColumnSettings = useColumnSettings('erp-purchase-return-approved', defaultColumns);

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
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (supplierFilter.value) params.supplierId = supplierFilter.value;
    if (dateRange.value && dateRange.value.length === 2) {
      const start = Number(dateRange.value[0]);
      const end = Number(dateRange.value[1]);
      params.startAt = start;
      params.endAt = end;
    }

    const requestKey = `${listEndpoint.value}?${JSON.stringify(params)}`;
    const res: any = await listRequestDeduper.run(requestKey, () => (
      request.get(listEndpoint.value, { params })
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
  if (!isPurchaseReturnRoute.value) {
    statusLocked.value = false;
    statusFilter.value = '';
    return;
  }
  const defaultStatus = route.meta.defaultStatus as string | undefined;
  const lockStatus = route.meta.lockStatus === true;
  statusLocked.value = lockStatus;
  if (defaultStatus) {
    statusFilter.value = defaultStatus === 'APPROVED' ? 'APPROVED,RED_FLUSHED' : defaultStatus;
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
  const isFirstInitForCurrentPath = initializedRoutePath.value !== route.fullPath;
  initializedRoutePath.value = route.fullPath;
  applyRouteStatus();
  if (isFirstInitForCurrentPath) {
    tableData.value = [];
    total.value = 0;
  }
  void fetchSuppliers();
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
  const query = isDraftPage.value ? { from: 'draft' } : undefined;
  router.push({ path: '/erp/purchase-returns/draft/create', query });
};

const openEditPage = (row: PurchaseReturn) => {
  const query = isDraftPage.value ? { from: 'draft' } : undefined;
  router.push({ path: `/erp/purchase-returns/draft/${row.id}/edit`, query });
};

const openViewPage = (row: PurchaseReturn) => {
  router.push({ path: `/erp/purchase-returns/approved/${row.id}`, query: { mode: 'view', from: 'approved' } });
};

const handleApprove = async (row: PurchaseReturn) => {
  try {
    await request.post(`/erp/purchase-returns/draft/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleCopy = async (row: PurchaseReturn) => {
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
    const createRes: any = await request.post(`/erp/purchase-returns/approved/${row.id}/copy`);
    if (createRes.data.code === 200) {
      const data = createRes.data.data || {};
      const newId = data.order?.id || data.id;
      notifySuccess();
      if (newId) {
        await router.push({ path: `/erp/purchase-returns/draft/${newId}/edit`, query: { from: 'draft' } });
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: PurchaseReturn) => {
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
    await request.delete(`/erp/purchase-returns/draft/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleCancel = async (row: PurchaseReturn) => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('message.enterRedFlushReason'),
      t('action.cancel'),
      {
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel'),
        inputPattern: /\S+/,
        inputErrorMessage: t('message.enterRedFlushReason'),
        type: 'warning'
      }
    );
    await request.post(`/erp/purchase-returns/approved/${row.id}/cancel`, { reason: value });
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const openPrintPage = (row: PurchaseReturn) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
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

.sale-approved-card .table-body {
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
