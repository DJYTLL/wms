<template>
  <div class="print-page">
    <div v-if="!isPreview" class="print-toolbar">
      <div class="toolbar-title">{{ $t('page.erpPurchaseReturnPrint') }}</div>
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
          <strong>{{ formatMoney(order?.totalAmount) }}</strong>
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
import { fetchPrintTemplate, parsePrintTemplateConfig, readPrintTemplatePreview, resolvePreviewConfigKey, resolveTemplateId, type PrintTemplateConfig as TemplateConfig } from '@/utils/printTemplate';
import { directPrintWindow } from '@/utils/directPrint';

const { t } = useI18n();
const route = useRoute();
const { notifyError } = useApiError();

const loading = ref(true);
const order = ref<any>(null);
const items = ref<any[]>([]);
const template = ref<any>(null);
const headerFields = ref<string[]>([]);
const detailColumnKeys = ref<string[]>([]);
const showTotals = ref(true);
const hasAutoPrinted = ref(false);
const columnWidths = ref<Record<string, number>>({});

const suppliers = ref<any[]>([]);
const warehouses = ref<any[]>([]);
const locations = ref<any[]>([]);
const purchaseOrderNo = ref<string>('');

const orderId = computed(() => Number(route.params.id));
const isPreview = computed(() => route.query.preview === '1' || route.query.preview === 'true');
const shouldAutoPrint = computed(() => route.query.auto === '1' || route.query.auto === 'true');

const headerTitle = computed(() => template.value?.headerTitle || t('print.purchaseReturnTitle'));
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
    orderNo: t('field.orderNo'),
    orderAt: t('field.orderTime'),
    supplierName: t('field.supplier'),
    returnSource: t('field.returnSource'),
    returnType: t('field.returnType'),
    purchaseOrderNo: t('field.purchaseOrderNo'),
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
    warehouse: t('field.warehouse'),
    location: t('field.location'),
    qty: t('field.quantity'),
    price: t('field.price'),
    amount: t('field.lineTotal'),
    taxRate: t('field.taxRate'),
    amountInclTax: t('field.totalAmount'),
    remark: t('field.remark')
  };
  return mapping[key] || '';
};

const headerFieldValue = (key: string) => {
  const current = order.value || {};
  switch (key) {
    case 'orderNo':
      return current.orderNo || '-';
    case 'orderAt':
      return formatDateTime(current.orderAt || current.createdAt);
    case 'supplierName':
      return getSupplierName(current.supplierId);
    case 'returnSource':
      return formatReturnSource(current.returnSource);
    case 'returnType':
      return formatReturnType(current.returnType);
    case 'purchaseOrderNo':
      return purchaseOrderNo.value || '-';
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

const formatReturnSource = (value?: string) => {
  if (value === 'BY_PURCHASE_ORDER') return t('returnSource.byPurchaseOrder');
  if (value === 'BY_PRODUCT') return t('returnSource.byProduct');
  return value || '-';
};

const formatReturnType = (value?: string) => {
  if (value === 'RETURN') return t('purchaseReturnType.return');
  if (value === 'SCRAP') return t('purchaseReturnType.scrap');
  return value || '-';
};

const formatItemValue = (row: any, key: string) => {
  switch (key) {
    case 'productCode':
      return row.productCode || '-';
    case 'productName':
      return row.productName || '-';
    case 'warehouse':
      return getWarehouseName(row.warehouseId);
    case 'location':
      return getLocationName(row.locationId);
    case 'qty':
      return formatNumber(row.qty);
    case 'price':
      return formatMoney(row.price);
    case 'amount':
      return formatMoney(row.amount);
    case 'taxRate':
      return row.taxRate != null ? `${formatNumber(row.taxRate)}%` : '-';
    case 'amountInclTax':
      return formatMoney(row.amountInclTax);
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

const getSupplierName = (id?: number) => suppliers.value.find((item) => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouses.value.find((item) => item.id === id)?.name || '-';
const getLocationName = (id?: number) => locations.value.find((item) => item.id === id)?.name || '-';

const buildDefaultConfig = (): TemplateConfig => ({
  headerFields: ['orderNo', 'orderAt', 'supplierName', 'returnSource', 'returnType', 'purchaseOrderNo', 'printCount', 'lastPrintedAt', 'remark'],
  detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
  showTotals: true,
  columnWidths: {
    productCode: 10,
    productName: 18,
    warehouse: 12,
    location: 12,
    qty: 6,
    price: 10,
    amount: 10,
    taxRate: 8,
    amountInclTax: 12,
    remark: 16
  }
});

const applyTemplate = (tpl?: any) => {
  const resolved = parsePrintTemplateConfig(tpl?.fieldConfig, buildDefaultConfig());
  headerFields.value = [...resolved.headerFields];
  detailColumnKeys.value = [...resolved.detailColumns];
  showTotals.value = resolved.showTotals;
  columnWidths.value = { ...resolved.columnWidths };
};

const fetchOptions = async () => {
  const [supplierRes, warehouseRes, locationRes] = await Promise.all([
    request.get('/erp/suppliers'),
    request.get('/erp/warehouses'),
    request.get('/erp/locations')
  ]);
  suppliers.value = supplierRes.data.data || [];
  warehouses.value = warehouseRes.data.data || [];
  locations.value = locationRes.data.data || [];
};

const fetchPurchaseOrderNo = async (id?: number) => {
  if (!id) return;
  try {
    const res: any = await request.get(`/erp/purchase-orders/${id}`);
    const data = res.data.data || {};
    purchaseOrderNo.value = data.order?.orderNo || data.orderNo || '';
  } catch {
    purchaseOrderNo.value = '';
  }
};

const fetchTemplate = async () => {
  try {
    const previewTemplate = readPrintTemplatePreview(resolvePreviewConfigKey(route.query.previewConfigKey));
    if (previewTemplate) {
      template.value = previewTemplate;
      applyTemplate(template.value);
      return;
    }
    template.value = await fetchPrintTemplate('PURCHASE_RETURN', resolveTemplateId(route.query.templateId));
    applyTemplate(template.value);
  } catch {
    template.value = null;
    applyTemplate();
  }
};

const fetchDetail = async () => {
  const res: any = await request.get(`/erp/purchase-returns/${orderId.value}`);
  const data = res.data.data || {};
  order.value = data.order || null;
  items.value = data.items || [];
  purchaseOrderNo.value = data.order?.purchaseOrderNo || '';
  if (data.order?.purchaseOrderId && !purchaseOrderNo.value) {
    await fetchPurchaseOrderNo(data.order.purchaseOrderId);
  }
};

const recordPrint = async () => {
  try {
    await request.post('/erp/print/logs', {
      docType: 'PURCHASE_RETURN',
      docId: orderId.value,
      templateId: template.value?.id || null
    });
  } catch {
    // ignore log errors
  }
};

const triggerPrint = async () => {
  if (!order.value) return;
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

