<template>
  <div class="print-page">
    <div v-if="!isPreview" class="print-toolbar">
      <div class="toolbar-title">{{ $t('page.erpAccountsReceivablePrint') }}</div>
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
          <span>{{ $t('field.totalAmount') }}</span>
          <strong>{{ formatMoney(receivable?.totalAmount) }}</strong>
        </div>
        <div class="totals-item">
          <span>{{ $t('field.paidAmount') }}</span>
          <strong>{{ formatMoney(receivable?.paidAmount) }}</strong>
        </div>
        <div class="totals-item">
          <span>{{ $t('field.discountAmount') }}</span>
          <strong>{{ formatMoney(receivable?.discountAmount) }}</strong>
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
const receivable = ref<any>(null);
const items = ref<any[]>([]);
const template = ref<any>(null);
const headerFields = ref<string[]>([]);
const detailColumnKeys = ref<string[]>([]);
const showTotals = ref(true);
const hasAutoPrinted = ref(false);
const columnWidths = ref<Record<string, number>>({});
const customerName = ref('');

const receivableId = computed(() => Number(route.params.id));
const isPreview = computed(() => route.query.preview === '1' || route.query.preview === 'true');
const shouldAutoPrint = computed(() => route.query.auto === '1' || route.query.auto === 'true');

const headerTitle = computed(() => template.value?.headerTitle || t('print.arTitle'));
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
    receivableNo: t('field.receivableNo'),
    orderNo: t('field.orderNo'),
    customerName: t('field.customer'),
    totalAmount: t('field.totalAmount'),
    paidAmount: t('field.paidAmount'),
    discountAmount: t('field.discountAmount'),
    unpaidAmount: t('field.unpaidAmount'),
    status: t('field.status'),
    printCount: t('field.printCount'),
    lastPrintedAt: t('field.lastPrintedAt'),
    remark: t('field.remark')
  };
  return mapping[key] || '';
};

const detailColumnLabel = (key: string) => {
  const mapping: Record<string, string> = {
    receiptNo: t('field.receiptNo'),
    status: t('field.status'),
    amount: t('field.receiptAmount'),
    discountAmount: t('field.discountAmount'),
    redFlushReason: t('field.redFlushReason'),
    createdAt: t('field.createdTime')
  };
  return mapping[key] || '';
};

const headerFieldValue = (key: string) => {
  const current = receivable.value || {};
  switch (key) {
    case 'receivableNo':
      return current.receivableNo || '-';
    case 'orderNo':
      return current.orderNo || '-';
    case 'customerName':
      return customerName.value || '-';
    case 'totalAmount':
      return formatMoney(current.totalAmount);
    case 'paidAmount':
      return formatMoney(current.paidAmount);
    case 'discountAmount':
      return formatMoney(current.discountAmount);
    case 'unpaidAmount':
      return formatMoney(current.unpaidAmount);
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
  if (value === 'OPEN') return t('status.open');
  if (value === 'RECEIVED') return t('status.received');
  if (value === 'RED_FLUSHED') return t('status.redFlushed');
  if (value === 'CANCELLED') return t('status.cancelled');
  return value;
};

const formatItemValue = (row: any, key: string) => {
  switch (key) {
    case 'receiptNo':
      return row.receiptNo || '-';
    case 'status':
      return statusLabel(row.status);
    case 'amount':
      return formatMoney(row.amount);
    case 'discountAmount':
      return formatMoney(row.discountAmount);
    case 'redFlushReason':
      return row.status === 'RED_FLUSHED' ? extractRedFlushReason(row.remark) : '-';
    case 'createdAt':
      return formatDateTime(row.createdAt);
    default:
      return row[key] ?? '-';
  }
};

const extractRedFlushReason = (remark?: string) => {
  if (!remark) return '-';
  const marker = '红冲原因:';
  const index = remark.indexOf(marker);
  if (index === -1) return remark;
  return remark.slice(index + marker.length).trim() || '-';
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

const buildDefaultConfig = (): TemplateConfig => ({
  headerFields: ['receivableNo', 'orderNo', 'customerName', 'totalAmount', 'paidAmount', 'discountAmount', 'unpaidAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
  detailColumns: ['receiptNo', 'status', 'amount', 'discountAmount', 'redFlushReason', 'createdAt'],
  showTotals: true,
  columnWidths: {
    receiptNo: 14,
    status: 10,
    amount: 10,
    discountAmount: 10,
    redFlushReason: 16,
    createdAt: 18
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
    const res: any = await request.get('/erp/print-templates/default', { params: { docType: 'ACCOUNTS_RECEIVABLE' } });
    template.value = res.data.data;
    applyTemplate(template.value);
  } catch {
    template.value = null;
    applyTemplate();
  }
};

const fetchDetail = async () => {
  const res: any = await request.get(`/erp/ar/${receivableId.value}`);
  const data = res.data.data || {};
  receivable.value = data.receivable || null;
  items.value = data.receipts || [];
  customerName.value = data.customerName || '';
};

const recordPrint = async () => {
  try {
    await request.post('/erp/print/logs', {
      docType: 'ACCOUNTS_RECEIVABLE',
      docId: receivableId.value,
      templateId: template.value?.id || null
    });
  } catch {
    // ignore log errors
  }
};

const triggerPrint = async () => {
  if (!receivable.value) return;
  await recordPrint();
  window.print();
};

const closeWindow = () => {
  window.close();
};

const init = async () => {
  loading.value = true;
  try {
    await Promise.all([fetchDetail(), fetchTemplate()]);
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

