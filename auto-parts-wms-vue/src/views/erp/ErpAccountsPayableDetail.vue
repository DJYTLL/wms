<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpAccountsPayableDetail') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar detail-toolbar">
          <div class="detail-toolbar__summary">
            <div class="detail-toolbar__item">
              <span class="detail-toolbar__label">{{ $t('field.orderNo') }}</span>
              <span class="detail-toolbar__value">{{ detail.payable.orderNo || '-' }}</span>
            </div>
            <div class="detail-toolbar__item">
              <span class="detail-toolbar__label">{{ $t('field.supplier') }}</span>
              <span class="detail-toolbar__value">{{ detail.supplierName || '-' }}</span>
            </div>
            <div class="detail-toolbar__item">
              <span class="detail-toolbar__label">{{ $t('field.status') }}</span>
              <el-tag :type="apStatusTagType(detail.payable.status)" size="small">
                {{ apStatusLabel(detail.payable.status) }}
              </el-tag>
            </div>
          </div>
          <div class="table-actions detail-toolbar__actions">
            <el-button @click="goBack">{{ $t('action.back') }}</el-button>
            <el-button
              v-if="canPrint"
              type="primary"
              @click="handlePrint"
            >
              {{ $t('action.print') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="$t('field.orderNo')">{{ detail.payable.orderNo }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.supplier')">{{ detail.supplierName }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.totalAmount')">{{ detail.payable.totalAmount }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.paidAmount')">{{ totalPaidAmount }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.discountAmount')">{{ totalDiscountAmount }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.unpaidAmount')">{{ detail.payable.unpaidAmount }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.status')">
            <el-tag :type="apStatusTagType(detail.payable.status)" size="small">
              {{ apStatusLabel(detail.payable.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.settlementMethod')">{{ detail.payable.settlementMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.remark')">{{ detail.payable.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <div class="table-card payments-card">
      <div class="table-body">
        <div class="section-title">{{ $t('page.erpPaymentManagement') }}</div>
        <el-table :data="detail.payments" style="width: 100%" stripe :empty-text="$t('table.empty')" :row-class-name="paymentRowClass">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column prop="paymentNo" :label="$t('field.paymentNo')" min-width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPayment(row)">{{ row.paymentNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="paymentStatusTagType(row.status)" size="small">
                {{ paymentStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" :label="$t('field.paymentAmount')" min-width="140" />
          <el-table-column prop="discountAmount" :label="$t('field.discountAmount')" min-width="140" />
          <el-table-column :label="$t('field.redFlushReason')" min-width="200">
            <template #default="{ row }">
              <span v-if="row.status === 'RED_FLUSHED'">{{ extractRedFlushReason(row.remark) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
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
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useAuthStore } from '@/stores/auth';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';

const route = useRoute();
const router = useRouter();
const { notifyError, notifyWarning } = useApiError();
const authStore = useAuthStore();
const { t } = useI18n();

const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const detail = reactive({
  payable: {
    orderNo: '',
    totalAmount: '',
    paidAmount: '',
    unpaidAmount: '',
    status: '',
    settlementMethod: '',
    remark: ''
  },
  supplierName: '-',
  payments: [] as any[]
});

const totalPaidAmount = computed(() => {
  const sum = detail.payments.reduce((acc, item) => {
    const value = Number(item?.amount ?? 0);
    return acc + (Number.isNaN(value) ? 0 : value);
  }, 0);
  return Number.isFinite(sum) ? sum : 0;
});

const totalDiscountAmount = computed(() => {
  const sum = detail.payments.reduce((acc, item) => {
    const value = Number(item?.discountAmount ?? 0);
    return acc + (Number.isNaN(value) ? 0 : value);
  }, 0);
  return Number.isFinite(sum) ? sum : 0;
});

const closePage = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path: route.path } }));
  }
};

const goBack = () => {
  closePage();
  router.push('/erp/ap');
};

const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canPrint = computed(() => hasPermission('erp-ap:view'));

const handlePrint = () => {
  const id = route.params.id;
  if (!id) return;
  printDocId.value = Number(id);
  printDialogVisible.value = true;
};

const fetchDetail = async () => {
  try {
    const res: any = await request.get(`/erp/ap/${route.params.id}`);
    if (res.data.code === 200) {
      Object.assign(detail, res.data.data);
    }
  } catch (error) {
    notifyError(error);
  }
};

const openPayment = (row: any) => {
  if (!row?.id) {
    notifyWarning(t('message.noData'));
    return;
  }
  if (row?.status === 'DRAFT') {
    router.push(`/erp/payments/${row.id}/edit`);
    return;
  }
  router.push(`/erp/payments/${row.id}`);
};

const paymentRowClass = ({ row }: { row: any }) => {
  return row.status === 'RED_FLUSHED' ? 'row-red-flush' : '';
};

const paymentStatusLabel = (status: string) => {
  if (status === 'DRAFT') return t('status.draft');
  if (status === 'APPROVED') return t('status.approved');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return status || '-';
};

const paymentStatusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const apStatusLabel = (status: string) => {
  if (status === 'OPEN') return t('status.open');
  if (status === 'SETTLED') return t('status.approved');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return status || '-';
};

const apStatusTagType = (status: string) => {
  if (status === 'SETTLED') return 'success';
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

const extractRedFlushReason = (remark?: string) => {
  if (!remark) return '-';
  const marker = '红冲原因：';
  const idx = remark.indexOf(marker);
  if (idx >= 0) {
    return remark.slice(idx + marker.length).trim() || '-';
  }
  return remark;
};

onMounted(() => {
  fetchDetail();
});
</script>

<style scoped>
:deep(.detail-toolbar) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.detail-toolbar__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, max-content));
  align-items: center;
  gap: 12px 24px;
  min-width: 0;
}

.detail-toolbar__item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.detail-toolbar__label {
  color: #6b7280;
  font-size: 13px;
  white-space: nowrap;
}

.detail-toolbar__value {
  color: #1f2937;
  font-weight: 500;
  min-width: 0;
  word-break: break-all;
}

:deep(.detail-toolbar__actions) {
  flex-wrap: nowrap;
  justify-content: flex-end;
  margin-left: 0;
}

:deep(.payments-card .table-body) {
  max-height: 320px;
  overflow: auto;
}

:deep(.row-red-flush > td) {
  background-color: rgba(255, 77, 79, 0.12) !important;
}

:deep(.row-red-flush:hover > td) {
  background-color: rgba(255, 77, 79, 0.12) !important;
}

@media (max-width: 1280px) {
  :deep(.detail-toolbar) {
    grid-template-columns: minmax(0, 1fr);
  }

  .detail-toolbar__summary {
    grid-template-columns: repeat(2, minmax(0, max-content));
  }

  :deep(.detail-toolbar__actions) {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .detail-toolbar__summary {
    grid-template-columns: 1fr;
  }

  :deep(.detail-toolbar__actions) {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
