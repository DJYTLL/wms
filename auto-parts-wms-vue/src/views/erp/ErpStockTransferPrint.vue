<template>
  <div class="print-page">
    <div v-if="!isPreview" class="print-toolbar">
      <div class="toolbar-title">{{ $t('page.erpStockTransferPrint') }}</div>
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
import { getCachedLocations, getCachedProductOptions, getCachedWarehouses } from '@/composables/erpBaseDataCache';
import { directPrintWindow } from '@/utils/directPrint';
import {
  fetchPrintTemplate,
  parsePrintTemplateConfig,
  readPrintTemplatePreview,
  resolvePreviewConfigKey,
  resolveTemplateId,
  type PrintTemplateConfig as TemplateConfig
} from '@/utils/printTemplate';

const { t } = useI18n();
const route = useRoute();
const { notifyError } = useApiError();
const authStore = useAuthStore();

const loading = ref(true);
const transfer = ref<any>(null);
const items = ref<any[]>([]);
const template = ref<any>(null);
const headerFields = ref<string[]>([]);
const detailColumnKeys = ref<string[]>([]);
const hasAutoPrinted = ref(false);
const columnWidths = ref<Record<string, number>>({});

const products = ref<any[]>([]);
const warehouses = ref<any[]>([]);
const locations = ref<any[]>([]);

const transferId = computed(() => Number(route.params.id));
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');
const isPreview = computed(() => route.query.preview === '1' || route.query.preview === 'true');
const shouldAutoPrint = computed(() => route.query.auto === '1' || route.query.auto === 'true');

const headerTitle = computed(() => template.value?.headerTitle || t('print.stockTransferTitle'));
const subTitle = computed(() => template.value?.subTitle || '');
const footerNote = computed(() => template.value?.footerNote || t('print.footerNote'));

const headerRows = computed(() =>
  headerFields.value
    .map((key) => ({ key, label: headerFieldLabel(key), value: headerFieldValue(key) }))
    .filter((item) => item.label)
);

const detailColumns = computed(() =>
  detailColumnKeys.value.map((key) => ({ key, label: detailColumnLabel(key), width: columnWidths.value[key] }))
);

const headerFieldLabel = (key: string) => {
  const mapping: Record<string, string> = {
    transferNo: t('field.transferNo'),
    transferAt: t('field.transferAt'),
    status: t('field.status'),
    printCount: t('field.printCount'),
    lastPrintedAt: t('field.lastPrintedAt'),
    remark: t('field.remark')
  };
  return mapping[key] || '';
};

const detailColumnLabel = (key: string) => {
  const mapping: Record<string, string> = {
    productCode: t('field.code'),
    productName: t('field.product'),
    fromWarehouse: t('field.fromWarehouse'),
    fromLocation: t('field.fromLocation'),
    toWarehouse: t('field.toWarehouse'),
    toLocation: t('field.toLocation'),
    qty: t('field.quantity'),
    remark: t('field.remark')
  };
  return mapping[key] || '';
};

const headerFieldValue = (key: string) => {
  const current = transfer.value || {};
  switch (key) {
    case 'transferNo':
      return current.transferNo || '-';
    case 'transferAt':
      return formatDateTime(current.transferAt || current.createdAt);
    case 'status':
      return statusLabel(current.status);
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
  if (value === 'APPROVED') return t('status.approved');
  if (value === 'CANCELLED') return t('status.cancelled');
  return value;
};

const formatItemValue = (row: any, key: string) => {
  switch (key) {
    case 'productCode':
      return getProductCode(row.productId);
    case 'productName':
      return getProductName(row.productId);
    case 'fromWarehouse':
      return getWarehouseName(row.fromWarehouseId);
    case 'fromLocation':
      return getLocationName(row.fromLocationId);
    case 'toWarehouse':
      return getWarehouseName(row.toWarehouseId);
    case 'toLocation':
      return getLocationName(row.toLocationId);
    case 'qty':
      return formatNumber(row.qty);
    case 'remark':
      return row.remark || '-';
    default:
      return row[key] ?? '-';
  }
};

const formatNumber = (value?: number | string) => {
  if (value === null || value === undefined || value === '') return '-';
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return String(value);
  return numeric.toString();
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const getProductCode = (id?: number) => products.value.find((item) => item.id === id)?.code || '-';
const getProductName = (id?: number) => products.value.find((item) => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouses.value.find((item) => item.id === id)?.name || '-';
const getLocationName = (id?: number) => {
  if (id == null) return t('field.unassignedLocation');
  return locations.value.find((item) => item.id === id)?.name || t('field.unassignedLocation');
};

const buildDefaultConfig = (): TemplateConfig => ({
  headerFields: ['transferNo', 'transferAt', 'status', 'printCount', 'lastPrintedAt', 'remark'],
  detailColumns: ['productCode', 'productName', 'fromWarehouse', 'fromLocation', 'toWarehouse', 'toLocation', 'qty', 'remark'],
  showTotals: false,
  columnWidths: {
    productCode: 8,
    productName: 14,
    fromWarehouse: 10,
    fromLocation: 10,
    toWarehouse: 10,
    toLocation: 10,
    qty: 6,
    remark: 12
  }
});

const applyTemplate = (tpl?: any) => {
  const resolved = parsePrintTemplateConfig(tpl?.fieldConfig, buildDefaultConfig());
  headerFields.value = [...resolved.headerFields];
  detailColumnKeys.value = [...resolved.detailColumns];
  columnWidths.value = { ...resolved.columnWidths };
};

const fetchTemplateData = async () => {
  try {
    const previewTemplate = readPrintTemplatePreview(resolvePreviewConfigKey(route.query.previewConfigKey));
    if (previewTemplate) {
      template.value = previewTemplate;
      applyTemplate(template.value);
      return;
    }
    template.value = await fetchPrintTemplate('STOCK_TRANSFER', resolveTemplateId(route.query.templateId));
    applyTemplate(template.value);
  } catch {
    template.value = null;
    applyTemplate();
  }
};

const fetchDetail = async () => {
  const res: any = await request.get(`/erp/stock-transfers/${transferId.value}`);
  const data = res.data.data || {};
  transfer.value = data.transfer || null;
  items.value = data.items || [];
};

const fetchOptions = async () => {
  const [productList, warehouseList, locationList] = await Promise.all([
    getCachedProductOptions(tenantCacheKey.value),
    getCachedWarehouses(tenantCacheKey.value),
    getCachedLocations(tenantCacheKey.value)
  ]);
  products.value = productList;
  warehouses.value = warehouseList;
  locations.value = locationList;
};

const recordPrint = async () => {
  try {
    await request.post('/erp/print/logs', {
      docType: 'STOCK_TRANSFER',
      docId: transferId.value,
      templateId: template.value?.id || null
    });
  } catch {
    // ignore log errors
  }
};

const triggerPrint = async () => {
  if (!transfer.value) return;
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
    await Promise.all([fetchDetail(), fetchOptions(), fetchTemplateData()]);
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

const columnStyle = (col: { width?: number }) => {
  if (!col.width) return undefined;
  return { width: `${col.width}ch` };
};

onMounted(() => {
  init();
});
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
