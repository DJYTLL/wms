<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title" :class="{ 'page-title--red': detail.payment.status === 'RED_FLUSHED' }">
          {{ $t('page.erpPaymentDetail') }}
        </h2>
        <el-tag
          v-if="detail.payment.status === 'RED_FLUSHED'"
          type="danger"
          size="small"
          class="red-flush-tag"
        >
          {{ statusLabel(detail.payment.status) }}
        </el-tag>
      </div>
      <div class="page-actions">
        <el-button @click="goBack">{{ $t('action.back') }}</el-button>
        <el-button
          v-if="canPrint"
          type="primary"
          @click="handlePrint"
        >
          {{ $t('action.print') }}
        </el-button>
        <el-button
          v-if="canApprove"
          type="success"
          @click="handleApprove"
        >
          {{ $t('action.approve') }}
        </el-button>
        <el-button
          v-if="canRedFlush"
          type="danger"
          @click="handleRedFlush"
        >
          {{ $t('action.redFlush') }}
        </el-button>
      </div>
    </div>

    <div class="table-card" >
      <div class="table-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="$t('field.paymentNo')">{{ detail.payment.paymentNo }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.orderNo')">{{ detail.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.payableNo')">
            <div v-if="detail.payables.length" class="payable-links">
              <el-button
                v-for="item in detail.payables"
                :key="item.payableId"
                link
                type="primary"
                @click="openPayable(item.payableId)"
              >
                {{ item.orderNo || '-' }}
              </el-button>
            </div>
            <el-button
              v-else-if="detail.payment.payableId && detail.payableNo"
              link
              type="primary"
              @click="openPayable(detail.payment.payableId)"
            >
              {{ detail.payableNo }}
            </el-button>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.supplier')">{{ detail.supplierName }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.paymentAmount')">{{ detail.payment.amount }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.discountAmount')">{{ detail.payment.discountAmount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.settlementMethod')">{{ detail.payment.settlementMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.paidAt')">{{ formatDateTime(detail.payment.paidAt) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.status')">
            <el-tag :type="statusTagType(detail.payment.status)" size="small">
              {{ statusLabel(detail.payment.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.redFlushReason')">
            <span v-if="detail.payment.status === 'RED_FLUSHED'">{{ extractRedFlushReason(detail.payment.remark) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.remark')">{{ detail.payment.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
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
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useAuthStore } from '@/stores/auth';
import { ElMessageBox } from 'element-plus';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';

const route = useRoute();
const router = useRouter();
const { notifyError, notifyWarning } = useApiError();
const authStore = useAuthStore();
const { t } = useI18n();

const detail = reactive({
  payment: {
    paymentNo: '',
    payableId: null as number | null,
    amount: '',
    discountAmount: '',
    settlementMethod: '',
    paidAt: '',
    status: '',
    remark: ''
  },
  supplierName: '-',
  orderNo: '',
  payableNo: '',
  payables: [] as Array<{ payableId: number; orderNo?: string; allocatedAmount?: string; allocatedDiscount?: string; allocatedTotal?: string }>
});

const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canPrint = computed(() => hasPermission('erp-payment:view'));

const canApprove = computed(() => {
  return detail.payment.status === 'DRAFT' && hasPermission('erp-payment:approve');
});

const canRedFlush = computed(() => {
  return detail.payment.status === 'APPROVED' && hasPermission('erp-payment:red-flush');
});

const closePage = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path: route.path } }));
  }
};

const goBack = () => {
  closePage();
  router.push('/erp/payments');
};

const handlePrint = () => {
  const id = route.params.id;
  if (!id) return;
  printDocId.value = Number(id);
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

const extractRedFlushReason = (remark?: string) => {
  if (!remark) return '-';
  const marker = '红冲原因：';
  const idx = remark.indexOf(marker);
  if (idx >= 0) {
    return remark.slice(idx + marker.length).trim() || '-';
  }
  return remark;
};

const openPayable = (id: number) => {
  router.push(`/erp/ap/${id}`);
};

const fetchDetail = async () => {
  try {
    const res: any = await request.get(`/erp/payments/${route.params.id}`);
    if (res.data.code === 200) {
      Object.assign(detail, res.data.data);
    }
  } catch (error) {
    const message = (error as any)?.response?.data?.message;
    if (message && String(message).includes('付款单不存在')) {
      notifyWarning(message);
      goBack();
      return;
    }
    notifyError(error);
  }
};

const handleApprove = async () => {
  try {
    const res: any = await request.post(`/erp/payments/${route.params.id}/approve`);
    if (res.data.code === 200) {
      Object.assign(detail, res.data.data);
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleRedFlush = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('message.confirmRedFlush'),
      t('action.redFlush'),
      { inputPlaceholder: t('placeholder.required'), confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel') }
    );
    if (!value || !String(value).trim()) {
      return;
    }
    const res: any = await request.post(`/erp/payments/${route.params.id}/red-flush`, { reason: String(value).trim() });
    if (res.data.code === 200) {
      Object.assign(detail, res.data.data);
    }
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchDetail();
});
</script>

<style scoped>
.card-red-flush {
  border: 1px solid rgba(255, 77, 79, 0.35);
  background: rgba(255, 77, 79, 0.08);
}

.page-title-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.page-title--red {
  color: #cf1322;
}

.red-flush-tag {
  font-weight: 600;
}

.payable-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
