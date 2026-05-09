<template>
  <div class="print-page">
    <div v-if="!isPreview" class="print-toolbar">
      <div class="toolbar-title">{{ $t('page.erpPaymentPrint') }}</div>
      <div class="toolbar-actions">
        <el-button size="small" type="primary" @click="triggerPrint">{{ $t('action.print') }}</el-button>
        <el-button size="small" @click="closeWindow">{{ $t('action.close') }}</el-button>
      </div>
    </div>

    <div class="print-paper" :class="{ 'print-paper--loading': loading }">
      <div class="paper-header">
        <div class="paper-title">{{ headerTitle }}</div>
        <div v-if="subTitle" class="paper-subtitle">{{ subTitle }}</div>
      </div>

      <div class="paper-meta">
        <div v-for="item in headerRows" :key="item.key" class="meta-item">
          <span class="meta-label">{{ item.label }}</span>
          <span class="meta-value">{{ item.value }}</span>
        </div>
      </div>

      <table class="paper-table">
        <thead>
          <tr>
            <th v-for="col in detailColumns" :key="col.key" :style="columnStyle(col)">{{ col.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in items" :key="row.id || index">
            <td v-for="col in detailColumns" :key="col.key" :style="columnStyle(col)">
              {{ formatItemValue(row, col.key) }}
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="showTotals" class="paper-totals">
        <div class="totals-item">
          <span>{{ $t('field.paymentAmount') }}</span>
          <strong>{{ formatMoney(payment?.amount) }}</strong>
        </div>
        <div class="totals-item">
          <span>{{ $t('field.discountAmount') }}</span>
          <strong>{{ formatMoney(payment?.discountAmount) }}</strong>
        </div>
      </div>

      <div v-if="footerNote" class="paper-footer">
        {{ footerNote }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';

interface TemplateConfig {
  headerFields: string[];
  detailColumns: string[];
  showTotals: boolean;
  columnWidths?: Record<string, number>;
}

const { t } = useI18n();
const route = useRoute();
const { notifyError } = useApiError();

const loading = ref(true);
const payment = ref<any>(null);
const items = ref<any[]>([]);
const template = ref<any>(null);
const headerFields = ref<string[]>([]);
const detailColumnKeys = ref<string[]>([]);
const showTotals = ref(true);
const hasAutoPrinted = ref(false);
const columnWidths = ref<Record<string, number>>({});
const supplierName = ref('');
const paymentMethods = ref<any[]>([]);

const paymentId = computed(() => Number(route.params.id));
const isPreview = computed(() => route.query.preview === '1' || route.query.preview === 'true');
const shouldAutoPrint = computed(() => route.query.auto === '1' || route.query.auto === 'true');

const headerTitle = computed(() => template.value?.headerTitle || t('print.paymentTitle'));
const subTitle = computed(() => template.value?.subTitle || '');
const footerNote = computed(() => template.value?.footerNote || t('print.footerNote'));

const headerRows = computed(() => {
  return headerFields.value
    .map((key) => ({
      key,
      label: headerFieldLabel(key),
      value: headerFieldValue(key)
    }))
    .filter((item) => item.label);
});

const detailColumns = computed(() => {
  return detailColumnKeys.value.map((key) => ({
    key,
    label: detailColumnLabel(key),
    width: columnWidths.value[key]
  }));
});

const headerFieldLabel = (key: string) => {
  const mapping: Record<string, string> = {
    paymentNo: t('field.paymentNo'),
    paidAt: t('field.paidAt'),
    supplierName: t('field.supplier'),
    paymentMethod: t('field.paymentMethod'),
    paymentAmount: t('field.paymentAmount'),
    discountAmount: t('field.discountAmount'),
    status: t('field.status'),
    printCount: t('field.printCount'),
    lastPrintedAt: t('field.lastPrintedAt'),
    remark: t('field.remark')
  };
  return mapping[key] || '';
};

const detailColumnLabel = (key: string) => {
  const mapping: Record<string, string> = {
    orderNo: t('field.orderNo'),
    allocatedAmount: t('field.paymentAmount'),
    allocatedDiscount: t('field.discountAllocated'),
    allocatedTotal: t('field.totalAmount')
  };
  return mapping[key] || '';
};

const headerFieldValue = (key: string) => {
  const current = payment.value || {};
  switch (key) {
    case 'paymentNo':
      return current.paymentNo || '-';
    case 'paidAt':
      return formatDateTime(current.paidAt || current.createdAt);
    case 'supplierName':
      return supplierName.value || '-';
    case 'paymentMethod':
      return mapMethodName(paymentMethods.value, current.paymentMethod);
    case 'paymentAmount':
      return formatMoney(current.amount);
    case 'discountAmount':
      return formatMoney(current.discountAmount);
    case 'status':
      return current.status ? statusLabel(current.status) : '-';
    case 'printCount':
      return current.printCount ?? 0;
    case 'lastPrintedAt':
      return formatDateTime(current.lastPrintedAt);
    case 'remark':
      return current.remark || '-';
    default:
      return '-';
  }
};

const statusLabel = (value?: string) => {
  if (!value) return '-';
  if (value === 'DRAFT') return t('status.draft');
  if (value === 'APPROVED') return t('status.approved');
  if (value === 'RED_FLUSHED') return t('status.redFlushed');
  return value;
};

const formatItemValue = (row: any, key: string) => {
  switch (key) {
    case 'orderNo':
      return row.orderNo || '-';
    case 'allocatedAmount':
      return formatMoney(row.allocatedAmount);
    case 'allocatedDiscount':
      return formatMoney(row.allocatedDiscount);
    case 'allocatedTotal':
      return formatMoney(row.allocatedTotal);
    default:
      return row[key] ?? '-';
  }
};

const formatMoney = (value?: number | string) => {
  if (value === null || value === undefined || value === '') return '0.00';
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return String(value);
  return numeric.toFixed(2);
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const mapMethodName = (list: any[], code?: string) => {
  if (!code) return '-';
  return list.find((item) => item.code === code)?.name || code;
};

const buildDefaultConfig = (): TemplateConfig => ({
  headerFields: ['paymentNo', 'paidAt', 'supplierName', 'paymentMethod', 'paymentAmount', 'discountAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
  detailColumns: ['orderNo', 'allocatedAmount', 'allocatedDiscount', 'allocatedTotal'],
  showTotals: true,
  columnWidths: {
    orderNo: 14,
    allocatedAmount: 12,
    allocatedDiscount: 12,
    allocatedTotal: 12
  }
});

const applyTemplate = (tpl?: any) => {
  const defaults = buildDefaultConfig();
  if (!tpl || !tpl.fieldConfig) {
    headerFields.value = [...defaults.headerFields];
    detailColumnKeys.value = [...defaults.detailColumns];
    showTotals.value = defaults.showTotals;
    columnWidths.value = { ...(defaults.columnWidths || {}) };
    return;
  }
  try {
    const parsed = JSON.parse(tpl.fieldConfig);
    headerFields.value = Array.isArray(parsed.headerFields) ? parsed.headerFields : defaults.headerFields;
    detailColumnKeys.value = Array.isArray(parsed.detailColumns) ? parsed.detailColumns : defaults.detailColumns;
    showTotals.value = parsed.showTotals !== undefined ? Boolean(parsed.showTotals) : defaults.showTotals;
    columnWidths.value = normalizeColumnWidths(parsed.columnWidths, detailColumnKeys.value, defaults.columnWidths || {});
  } catch {
    headerFields.value = [...defaults.headerFields];
    detailColumnKeys.value = [...defaults.detailColumns];
    showTotals.value = defaults.showTotals;
    columnWidths.value = { ...(defaults.columnWidths || {}) };
  }
};

const fetchTemplate = async () => {
  try {
    const res: any = await request.get('/erp/print-templates/default', { params: { docType: 'PAYMENT' } });
    template.value = res.data.data;
    applyTemplate(template.value);
  } catch {
    template.value = null;
    applyTemplate();
  }
};

const fetchDetail = async () => {
  const res: any = await request.get(`/erp/payments/${paymentId.value}`);
  const data = res.data.data || {};
  payment.value = data.payment || null;
  items.value = data.payables || [];
  supplierName.value = data.supplierName || '';
};

const fetchOptions = async () => {
  const res: any = await request.get('/erp/payment-methods');
  paymentMethods.value = res.data.data || [];
};

const recordPrint = async () => {
  try {
    await request.post('/erp/print/logs', {
      docType: 'PAYMENT',
      docId: paymentId.value,
      templateId: template.value?.id || null
    });
  } catch {
    // ignore log errors
  }
};

const triggerPrint = async () => {
  if (!payment.value) return;
  await recordPrint();
  window.print();
};

const closeWindow = () => {
  window.close();
};

const init = async () => {
  loading.value = true;
  try {
    await Promise.all([fetchDetail(), fetchOptions(), fetchTemplate()]);
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
  await nextTick();
  if (isPreview.value && !shouldAutoPrint.value) return;
  if (!hasAutoPrinted.value) {
    hasAutoPrinted.value = true;
    setTimeout(() => {
      triggerPrint();
    }, 200);
  }
};

onMounted(() => {
  init();
});

const normalizeColumnWidths = (
  input: Record<string, number> | undefined,
  columns: string[],
  defaults: Record<string, number>
) => {
  const safe: Record<string, number> = {};
  columns.forEach((key) => {
    const value = input && typeof input[key] === 'number' ? input[key] : defaults[key];
    if (value) {
      safe[key] = value;
    }
  });
  return safe;
};

const columnStyle = (col: { width?: number }) => {
  if (!col.width) return undefined;
  return { width: `${col.width}ch` };
};
</script>

<style scoped>
.print-page {
  min-height: auto;
  background: #f3f1ed;
  padding: 16px;
  font-family: "Courier New", Courier, monospace;
}

.print-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
}

.print-paper {
  background: #fff;
  padding: 18px 20px 24px;
  border: 1px dashed #9b9b9b;
  border-radius: 6px;
  box-shadow: 0 12px 32px rgba(30, 30, 30, 0.1);
}

.print-paper--loading {
  opacity: 0.6;
}

.paper-header {
  text-align: center;
  margin-bottom: 16px;
}

.paper-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
}

.paper-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #5c5c5c;
}

.paper-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
  margin-bottom: 14px;
  font-size: 12px;
}

.meta-item {
  display: flex;
  gap: 8px;
}

.meta-label {
  color: #4d4d4d;
  min-width: 80px;
}

.meta-value {
  color: #111;
  font-weight: 600;
}

.paper-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.paper-table th,
.paper-table td {
  border: 1px solid #b3b3b3;
  padding: 6px 8px;
  text-align: left;
}

.paper-table th {
  background: #f0ede7;
}

.paper-totals {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  margin-top: 12px;
  font-size: 12px;
}

.totals-item {
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.paper-footer {
  margin-top: 16px;
  font-size: 12px;
  color: #4d4d4d;
  border-top: 1px dashed #b3b3b3;
  padding-top: 8px;
}

@media print {
  .print-page {
    background: #fff;
    padding: 0;
  }

  .print-toolbar {
    display: none;
  }

  .print-paper {
    border: none;
    box-shadow: none;
    border-radius: 0;
    padding: 0;
  }
}
</style>

