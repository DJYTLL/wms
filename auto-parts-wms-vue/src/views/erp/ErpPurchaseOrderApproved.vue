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
        <el-table
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="rowClassName"
        >
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="170" />
          <el-table-column v-if="canShow('supplier')" :label="$t('field.supplier')" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">
              {{ getSupplierName(row.supplierId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.orderTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('totalAmount')" prop="totalAmount" :label="$t('field.totalAmount')" min-width="130" align="right">
            <template #default="{ row }">
              <span class="amount-text">{{ formatAmount(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('status')" prop="status" :label="$t('field.status')" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openViewPage(row)">{{ $t('action.view') }}</el-button>
              <el-button link type="primary" size="small" v-permission="'erp-purchase:view'" @click="openPrintPage(row)">
                {{ $t('action.print') }}
              </el-button>
              <el-button link type="primary" size="small" v-permission="'erp-purchase:add'" @click="handleCopy(row)">
                {{ $t('action.copy') }}
              </el-button>
              <el-button
                v-if="row.status === 'APPROVED'"
                link
                type="danger"
                size="small"
                v-permission="'erp-purchase:cancel'"
                @click="handleCancel(row)"
              >
                {{ $t('action.redFlush') }}
              </el-button>
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
      doc-type="PURCHASE_ORDER"
      :doc-id="printDocId"
      :title="$t('page.erpPurchaseOrderPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

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
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

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

const defaultColumns = ['orderNo', 'supplier', 'status', 'totalAmount', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-purchase-approved', defaultColumns);

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
  try {
    const res: any = await request.get('/erp/suppliers');
    supplierOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
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
    const res: any = await request.get('/erp/purchase-orders/page', { params });
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
    path: `/erp/purchase-orders/${row.id}/edit`,
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
    await request.post(`/erp/purchase-orders/${row.id}/cancel`, { reason: String(value).trim() });
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
    const detailRes: any = await request.get(`/erp/purchase-orders/${row.id}`);
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
    const orderNoRes: any = await request.get('/erp/purchase-orders/next-order-no');
    const orderNo = orderNoRes.data?.data || '';
    const createRes: any = await request.post('/erp/purchase-orders', {
      orderNo,
      orderAt: order.orderAt,
      supplierId: order.supplierId,
      paymentMethodCode: order.paymentMethodCode || undefined,
      paidAmount: order.paidAmount,
      discountAmount: order.discountAmount,
      remark: order.remark,
      items
    });
    if (createRes.data.code === 200) {
      const data = createRes.data.data || {};
      const newId = data.order?.id || data.id;
      notifySuccess();
      if (newId) {
        await router.push({
          path: `/erp/purchase-orders/${newId}/edit`,
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

const rowClassName = ({ row }: { row: PurchaseOrder }) => {
  if (row.status === 'CANCELLED') return 'row-red-flushed';
  return '';
};

onMounted(() => {
  fetchSuppliers();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchSuppliers();
  fetchList();
});
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
