<template>
  <div class="print-page">
    <div v-if="!isPreview" class="print-toolbar">
      <div class="toolbar-title">{{ $t('page.erpReceiptPrint') }}</div>
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
          <span>{{ $t('field.receiptAmount') }}</span>
          <strong>{{ formatMoney(receipt?.amount) }}</strong>
        </div>
        <div class="totals-item">
          <span>{{ $t('field.discountAmount') }}</span>
          <strong>{{ formatMoney(receipt?.discountAmount) }}</strong>
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
import { useAuthStore } from '@/stores/auth';
import { getCachedSettlementMethods } from '@/composables/erpBaseDataCache';
import { fetchPrintTemplate, parsePrintTemplateConfig, readPrintTemplatePreview, resolvePreviewConfigKey, resolveTemplateId, type PrintTemplateConfig as TemplateConfig } from '@/utils/printTemplate';
import { directPrintWindow } from '@/utils/directPrint';

const { t } = useI18n();
const route = useRoute();
const { notifyError } = useApiError();
const authStore = useAuthStore();

const loading = ref(true);
const receipt = ref<any>(null);
const items = ref<any[]>([]);
const template = ref<any>(null);
const headerFields = ref<string[]>([]);
const detailColumnKeys = ref<string[]>([]);
const showTotals = ref(true);
const hasAutoPrinted = ref(false);
const columnWidths = ref<Record<string, number>>({});
const customerName = ref('');
const settlementMethods = ref<any[]>([]);

const receiptId = computed(() => Number(route.params.id));
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');
const isPreview = computed(() => route.query.preview === '1' || route.query.preview === 'true');
const shouldAutoPrint = computed(() => route.query.auto === '1' || route.query.auto === 'true');

const headerTitle = computed(() => template.value?.headerTitle || t('print.receiptTitle'));
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
    receiptNo: t('field.receiptNo'),
    receivedAt: t('field.receivedAt'),
    customerName: t('field.customer'),
    settlementMethod: t('field.settlementMethod'),
    receiptAmount: t('field.receiptAmount'),
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
    allocatedAmount: t('field.receiptAmount'),
    allocatedDiscount: t('field.discountAllocated'),
    allocatedTotal: t('field.totalAmount')
  };
  return mapping[key] || '';
};

const headerFieldValue = (key: string) => {
  const current = receipt.value || {};
  switch (key) {
    case 'receiptNo':
      return current.receiptNo || '-';
    case 'receivedAt':
      return formatDateTime(current.receivedAt || current.createdAt);
    case 'customerName':
      return customerName.value || '-';
    case 'settlementMethod':
      return mapMethodName(settlementMethods.value, current.settlementMethod);
    case 'receiptAmount':
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
  headerFields: ['receiptNo', 'receivedAt', 'customerName', 'settlementMethod', 'receiptAmount', 'discountAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
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
  const resolved = parsePrintTemplateConfig(tpl?.fieldConfig, buildDefaultConfig());
  headerFields.value = [...resolved.headerFields];
  detailColumnKeys.value = [...resolved.detailColumns];
  showTotals.value = resolved.showTotals;
  columnWidths.value = { ...resolved.columnWidths };
};

const fetchTemplate = async () => {
  try {
    const previewTemplate = readPrintTemplatePreview(resolvePreviewConfigKey(route.query.previewConfigKey));
    if (previewTemplate) {
      template.value = previewTemplate;
      applyTemplate(template.value);
      return;
    }
    template.value = await fetchPrintTemplate('RECEIPT', resolveTemplateId(route.query.templateId));
    applyTemplate(template.value);
  } catch {
    template.value = null;
    applyTemplate();
  }
};

const fetchDetail = async () => {
  const res: any = await request.get(`/erp/receipts/${receiptId.value}`);
  const data = res.data.data || {};
  receipt.value = data.receipt || null;
  items.value = data.receivables || [];
  customerName.value = data.customerName || '';
};

const fetchOptions = async () => {
  settlementMethods.value = await getCachedSettlementMethods(tenantCacheKey.value);
};

const recordPrint = async () => {
  try {
    await request.post('/erp/print/logs', {
      docType: 'RECEIPT',
      docId: receiptId.value,
      templateId: template.value?.id || null
    });
  } catch {
    // ignore log errors
  }
};

const triggerPrint = async () => {
  if (!receipt.value) return;
  await recordPrint();
  const printed = await directPrintWindow(window, { removeSelectors: ['.print-toolbar'] });
  if (!printed) window.print();
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

const columnStyle = (col: { width?: number }) => {
  if (!col.width) return undefined;
  return { width: `${col.width}ch` };
};
</script>

<style scoped>
.print-page {
  min-height: auto;
  background: #fff;
  padding: 0;
  font-family: "SimSun", "宋体", "Microsoft YaHei", sans-serif;
  color: #000;
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
  padding: 4mm 5mm 5mm;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.print-paper--loading {
  opacity: 0.6;
}

.paper-header {
  text-align: center;
  margin-bottom: 10px;
}

.paper-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.paper-subtitle {
  margin-top: 2px;
  font-size: 10px;
  color: #000;
}

.paper-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 12px;
  margin-bottom: 10px;
  font-size: 10px;
}

.meta-item {
  display: flex;
  gap: 8px;
}

.meta-label {
  color: #000;
  min-width: 58px;
}

.meta-value {
  color: #111;
  font-weight: 600;
}

.paper-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 10px;
}

.paper-table th,
.paper-table td {
  border: 1px solid #b3b3b3;
  padding: 3px 4px;
  text-align: left;
  word-break: break-all;
}

.paper-table th {
  background: #fff;
}

.paper-totals {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 8px;
  font-size: 10px;
}

.totals-item {
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.paper-footer {
  margin-top: 10px;
  font-size: 10px;
  color: #000;
  border-top: 1px solid #b3b3b3;
  padding-top: 6px;
}

@page {
  size: auto;
  margin: 4mm 5mm;
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

