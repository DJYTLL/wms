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
          </div>
          <div class="table-actions">
            <el-button
              v-if="canCreate"
              type="primary"
              v-permission="'erp-sale:add'"
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
        <el-table
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="rowClassName"
        >
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
          <el-table-column v-if="canShow('customer')" :label="$t('field.customer')" min-width="160">
            <template #default="{ row }">
              {{ getCustomerName(row.customerId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('totalAmount')" prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
          <el-table-column v-if="canShow('receivableStatus')" :label="$t('field.receivableStatus')" min-width="150">
            <template #default="{ row }">
              <el-tag :type="financeStatusTagType(row.receivableStatus)" size="small">
                {{ formatFinanceStatus(row.receivableStatus, row.receivableUnpaidAmount) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('returnStatus')" :label="$t('field.returnStatus')" min-width="130">
            <template #default="{ row }">
              <el-tag :type="Number(row.approvedReturnCount || 0) > 0 ? 'warning' : 'info'" size="small">
                {{ formatReturnStatus(row.approvedReturnCount) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="300" fixed="right">
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
                  v-permission="'erp-sale:view'"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale:add'"
                  @click="handleCopy(row)"
                >
                  {{ $t('action.copy') }}
                </el-button>
                <el-button
                  v-if="row.status === 'APPROVED'"
                  link
                  type="danger"
                  size="small"
                  v-permission="'erp-sale:redflush'"
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
                  v-permission="'erp-sale:edit'"
                  @click="openEditPage(row)"
                >
                  {{ $t('action.edit') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale:view'"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="success"
                  size="small"
                  v-permission="'erp-sale:approve'"
                  @click="handleApprove(row)"
                >
                  {{ $t('action.approve') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="danger"
                  size="small"
                  v-permission="'erp-sale:edit'"
                  @click="handleDelete(row)"
                >
                  {{ $t('action.delete') }}
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
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
      doc-type="SALE_ORDER"
      :doc-id="printDocId"
      :title="$t('page.erpSaleOrderPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
  import { useSystemConfig } from '@/composables/useSystemConfig';
  import { useColumnSettings } from '@/composables/useColumnSettings';
  import { useRouter } from 'vue-router';
  import { ElMessageBox } from 'element-plus';
  import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
  import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface SaleOrderItem {
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

interface SaleOrder {
  id: number;
  orderNo?: string;
  customerId?: number;
  status: string;
  totalAmount?: number;
  receivableStatus?: string;
  receivableUnpaidAmount?: number;
  approvedReturnCount?: number;
  createdAt?: string;
}

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const statusFilter = ref('');
const statusLocked = ref(false);
const customerFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<SaleOrder[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const customerOptions = ref<OptionItem[]>([]);
const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const isDraftPage = computed(() => route.meta.defaultStatus === 'DRAFT');
const isApprovedPage = computed(() => route.meta.defaultStatus === 'APPROVED');

const statusOptions = computed(() => {
  const base = [
    { value: 'DRAFT', label: t('status.draft') },
    { value: 'APPROVED', label: t('status.approved') },
    { value: 'CANCELLED', label: t('status.cancelled') },
    { value: 'RED_FLUSHED', label: t('status.redFlushed') }
  ];
  if (isApprovedPage.value) {
    base.unshift({ value: 'APPROVED,RED_FLUSHED', label: `${t('status.approved')}/${t('status.redFlushed')}` });
  }
  return base;
});

const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined;
  return key ? t(key) : t('page.erpSaleOrderManagement');
});

const canCreate = computed(() => {
  const defaultStatus = route.meta.defaultStatus as string | undefined;
  if (defaultStatus === 'APPROVED') {
    return false;
  }
  if (statusLocked.value && statusFilter.value === 'APPROVED') {
    return false;
  }
  return true;
});

const defaultColumns = ['orderNo', 'customer', 'status', 'totalAmount', 'receivableStatus', 'returnStatus', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-sale', defaultColumns);

const canShow = (key: string) => isVisible(key);

const statusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'CANCELLED') return 'danger';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const formatStatus = (status: string) => {
  const mapping: Record<string, string> = {
    DRAFT: t('status.draft'),
    APPROVED: t('status.approved'),
    CANCELLED: t('status.cancelled'),
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
    const unpaid = Number(unpaidAmount || 0);
    return unpaid > 0 ? `${t('status.open')} ${formatAmount(unpaid)}` : t('status.open');
  }
  return status;
};

const formatReturnStatus = (count?: number) => {
  const value = Number(count || 0);
  return value > 0 ? `${t('status.hasReturn')} ${value}` : t('status.noReturn');
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

const getLocationOptions = (warehouseId?: number) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const fetchCustomers = async () => {
  try {
    const res: any = await request.get('/erp/customers');
    customerOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchProducts = async () => {
  try {
    const res: any = await request.get('/erp/products');
    productOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchWarehouses = async () => {
  try {
    const res: any = await request.get('/erp/warehouses');
    warehouseOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchLocations = async () => {
  try {
    const res: any = await request.get('/erp/locations');
    locationOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
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

    const res: any = await request.get('/erp/sale-orders/page', { params });
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
  router.push({ path: '/erp/sale-orders/create', query });
};

const openEditPage = (row: SaleOrder) => {
  const query = isDraftPage.value ? { from: 'draft' } : undefined;
  router.push({ path: `/erp/sale-orders/${row.id}/edit`, query });
};

const openViewPage = (row: SaleOrder) => {
  router.push({ path: `/erp/sale-orders/${row.id}/edit`, query: { mode: 'view' } });
};

const openPrintPage = (row: SaleOrder) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const handleApprove = async (row: SaleOrder) => {
  try {
    await request.post(`/erp/sale-orders/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleRedFlush = async (row: SaleOrder) => {
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
    await request.post(`/erp/sale-orders/${row.id}/red-flush`, { reason: String(value).trim() });
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleCopy = async (row: SaleOrder) => {
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
    const detailRes: any = await request.get(`/erp/sale-orders/${row.id}`);
    const detail = detailRes.data?.data;
    if (!detail?.order) {
      notifyWarning(t('message.noItems'));
      return;
    }
    const order = detail.order;
    const items = (detail.items || []).map((item: any, index: number) => ({
      productId: item.productId,
      warehouseId: item.warehouseId,
      locationId: item.locationId,
      qty: item.qty,
      price: item.price,
      taxRate: item.taxRate,
      remark: item.remark,
      sortNo: index + 1
    }));

    const orderNoRes: any = await request.get('/erp/sale-orders/next-order-no');
    const orderNo = orderNoRes.data?.data || '';

    const payload = {
      orderNo,
      orderAt: order.orderAt,
      customerId: order.customerId,
      settlementMethod: order.settlementMethod,
      deliveryMethod: order.deliveryMethod || undefined,
      paidAmount: order.paidAmount,
      discountAmount: order.discountAmount,
      remark: order.remark,
      items
    };
    const createRes: any = await request.post('/erp/sale-orders', payload);
    if (createRes.data.code === 200) {
      const data = createRes.data.data || {};
      const newId = data.order?.id || data.id;
      notifySuccess();
      if (newId) {
        await router.push({ path: `/erp/sale-orders/${newId}/edit`, query: { from: 'draft' } });
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: SaleOrder) => {
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
    await request.delete(`/erp/sale-orders/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const rowClassName = ({ row }: { row: SaleOrder }) => {
  if (row.status === 'RED_FLUSHED') return 'row-red-flushed';
  return '';
};

onMounted(() => {
  applyRouteStatus();
  fetchCustomers();
  fetchProducts();
  fetchWarehouses();
  fetchLocations();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  applyRouteStatus();
  tableData.value = [];
  total.value = 0;
  fetchCustomers();
  fetchProducts();
  fetchWarehouses();
  fetchLocations();
  fetchList();
});

watch(
  () => route.fullPath,
  () => {
    applyRouteStatus();
    handleSearch();
  },
  { flush: 'sync' }
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
