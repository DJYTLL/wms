<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpAccountsPayableManagement') }}</h2>
      <div class="table-toolbar">
        <div class="table-filters">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="table-search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <FuzzyProductSelect
            v-model="supplierFilter"
            :options="supplierOptions"
            :placeholder="$t('field.supplier')"
            class="table-search"
            @change="handleSearch"
          />
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search" clearable @change="handleSearch">
            <el-option :label="$t('filter.all')" value="" />
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="x"
            format="YYYY/MM/DD HH:mm:ss"
            :start-placeholder="$t('field.startTime')"
            :end-placeholder="$t('field.endTime')"
            @change="handleSearch"
            class="table-date-range--compact"
          />
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column prop="supplierName" :label="$t('field.supplier')" min-width="160" />
          <el-table-column prop="orderNo" :label="$t('field.orderNo')" min-width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">{{ row.orderNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
          <el-table-column :label="$t('field.paidAmount')" min-width="140">
            <template #default="{ row }">
              {{ formatAmount(toAmount(row.paidAmount)) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('field.discountAmount')" min-width="140">
            <template #default="{ row }">
              {{ formatAmount(toAmount(row.discountAmount)) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('field.unpaidAmount')" min-width="140">
            <template #default="{ row }">
              {{ formatAmount(toAmount(row.unpaidAmount)) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="160">
            <template #default="{ row }">
              <el-button
                v-permission="'erp-ap:view'"
                type="primary"
                size="small"
                @click="openPrintPage(row)"
              >
                {{ $t('action.print') }}
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
      doc-type="ACCOUNTS_PAYABLE"
      :doc-id="printDocId"
      :title="$t('page.erpAccountsPayablePrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';

interface OptionItem {
  id: number;
  name: string;
}

const { t } = useI18n();
const router = useRouter();

const searchQuery = ref('');
const statusFilter = ref('');
const supplierFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<any[]>([]);
const supplierOptions = ref<OptionItem[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const { notifyError } = useApiError();
const statusOptions = [
  { value: 'OPEN', label: t('status.open') },
  { value: 'SETTLED', label: t('status.approved') },
  { value: 'RED_FLUSHED', label: t('status.redFlushed') }
];

const fetchSuppliers = async () => {
  try {
    const res: any = await request.get('/erp/suppliers');
    if (res.data.code === 200) {
      supplierOptions.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value) params.status = statusFilter.value;
    if (supplierFilter.value) params.supplierId = supplierFilter.value;
    if (dateRange.value && dateRange.value.length === 2) {
      params.startAt = Number(dateRange.value[0]);
      params.endAt = Number(dateRange.value[1]);
    }
    const res: any = await request.get('/erp/ap/page', { params });
    if (res.data.code === 200) {
      tableData.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
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

const openDetail = (row: any) => {
  router.push(`/erp/ap/${row.id}`);
};

const openPrintPage = (row: any) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const statusLabel = (status: string) => {
  if (status === 'OPEN') return t('status.open');
  if (status === 'SETTLED') return t('status.approved');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return status || '-';
};

const statusTagType = (status: string) => {
  if (status === 'SETTLED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const toAmount = (value: any) => {
  if (value == null || value === '') return 0;
  const num = Number(value);
  return Number.isNaN(num) ? 0 : num;
};

const formatAmount = (value: number) => {
  return value.toFixed(2);
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

onMounted(() => {
  fetchSuppliers();
  fetchList();
});

onActivated(() => {
  fetchSuppliers();
  fetchList();
});
</script>

<style scoped>
:deep(.table-date-range--compact) {
  flex: 0 0 280px;
}

:deep(.table-date-range--compact.el-range-editor) {
  width: 280px !important;
  min-width: 280px !important;
}

:deep(.table-date-range--compact .el-range-input) {
  width: 86px;
}
</style>
