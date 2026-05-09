<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpPrintTemplateManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--3">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="table-search erp-basic-field--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="docTypeFilter" :placeholder="$t('field.docType')" class="table-search erp-basic-field--narrow" clearable @change="handleSearch">
            <el-option v-for="item in docTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" v-permission="'erp-print-template:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column prop="code" :label="$t('field.code')" min-width="140" />
          <el-table-column prop="name" :label="$t('field.name')" min-width="200">
            <template #default="{ row }">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" type="warning" style="margin-left: 6px">
                {{ $t('field.isDefault') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="docType" :label="$t('field.docType')" min-width="140">
            <template #default="{ row }">
              {{ docTypeLabel(row.docType) }}
            </template>
          </el-table-column>
          <el-table-column prop="sortNo" :label="$t('field.sortNo')" width="120" />
          <el-table-column prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-print-template:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="primary" size="small" v-permission="'erp-print-template:edit'" @click="setDefault(row)">{{ $t('action.resetDefault') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-print-template:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="760px" @closed="resetForm">
      <el-form :model="formData" label-width="120px" class="print-template-form">
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item :label="$t('field.docType')" required>
          <el-select v-model="formData.docType" style="width: 100%" @change="applyDocTypeDefaults">
            <el-option v-for="item in docTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.headerTitle')">
          <el-input v-model="formData.headerTitle" />
        </el-form-item>
        <el-form-item :label="$t('field.subTitle')">
          <el-input v-model="formData.subTitle" />
        </el-form-item>
        <el-form-item :label="$t('field.footerNote')">
          <el-input v-model="formData.footerNote" />
        </el-form-item>
        <el-form-item :label="$t('field.headerFields')">
          <el-checkbox-group v-model="formData.headerFields" class="print-field-group">
            <el-checkbox v-for="item in headerFieldOptions" :key="item.key" :value="item.key">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="$t('field.detailColumns')">
          <el-checkbox-group v-model="formData.detailColumns" class="print-field-group">
            <el-checkbox v-for="item in detailFieldOptions" :key="item.key" :value="item.key">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="$t('field.columnWidth')">
          <div class="print-width-grid">
            <div v-for="item in selectedDetailFields" :key="item.key" class="print-width-item">
              <span class="print-width-label">{{ item.label }}</span>
              <DecimalInput
                :model-value="formData.columnWidths[item.key] ?? ''"
                :scale="0"
                input-mode="numeric"
                class="print-width-input"
                @update:modelValue="(value) => updateColumnWidth(item.key, value)"
              />
              <span class="print-width-unit">ch</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="$t('field.showTotals')">
          <el-switch v-model="formData.showTotals" />
        </el-form-item>
        <el-form-item :label="$t('field.sortNo')">
          <DecimalInput
            :model-value="formData.sortNo ?? ''"
            :scale="0"
            input-mode="numeric"
            class="full-input"
            @update:modelValue="updateSortNo"
          />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.isDefault')">
          <el-switch v-model="formData.isDefault" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import DecimalInput from '@/components/DecimalInput.vue';

interface PrintTemplate {
  id: number;
  code: string;
  name: string;
  docType: string;
  headerTitle?: string;
  subTitle?: string;
  footerNote?: string;
  fieldConfig?: string;
  sortNo?: number;
  enabled: boolean;
  isDefault?: boolean;
  remark?: string;
}

interface PrintFieldConfig {
  headerFields: string[];
  detailColumns: string[];
  showTotals: boolean;
  columnWidths: Record<string, number>;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const docTypeFilter = ref<string>('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<PrintTemplate[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);

const docTypeOptions = computed(() => [
  { value: 'SALE_ORDER', label: t('print.docTypeSale') },
  { value: 'PURCHASE_ORDER', label: t('print.docTypePurchase') },
  { value: 'SALE_RETURN', label: t('print.docTypeSaleReturn') },
  { value: 'PURCHASE_RETURN', label: t('print.docTypePurchaseReturn') },
  { value: 'RECEIPT', label: t('print.docTypeReceipt') },
  { value: 'PAYMENT', label: t('print.docTypePayment') },
  { value: 'ACCOUNTS_RECEIVABLE', label: t('print.docTypeAccountsReceivable') },
  { value: 'ACCOUNTS_PAYABLE', label: t('print.docTypeAccountsPayable') },
  { value: 'STOCK_COUNT', label: t('print.docTypeStockCount') },
  { value: 'STOCK_INIT', label: t('print.docTypeStockInit') }
]);

const formData = reactive({
  code: '',
  name: '',
  docType: 'SALE_ORDER',
  headerTitle: '',
  subTitle: '',
  footerNote: '',
  headerFields: [] as string[],
  detailColumns: [] as string[],
  columnWidths: {} as Record<string, number>,
  showTotals: true,
  sortNo: 0,
  enabled: true,
  isDefault: false,
  remark: ''
});

const headerFieldOptions = computed(() => buildFieldOptions(formData.docType, true));

const detailFieldOptions = computed(() => buildFieldOptions(formData.docType, false));

const selectedDetailFields = computed(() => {
  return detailFieldOptions.value.filter((item) => formData.detailColumns.includes(item.key));
});

const updateColumnWidth = (key: string, value: string) => {
  if (!value) {
    delete formData.columnWidths[key];
    return;
  }
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return;
  const clamped = Math.max(4, Math.min(40, numeric));
  formData.columnWidths[key] = Math.round(clamped);
};

const updateSortNo = (value: string) => {
  if (!value) {
    formData.sortNo = 0;
    return;
  }
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return;
  formData.sortNo = Math.max(0, Math.round(numeric));
};

const buildFieldOptions = (docType: string, isHeader: boolean) => {
  if (isHeader) {
    switch (docType) {
      case 'PURCHASE_ORDER':
        return [
          { key: 'orderNo', label: t('field.orderNo') },
          { key: 'orderAt', label: t('field.orderTime') },
          { key: 'supplierName', label: t('field.supplier') },
          { key: 'paymentMethod', label: t('field.paymentMethod') },
          { key: 'paidAmount', label: t('field.paidAmount') },
          { key: 'discountAmount', label: t('field.discountAmount') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'SALE_RETURN':
        return [
          { key: 'orderNo', label: t('field.orderNo') },
          { key: 'orderAt', label: t('field.orderTime') },
          { key: 'customerName', label: t('field.customer') },
          { key: 'returnSource', label: t('field.returnSource') },
          { key: 'returnType', label: t('field.returnType') },
          { key: 'saleOrderNo', label: t('field.saleOrderNo') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'PURCHASE_RETURN':
        return [
          { key: 'orderNo', label: t('field.orderNo') },
          { key: 'orderAt', label: t('field.orderTime') },
          { key: 'supplierName', label: t('field.supplier') },
          { key: 'returnSource', label: t('field.returnSource') },
          { key: 'returnType', label: t('field.returnType') },
          { key: 'purchaseOrderNo', label: t('field.purchaseOrderNo') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'RECEIPT':
        return [
          { key: 'receiptNo', label: t('field.receiptNo') },
          { key: 'receivedAt', label: t('field.receivedAt') },
          { key: 'customerName', label: t('field.customer') },
          { key: 'settlementMethod', label: t('field.settlementMethod') },
          { key: 'receiptAmount', label: t('field.receiptAmount') },
          { key: 'discountAmount', label: t('field.discountAmount') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'PAYMENT':
        return [
          { key: 'paymentNo', label: t('field.paymentNo') },
          { key: 'paidAt', label: t('field.paidAt') },
          { key: 'supplierName', label: t('field.supplier') },
          { key: 'paymentMethod', label: t('field.paymentMethod') },
          { key: 'paymentAmount', label: t('field.paymentAmount') },
          { key: 'discountAmount', label: t('field.discountAmount') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'ACCOUNTS_RECEIVABLE':
        return [
          { key: 'receivableNo', label: t('field.receivableNo') },
          { key: 'orderNo', label: t('field.orderNo') },
          { key: 'customerName', label: t('field.customer') },
          { key: 'totalAmount', label: t('field.totalAmount') },
          { key: 'paidAmount', label: t('field.paidAmount') },
          { key: 'discountAmount', label: t('field.discountAmount') },
          { key: 'unpaidAmount', label: t('field.unpaidAmount') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'ACCOUNTS_PAYABLE':
        return [
          { key: 'payableNo', label: t('field.payableNo') },
          { key: 'orderNo', label: t('field.orderNo') },
          { key: 'supplierName', label: t('field.supplier') },
          { key: 'totalAmount', label: t('field.totalAmount') },
          { key: 'paidAmount', label: t('field.paidAmount') },
          { key: 'discountAmount', label: t('field.discountAmount') },
          { key: 'unpaidAmount', label: t('field.unpaidAmount') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'STOCK_COUNT':
        return [
          { key: 'countNo', label: t('field.countNo') },
          { key: 'countAt', label: t('field.countAt') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'STOCK_INIT':
        return [
          { key: 'stockInitNo', label: t('field.stockInitNo') },
          { key: 'countAt', label: t('field.countAt') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      default:
        return [
          { key: 'orderNo', label: t('field.orderNo') },
          { key: 'orderAt', label: t('field.orderTime') },
          { key: 'customerName', label: t('field.customer') },
          { key: 'settlementMethod', label: t('field.settlementMethod') },
          { key: 'deliveryMethod', label: t('field.deliveryMethod') },
          { key: 'paidAmount', label: t('field.paidAmount') },
          { key: 'discountAmount', label: t('field.discountAmount') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
    }
  }
  switch (docType) {
    case 'RECEIPT':
    case 'PAYMENT':
      return [
        { key: 'orderNo', label: t('field.orderNo') },
        { key: 'allocatedAmount', label: t('field.receiptAmount') },
        { key: 'allocatedDiscount', label: t('field.discountAllocated') },
        { key: 'allocatedTotal', label: t('field.totalAmount') }
      ];
    case 'ACCOUNTS_RECEIVABLE':
      return [
        { key: 'receiptNo', label: t('field.receiptNo') },
        { key: 'status', label: t('field.status') },
        { key: 'amount', label: t('field.receiptAmount') },
        { key: 'discountAmount', label: t('field.discountAmount') },
        { key: 'redFlushReason', label: t('field.redFlushReason') },
        { key: 'createdAt', label: t('field.createdTime') }
      ];
    case 'ACCOUNTS_PAYABLE':
      return [
        { key: 'paymentNo', label: t('field.paymentNo') },
        { key: 'status', label: t('field.status') },
        { key: 'amount', label: t('field.paymentAmount') },
        { key: 'discountAmount', label: t('field.discountAmount') },
        { key: 'redFlushReason', label: t('field.redFlushReason') },
        { key: 'createdAt', label: t('field.createdTime') }
      ];
    case 'STOCK_COUNT':
    case 'STOCK_INIT':
      return [
        { key: 'productCode', label: t('field.code') },
        { key: 'productName', label: t('field.product') },
        { key: 'warehouse', label: t('field.warehouse') },
        { key: 'location', label: t('field.location') },
        { key: 'systemQty', label: t('field.systemQty') },
        { key: 'countedQty', label: t('field.countedQty') },
        { key: 'diffQty', label: t('field.diffQty') },
        { key: 'remark', label: t('field.remark') }
      ];
    default:
      return [
        { key: 'productCode', label: t('field.code') },
        { key: 'productName', label: t('field.product') },
        { key: 'warehouse', label: t('field.warehouse') },
        { key: 'location', label: t('field.location') },
        { key: 'qty', label: t('field.quantity') },
        { key: 'price', label: t('field.price') },
        { key: 'amount', label: t('field.lineTotal') },
        { key: 'taxRate', label: t('field.taxRate') },
        { key: 'amountInclTax', label: t('field.totalAmount') },
        { key: 'remark', label: t('field.remark') }
      ];
  }
};

const docTypeLabel = (value?: string) => {
  const option = docTypeOptions.value.find((item) => item.value === value);
  return option?.label || value || '-';
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (docTypeFilter.value) params.docType = docTypeFilter.value;
    if (statusFilter.value !== 'all') params.enabled = statusFilter.value === 'enabled';

    const res: any = await request.get('/erp/print-templates/page', { params });
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

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
};

const openEditModal = (row: PrintTemplate) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.docType = row.docType || 'SALE_ORDER';
  formData.headerTitle = row.headerTitle || '';
  formData.subTitle = row.subTitle || '';
  formData.footerNote = row.footerNote || '';
  formData.sortNo = row.sortNo || 0;
  formData.enabled = row.enabled;
  formData.isDefault = Boolean(row.isDefault);
  formData.remark = row.remark || '';

  const config = parseFieldConfig(row.fieldConfig, row.docType || 'SALE_ORDER');
  formData.headerFields = [...config.headerFields];
  formData.detailColumns = [...config.detailColumns];
  formData.columnWidths = { ...config.columnWidths };
  formData.showTotals = config.showTotals;
  showModal.value = true;
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.docType = 'SALE_ORDER';
  formData.headerTitle = '';
  formData.subTitle = '';
  formData.footerNote = '';
  formData.sortNo = 0;
  formData.enabled = true;
  formData.isDefault = false;
  formData.remark = '';
  const defaults = parseFieldConfig('', 'SALE_ORDER');
  formData.headerFields = [...defaults.headerFields];
  formData.detailColumns = [...defaults.detailColumns];
  formData.columnWidths = { ...defaults.columnWidths };
  formData.showTotals = defaults.showTotals;
};

const applyDocTypeDefaults = () => {
  const defaults = parseFieldConfig('', formData.docType);
  formData.headerFields = [...defaults.headerFields];
  formData.detailColumns = [...defaults.detailColumns];
  formData.columnWidths = { ...defaults.columnWidths };
  formData.showTotals = defaults.showTotals;
};

const parseFieldConfig = (config?: string, docType?: string): PrintFieldConfig => {
  const base = getDocTypeDefaults(docType || 'SALE_ORDER');
  if (!config) return base;
  try {
    const parsed = JSON.parse(config);
    return {
      headerFields: Array.isArray(parsed.headerFields) ? parsed.headerFields : base.headerFields,
      detailColumns: Array.isArray(parsed.detailColumns) ? parsed.detailColumns : base.detailColumns,
      showTotals: parsed.showTotals !== undefined ? Boolean(parsed.showTotals) : base.showTotals,
      columnWidths: normalizeColumnWidths(parsed.columnWidths, Array.isArray(parsed.detailColumns) ? parsed.detailColumns : base.detailColumns, base.columnWidths)
    };
  } catch {
    return base;
  }
};

const getDocTypeDefaults = (docType: string): PrintFieldConfig => {
  const columnWidths = buildDefaultColumnWidths(docType);
  switch (docType) {
    case 'PURCHASE_ORDER':
      return {
        headerFields: ['orderNo', 'orderAt', 'supplierName', 'paymentMethod', 'paidAmount', 'discountAmount', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
        showTotals: true,
        columnWidths
      };
    case 'SALE_RETURN':
      return {
        headerFields: ['orderNo', 'orderAt', 'customerName', 'returnSource', 'returnType', 'saleOrderNo', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
        showTotals: true,
        columnWidths
      };
    case 'PURCHASE_RETURN':
      return {
        headerFields: ['orderNo', 'orderAt', 'supplierName', 'returnSource', 'returnType', 'purchaseOrderNo', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
        showTotals: true,
        columnWidths
      };
    case 'RECEIPT':
      return {
        headerFields: ['receiptNo', 'receivedAt', 'customerName', 'settlementMethod', 'receiptAmount', 'discountAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['orderNo', 'allocatedAmount', 'allocatedDiscount', 'allocatedTotal'],
        showTotals: true,
        columnWidths
      };
    case 'PAYMENT':
      return {
        headerFields: ['paymentNo', 'paidAt', 'supplierName', 'paymentMethod', 'paymentAmount', 'discountAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['orderNo', 'allocatedAmount', 'allocatedDiscount', 'allocatedTotal'],
        showTotals: true,
        columnWidths
      };
    case 'ACCOUNTS_RECEIVABLE':
      return {
        headerFields: ['receivableNo', 'orderNo', 'customerName', 'totalAmount', 'paidAmount', 'discountAmount', 'unpaidAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['receiptNo', 'status', 'amount', 'discountAmount', 'redFlushReason', 'createdAt'],
        showTotals: true,
        columnWidths
      };
    case 'ACCOUNTS_PAYABLE':
      return {
        headerFields: ['payableNo', 'orderNo', 'supplierName', 'totalAmount', 'paidAmount', 'discountAmount', 'unpaidAmount', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['paymentNo', 'status', 'amount', 'discountAmount', 'redFlushReason', 'createdAt'],
        showTotals: true,
        columnWidths
      };
    case 'STOCK_COUNT':
      return {
        headerFields: ['countNo', 'countAt', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'systemQty', 'countedQty', 'diffQty', 'remark'],
        showTotals: false,
        columnWidths
      };
    case 'STOCK_INIT':
      return {
        headerFields: ['stockInitNo', 'countAt', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'systemQty', 'countedQty', 'diffQty', 'remark'],
        showTotals: false,
        columnWidths
      };
    default:
      return {
        headerFields: ['orderNo', 'orderAt', 'customerName', 'settlementMethod', 'deliveryMethod', 'paidAmount', 'discountAmount', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
        showTotals: true,
        columnWidths
      };
  }
};

const buildDefaultColumnWidths = (docType: string): Record<string, number> => {
  switch (docType) {
    case 'RECEIPT':
    case 'PAYMENT':
      return {
        orderNo: 14,
        allocatedAmount: 12,
        allocatedDiscount: 12,
        allocatedTotal: 12
      };
    case 'ACCOUNTS_RECEIVABLE':
      return {
        receiptNo: 14,
        status: 10,
        amount: 10,
        discountAmount: 10,
        redFlushReason: 16,
        createdAt: 18
      };
    case 'ACCOUNTS_PAYABLE':
      return {
        paymentNo: 14,
        status: 10,
        amount: 10,
        discountAmount: 10,
        redFlushReason: 16,
        createdAt: 18
      };
    case 'STOCK_COUNT':
    case 'STOCK_INIT':
      return {
        productCode: 10,
        productName: 18,
        warehouse: 12,
        location: 12,
        systemQty: 8,
        countedQty: 8,
        diffQty: 8,
        remark: 16
      };
    default:
      return {
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
      };
  }
};

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

const saveData = async () => {
  if (!formData.code || !formData.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = {
      code: formData.code,
      name: formData.name,
      docType: formData.docType,
      headerTitle: formData.headerTitle,
      subTitle: formData.subTitle,
      footerNote: formData.footerNote,
      fieldConfig: JSON.stringify({
        headerFields: formData.headerFields,
        detailColumns: formData.detailColumns,
        showTotals: formData.showTotals,
        columnWidths: formData.columnWidths
      }),
      sortNo: formData.sortNo,
      enabled: formData.enabled,
      isDefault: formData.isDefault,
      remark: formData.remark
    };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/print-templates/${currentId.value}`, payload)
      : await request.post('/erp/print-templates', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: PrintTemplate) => {
  try {
    await request.delete(`/erp/print-templates/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const setDefault = async (row: PrintTemplate) => {
  try {
    await request.post(`/erp/print-templates/${row.id}/default`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchList();
  bindPageSizeSync(size, fetchList);
});

onActivated(() => {
  fetchList();
});

watch(
  () => formData.detailColumns,
  (columns) => {
    const defaults = buildDefaultColumnWidths(formData.docType);
    const next: Record<string, number> = {};
    columns.forEach((key) => {
      const currentValue = formData.columnWidths[key];
      next[key] = typeof currentValue === 'number' ? currentValue : defaults[key] || 10;
    });
    formData.columnWidths = next;
  },
  { deep: true }
);
</script>

<style scoped>
.print-field-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
}

.print-template-form :deep(.el-checkbox) {
  margin-right: 0;
}

.print-width-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px 16px;
  width: 100%;
}

.print-width-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafbfd;
}

.print-width-label {
  flex: 1 1 auto;
  font-size: 12px;
  color: #606266;
}

.print-width-unit {
  font-size: 12px;
  color: #909399;
}

.print-width-input {
  width: 80px;
}

.full-input :deep(.el-input__wrapper) {
  width: 100%;
}
</style>
