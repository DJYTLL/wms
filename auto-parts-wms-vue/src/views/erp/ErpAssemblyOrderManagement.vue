<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpAssembleOrderManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar">
          <div class="table-filters inventory-filters inventory-filters--assembly">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="inventory-field--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="inventory-field--narrow" clearable @change="handleSearch">
            <el-option :label="$t('filter.all')" value="" />
            <el-option :label="$t('status.draft')" value="DRAFT" />
            <el-option :label="$t('status.approved')" value="APPROVED" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="x"
            format="YYYY-MM-DD HH:mm"
            :start-placeholder="$t('field.startTime')"
            :end-placeholder="$t('field.endTime')"
            class="inventory-date-range"
            @change="handleSearch"
          />
          </div>
          <div class="table-actions inventory-actions">
            <el-button
              type="primary"
              v-permission="'erp-assemble-order:add'"
              @click="openCreatePage"
            >
              {{ $t('action.add') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
         table-key="erp-assembly-order-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
          <ErpDataTableColumn v-if="canShow('orderType')" prop="orderType" :label="$t('field.orderType')" width="140">
            <template #default="{ row }">
              {{ formatOrderType(row.orderType) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('finishedProduct')" :label="$t('field.finishedProduct')" min-width="180" column-key="finishedProduct">
            <template #default="{ row }">
              {{ row.finishedProductName || '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('sourceSaleOrder')" prop="sourceSaleOrderNo" label="来源销售单" min-width="160">
            <template #default="{ row }">
              {{ row.sourceSaleOrderNo || '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('customerName')" prop="customerName" label="客户" min-width="150">
            <template #default="{ row }">
              {{ row.customerName || '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('finishedQty')" prop="finishedQty" :label="$t('field.finishedQty')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('totalCost')" prop="totalCost" :label="$t('field.totalCost')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('orderAt')" prop="orderAt" :label="$t('field.orderTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.orderAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="240" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                @click="openViewPage(row)"
              >
                {{ $t('action.view') }}
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                link
                type="primary"
                size="small"
                v-permission="'erp-assemble-order:edit'"
                @click="openEditPage(row)"
              >
                {{ $t('action.edit') }}
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                link
                type="success"
                size="small"
                v-permission="'erp-assemble-order:approve'"
                @click="handleApprove(row)"
              >
                {{ $t('action.approve') }}
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                link
                type="danger"
                size="small"
                v-permission="'erp-assemble-order:delete'"
                @click="handleDelete(row)"
              >
                {{ $t('action.delete') }}
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';

interface OptionItem {
  id: number;
  name: string;
}

interface AssemblyOrder {
  id: number;
  orderNo: string;
  orderType: string;
  finishedProductId?: number;
  finishedProductName?: string;
  sourceSaleOrderNo?: string;
  customerName?: string;
  finishedQty?: number;
  totalCost?: number;
  status?: string;
  orderAt?: string;
}

const { t } = useI18n();
const router = useRouter();
const { notifyError, notifySuccess } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<AssemblyOrder[]>([]);

const searchQuery = ref('');
const statusFilter = ref('');
const orderTypeFilter = ref('ASSEMBLE');
const dateRange = ref<[string, string] | null>(null);

const defaultColumns = ['orderNo', 'orderType', 'finishedProduct', 'sourceSaleOrder', 'customerName', 'finishedQty', 'totalCost', 'status', 'orderAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-assemble-order', defaultColumns);

const canShow = (key: string) => isVisible(key);

const formatStatus = (value?: string) => {
  if (!value) return '-';
  const key = `status.${value.toLowerCase()}`;
  const translated = t(key);
  return translated === key ? value : translated;
};

const statusTagType = (value?: string) => {
  if (value === 'APPROVED') return 'success';
  if (value === 'DRAFT') return 'info';
  return 'info';
};

const formatOrderType = (value?: string) => {
  if (!value) return '-';
  return value === 'DISASSEMBLE' ? t('assemblyType.disassemble') : t('assemblyType.assemble');
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
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
    if (orderTypeFilter.value) params.orderType = orderTypeFilter.value;
    if (dateRange.value?.length === 2) {
      params.startAt = dateRange.value[0];
      params.endAt = dateRange.value[1];
    }
    const res: any = await request.get('/erp/assembly-orders/page', { params });
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

const openCreatePage = () => {
  router.push('/erp/assemble-orders/create');
};

const openEditPage = (row: AssemblyOrder) => {
  router.push(`/erp/assemble-orders/${row.id}/edit`);
};

const openViewPage = (row: AssemblyOrder) => {
  router.push({ path: `/erp/assemble-orders/${row.id}/view`, query: { mode: 'view' } });
};

const handleApprove = async (row: AssemblyOrder) => {
  try {
    await ElMessageBox.confirm(
      t('message.confirmApprove'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.approve'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
    await request.post(`/erp/assembly-orders/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleDelete = async (row: AssemblyOrder) => {
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
    await request.delete(`/erp/assembly-orders/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

onMounted(() => {
  if (pageSizeSyncReady.value) {
    fetchList();
  } else {
    pendingInitialLoad.value = true;
  }
  fetchTenantKeys();
});

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchList();
});

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
</script>
