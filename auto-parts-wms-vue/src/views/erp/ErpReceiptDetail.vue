<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title" :class="{ 'page-title--red': detail.receipt.status === 'RED_FLUSHED' }">
          {{ $t('page.erpReceiptDetail') }}
        </h2>
        <el-tag
          v-if="detail.receipt.status === 'RED_FLUSHED'"
          type="danger"
          size="small"
          class="red-flush-tag"
        >
          {{ statusLabel(detail.receipt.status) }}
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
          <el-descriptions-item :label="$t('field.receiptNo')">{{ detail.receipt.receiptNo }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.orderNo')">{{ detail.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.receivableNo')">
            <div v-if="detail.receivables.length" class="receivable-links">
              <template v-if="canViewSourceReceivables">
                <el-button
                  v-for="item in detail.receivables"
                  :key="item.receivableId"
                  link
                  type="primary"
                  @click="openReceivable(item.receivableId)"
                >
                  {{ item.orderNo || '-' }}
                </el-button>
              </template>
              <span v-else>{{ detail.receivables.map(item => item.orderNo || '-').join('、') }}</span>
            </div>
            <el-button
              v-else-if="canViewSourceReceivables && detail.receipt.receivableId && detail.receivableNo"
              link
              type="primary"
              @click="openReceivable(detail.receipt.receivableId)"
            >
              {{ detail.receivableNo }}
            </el-button>
            <span v-else>{{ detail.receivableNo || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.customer')">{{ detail.customerName }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.receiptAmount')">{{ detail.receipt.amount }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.discountAmount')">{{ detail.receipt.discountAmount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.settlementMethod')">{{ detail.receipt.settlementMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.receivedAt')">{{ formatDateTime(detail.receipt.receivedAt) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('field.status')">
            <el-tag :type="statusTagType(detail.receipt.status)" size="small">
              {{ statusLabel(detail.receipt.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.redFlushReason')">
            <span v-if="detail.receipt.status === 'RED_FLUSHED'">{{ extractRedFlushReason(detail.receipt.remark) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('field.remark')">{{ detail.receipt.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <PrintPreviewDialog
      v-model="printDialogVisible"
      doc-type="RECEIPT"
      :doc-id="printDocId"
      :title="$t('page.erpReceiptPrint')"
    />

    <el-dialog v-model="sourcePreviewVisible" :title="sourcePreviewTitle" width="720px">
      <el-descriptions v-if="sourcePreview" :column="2" border>
        <el-descriptions-item :label="$t('field.orderNo')">{{ sourcePreview.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('field.customer')">{{ sourcePreview.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('field.totalAmount')">{{ sourcePreview.totalAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('field.paidAmount')">{{ sourcePreview.paidAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('field.unpaidAmount')">{{ sourcePreview.unpaidAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('field.status')">{{ sourcePreview.status || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('field.remark')" :span="2">{{ sourcePreview.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
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
  receipt: {
    receiptNo: '',
    receivableId: null as number | null,
    amount: '',
    discountAmount: '',
    settlementMethod: '',
    receivedAt: '',
    status: '',
    remark: ''
  },
  customerName: '-',
  orderNo: '',
  receivableNo: '',
  receivables: [] as Array<{ receivableId: number; orderNo?: string; allocatedAmount?: string; allocatedDiscount?: string; allocatedTotal?: string }>
});

const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const sourcePreviewVisible = ref(false);
const sourcePreview = ref<any>(null);

const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canPrint = computed(() => hasPermission('erp-receipt:view'));
const canViewSourceReceivables = computed(() => hasPermission('erp-receipt:source-view') || hasPermission('erp-ar:view'));
const sourcePreviewTitle = computed(() => {
  if (!sourcePreview.value?.orderNo) return t('page.erpAccountsReceivableManagement');
  return `${t('page.erpAccountsReceivableManagement')} · ${sourcePreview.value.orderNo}`;
});

const canApprove = computed(() => {
  return detail.receipt.status === 'DRAFT' && hasPermission('erp-receipt:approve');
});

const canRedFlush = computed(() => {
  return detail.receipt.status === 'APPROVED' && hasPermission('erp-receipt:red-flush');
});

const closePage = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path: route.path } }));
  }
};

const goBack = () => {
  closePage();
  router.push('/erp/receipts');
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

const openReceivable = (id: number) => {
  if (!canViewSourceReceivables.value) {
    notifyWarning('当前角色缺少来源应收单引用权限');
    return;
  }
  request.get(`/erp/receipts/source-receivables/${id}`).then((res: any) => {
    sourcePreview.value = res.data.data || null;
    sourcePreviewVisible.value = true;
  }).catch((error) => {
    notifyError(error);
  });
};

const fetchDetail = async () => {
  try {
    const res: any = await request.get(`/erp/receipts/${route.params.id}`);
    if (res.data.code === 200) {
      Object.assign(detail, res.data.data);
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleApprove = async () => {
  try {
    const res: any = await request.post(`/erp/receipts/${route.params.id}/approve`);
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
    const res: any = await request.post(`/erp/receipts/${route.params.id}/red-flush`, { reason: String(value).trim() });
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

.receivable-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
