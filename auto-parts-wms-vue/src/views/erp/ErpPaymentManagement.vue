<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpPaymentManagement') }}</h2>
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
        <el-button type="primary" v-permission="'erp-payment:add'" @click="openCreate">{{ $t('action.add') }}</el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe :empty-text="$t('table.empty')" :row-class-name="paymentRowClass">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column prop="paymentNo" :label="$t('field.paymentNo')" min-width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">{{ row.paymentNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="supplierName" :label="$t('field.supplier')" min-width="160" />
          <el-table-column prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" :label="$t('field.paymentAmount')" min-width="140" />
          <el-table-column prop="discountAmount" :label="$t('field.discountAmount')" min-width="140" />
          <el-table-column prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="260">
            <template #default="{ row }">
              <el-button
                v-permission="'erp-payment:view'"
                type="primary"
                size="small"
                @click="openPrintPage(row)"
              >
                {{ $t('action.print') }}
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                v-permission="'erp-payment:approve'"
                type="success"
                size="small"
                @click="approveRow(row)"
              >
                {{ $t('action.approve') }}
              </el-button>
              <el-button
                v-if="row.status === 'APPROVED'"
                v-permission="'erp-payment:red-flush'"
                type="danger"
                size="small"
                @click="redFlushRow(row)"
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
      doc-type="PAYMENT"
      :doc-id="printDocId"
      :title="$t('page.erpPaymentPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
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
const loading = ref(false);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const { notifyError } = useApiError();

const statusOptions = [
  { value: 'DRAFT', label: t('status.draft') },
  { value: 'APPROVED', label: t('status.approved') },
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
  loading.value = true;
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
    const res: any = await request.get('/erp/payments/page', { params });
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

const openCreate = () => {
  router.push('/erp/payments/create');
};

const openDetail = (row: any) => {
  if (row.status === 'DRAFT') {
    router.push(`/erp/payments/${row.id}/edit`);
    return;
  }
  router.push(`/erp/payments/${row.id}`);
};

const openPrintPage = (row: any) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const statusLabel = (status: string) => {
  if (status === 'DRAFT') return t('status.draft');
  if (status === 'APPROVED') return t('status.approved');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return status || '-';
};

const statusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const paymentRowClass = ({ row }: { row: any }) => {
  return row.status === 'RED_FLUSHED' ? 'row-red-flush' : '';
};

const approveRow = async (row: any) => {
  try {
    const res: any = await request.post(`/erp/payments/${row.id}/approve`);
    if (res.data.code === 200) {
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const redFlushRow = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('message.confirmRedFlush'),
      t('action.redFlush'),
      { inputPlaceholder: t('placeholder.required'), confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel') }
    );
    if (!value || !String(value).trim()) {
      return;
    }
    const res: any = await request.post(`/erp/payments/${row.id}/red-flush`, { reason: String(value).trim() });
    if (res.data.code === 200) {
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
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
  flex: 0 0 380px;
}

:deep(.table-date-range--compact.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.table-date-range--compact .el-range-input) {
  width: 132px;
}

:deep(.row-red-flush > td) {
  background-color: rgba(255, 77, 79, 0.12) !important;
}

:deep(.row-red-flush:hover > td) {
  background-color: rgba(255, 77, 79, 0.12) !important;
}
</style>
