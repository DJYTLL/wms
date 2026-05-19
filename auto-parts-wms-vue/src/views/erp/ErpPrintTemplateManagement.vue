<template>
  <div class="page-shell page-shell--system print-template-page">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpPrintTemplateManagement') }}</div>
      <div v-if="!designerVisible" class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--4">
            <el-input
              v-model="nameQuery"
              placeholder="名称"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="codeQuery"
              placeholder="编码"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="docTypeFilter"
              :placeholder="$t('field.docType')"
              class="table-search erp-basic-field--narrow"
              clearable
              @change="handleSearch"
            >
              <el-option v-for="item in docTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search erp-basic-field--narrow"
              @change="handleSearch"
            >
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button type="primary" v-permission="'erp-print-template:add'" @click="openAddDesigner">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!designerVisible" class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-print-template-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="200">
            <template #default="{ row }">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" type="warning" style="margin-left: 6px">
                {{ $t('field.isDefault') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('docType')" prop="docType" :label="$t('field.docType')" min-width="140">
            <template #default="{ row }">
              {{ docTypeLabel(row.docType) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('sortNo')" prop="sortNo" :label="$t('field.sortNo')" width="120" />
          <ErpDataTableColumn v-if="canShow('enabled')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="260" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-print-template:edit'" @click="openEditDesigner(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="primary" size="small" v-permission="'erp-print-template:edit'" @click="setDefault(row)">{{ $t('action.resetDefault') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-print-template:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
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

    <section v-if="designerVisible" class="designer-shell">
      <div class="designer-topbar">
        <div class="designer-topbar__title">
          <div class="designer-eyebrow">{{ isEditing ? '模板编辑器' : '新建模板' }}</div>
          <div class="designer-heading">{{ formData.name || formData.code || docTypeLabel(formData.docType) }}</div>
        </div>
        <div class="designer-topbar__actions">
          <el-select
            v-model="previewSampleId"
            filterable
            clearable
            :loading="previewSamplesLoading"
            class="designer-sample-select"
            placeholder="选择预览样例单据"
          >
            <el-option v-for="item in previewSamples" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
          <el-button @click="closeDesigner">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
        </div>
      </div>

      <div class="designer-layout">
        <div class="designer-config">
          <div class="designer-panel">
            <div class="designer-panel__header">
              <div class="designer-panel__title">基础设置</div>
            </div>
            <div class="designer-form-grid">
              <el-form-item :label="$t('field.code')" required>
                <el-input v-model="formData.code" :placeholder="$t('placeholder.autoGenerated')" />
              </el-form-item>
              <el-form-item :label="$t('field.name')" required>
                <el-input v-model="formData.name" />
              </el-form-item>
              <el-form-item :label="$t('field.docType')" required>
                <el-select v-model="formData.docType" style="width: 100%" @change="applyDocTypeDefaults">
                  <el-option v-for="item in docTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
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
            </div>
          </div>

          <div class="designer-panel">
            <div class="designer-panel__header">
              <div class="designer-panel__title">页面文案</div>
            </div>
            <el-form-item :label="$t('field.headerTitle')">
              <el-input v-model="formData.headerTitle" />
            </el-form-item>
            <el-form-item :label="$t('field.subTitle')">
              <el-input v-model="formData.subTitle" />
            </el-form-item>
            <el-form-item :label="$t('field.footerNote')">
              <el-input v-model="formData.footerNote" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item :label="$t('field.remark')">
              <el-input v-model="formData.remark" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item :label="$t('field.showTotals')">
              <el-switch v-model="formData.showTotals" />
            </el-form-item>
          </div>

          <div class="designer-panel">
            <div class="designer-panel__header">
              <div class="designer-panel__title">抬头字段排序</div>
              <div class="designer-panel__hint">左侧添加，右侧调整顺序</div>
            </div>
            <div class="designer-chooser">
              <div class="designer-chooser__pool">
                <div class="designer-chooser__label">可选字段</div>
                <div class="token-grid">
                  <el-button
                    v-for="item in availableHeaderFields"
                    :key="item.key"
                    size="small"
                    plain
                    @click="addHeaderField(item.key)"
                  >
                    {{ item.label }}
                  </el-button>
                </div>
              </div>
              <div class="designer-chooser__selected">
                <div class="designer-chooser__label">已显示字段</div>
                <div class="designer-sort-list">
                  <div v-for="(item, index) in selectedHeaderFields" :key="item.key" class="designer-sort-item">
                    <div class="designer-sort-item__main">
                      <span class="designer-sort-item__index">{{ index + 1 }}</span>
                      <span class="designer-sort-item__label">{{ item.label }}</span>
                    </div>
                    <div class="designer-sort-item__actions">
                      <el-button circle size="small" :disabled="index === 0" @click="moveHeaderField(index, -1)">↑</el-button>
                      <el-button circle size="small" :disabled="index === selectedHeaderFields.length - 1" @click="moveHeaderField(index, 1)">↓</el-button>
                      <el-button circle size="small" type="danger" @click="removeHeaderField(item.key)">×</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="designer-panel">
            <div class="designer-panel__header">
              <div class="designer-panel__title">明细列排序</div>
              <div class="designer-panel__hint">支持顺序调整和列宽设置</div>
            </div>
            <div class="designer-chooser">
              <div class="designer-chooser__pool">
                <div class="designer-chooser__label">可选列</div>
                <div class="token-grid">
                  <el-button
                    v-for="item in availableDetailFields"
                    :key="item.key"
                    size="small"
                    plain
                    @click="addDetailField(item.key)"
                  >
                    {{ item.label }}
                  </el-button>
                </div>
              </div>
              <div class="designer-chooser__selected">
                <div class="designer-chooser__label">已显示列</div>
                <div class="designer-sort-list">
                  <div v-for="(item, index) in selectedDetailFields" :key="item.key" class="designer-sort-item designer-sort-item--wide">
                    <div class="designer-sort-item__main designer-sort-item__main--stacked">
                      <div class="designer-sort-item__row">
                        <span class="designer-sort-item__index">{{ index + 1 }}</span>
                        <span class="designer-sort-item__label">{{ item.label }}</span>
                      </div>
                      <div class="designer-sort-item__row designer-sort-item__row--meta">
                        <DecimalInput
                          :model-value="formData.columnWidths[item.key] ?? ''"
                          :scale="0"
                          input-mode="numeric"
                          class="designer-width-input"
                          @update:modelValue="(value) => updateColumnWidth(item.key, value)"
                        />
                        <span class="designer-width-unit">ch</span>
                      </div>
                    </div>
                    <div class="designer-sort-item__actions">
                      <el-button circle size="small" :disabled="index === 0" @click="moveDetailField(index, -1)">↑</el-button>
                      <el-button circle size="small" :disabled="index === selectedDetailFields.length - 1" @click="moveDetailField(index, 1)">↓</el-button>
                      <el-button circle size="small" type="danger" @click="removeDetailField(item.key)">×</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="designer-preview">
          <div class="designer-preview__toolbar">
            <div>
              <div class="designer-preview__eyebrow">实时预览</div>
              <div class="designer-preview__title">{{ previewSampleLabel || '请选择样例单据' }}</div>
            </div>
            <div class="designer-preview__status" :class="{ 'designer-preview__status--ready': previewReady }">
              {{ previewReady ? '已联动' : '待选择样例' }}
            </div>
          </div>
          <div class="designer-preview__canvas">
            <iframe v-if="previewFrameUrl" :src="previewFrameUrl" class="designer-preview__frame" />
            <div v-else class="designer-preview__empty">
              <div class="designer-preview__empty-title">右侧预览待配置</div>
              <div class="designer-preview__empty-text">先选择一条样例单据，左侧调整字段顺序和列宽，右侧会同步展示打印结果。</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useSystemConfig } from '@/composables/useSystemConfig';
import DecimalInput from '@/components/DecimalInput.vue';
import { normalizeColumnWidths as normalizeTemplateColumnWidths, parsePrintTemplateConfig, savePrintTemplatePreview } from '@/utils/printTemplate';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

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

type PrintDocType =
  | 'SALE_ORDER'
  | 'SALE_ORDER_DRAFT'
  | 'SALE_ORDER_APPROVED'
  | 'PURCHASE_ORDER'
  | 'PURCHASE_ORDER_DRAFT'
  | 'PURCHASE_ORDER_APPROVED'
  | 'SALE_RETURN'
  | 'SALE_RETURN_DRAFT'
  | 'SALE_RETURN_APPROVED'
  | 'PURCHASE_RETURN'
  | 'PURCHASE_RETURN_DRAFT'
  | 'PURCHASE_RETURN_APPROVED'
  | 'RECEIPT'
  | 'PAYMENT'
  | 'ACCOUNTS_RECEIVABLE'
  | 'ACCOUNTS_PAYABLE'
  | 'STOCK_COUNT'
  | 'STOCK_TRANSFER'
  | 'STOCK_INIT';

interface FieldOption {
  key: string;
  label: string;
}

interface PreviewSampleOption {
  id: number;
  label: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();
const defaultColumns = ['code', 'name', 'docType', 'sortNo', 'enabled'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-print-template', defaultColumns);
const canShow = (key: string) => isVisible(key);

const nameQuery = ref('');
const codeQuery = ref('');
const docTypeFilter = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<PrintTemplate[]>([]);
const allTableData = ref<PrintTemplate[]>([]);

const designerVisible = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const previewSamplesLoading = ref(false);
const previewSamples = ref<PreviewSampleOption[]>([]);
const previewSampleId = ref<number | null>(null);
const previewConfigKey = ref<string | null>(null);
const previewRevision = ref(0);
let previewSyncTimer: ReturnType<typeof setTimeout> | null = null;

const docTypeOptions = computed<{ value: PrintDocType; label: string }[]>(() => [
  { value: 'SALE_ORDER', label: t('print.docTypeSale') },
  { value: 'SALE_ORDER_DRAFT', label: '销售单草稿' },
  { value: 'SALE_ORDER_APPROVED', label: '销售单已审核' },
  { value: 'PURCHASE_ORDER', label: t('print.docTypePurchase') },
  { value: 'PURCHASE_ORDER_DRAFT', label: '采购单草稿' },
  { value: 'PURCHASE_ORDER_APPROVED', label: '采购单已审核' },
  { value: 'SALE_RETURN', label: t('print.docTypeSaleReturn') },
  { value: 'SALE_RETURN_DRAFT', label: t('print.docTypeSaleReturnDraft') },
  { value: 'SALE_RETURN_APPROVED', label: t('print.docTypeSaleReturnApproved') },
  { value: 'PURCHASE_RETURN', label: t('print.docTypePurchaseReturn') },
  { value: 'PURCHASE_RETURN_DRAFT', label: t('print.docTypePurchaseReturnDraft') },
  { value: 'PURCHASE_RETURN_APPROVED', label: t('print.docTypePurchaseReturnApproved') },
  { value: 'RECEIPT', label: t('print.docTypeReceipt') },
  { value: 'PAYMENT', label: t('print.docTypePayment') },
  { value: 'ACCOUNTS_RECEIVABLE', label: t('print.docTypeAccountsReceivable') },
  { value: 'ACCOUNTS_PAYABLE', label: t('print.docTypeAccountsPayable') },
  { value: 'STOCK_COUNT', label: t('print.docTypeStockCount') },
  { value: 'STOCK_TRANSFER', label: t('print.docTypeStockTransfer') },
  { value: 'STOCK_INIT', label: t('print.docTypeStockInit') }
]);

const formData = reactive({
  code: '',
  name: '',
  docType: 'SALE_ORDER' as PrintDocType,
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

const previewSourceMap: Record<PrintDocType, { endpoint: string; preferredStatus?: string }> = {
  SALE_ORDER: { endpoint: '/erp/sale-orders/page', preferredStatus: 'APPROVED' },
  SALE_ORDER_DRAFT: { endpoint: '/erp/sale-orders/draft/page' },
  SALE_ORDER_APPROVED: { endpoint: '/erp/sale-orders/approved/page' },
  PURCHASE_ORDER: { endpoint: '/erp/purchase-orders/page', preferredStatus: 'APPROVED' },
  PURCHASE_ORDER_DRAFT: { endpoint: '/erp/purchase-orders/draft/page' },
  PURCHASE_ORDER_APPROVED: { endpoint: '/erp/purchase-orders/approved/page', preferredStatus: 'APPROVED' },
  SALE_RETURN: { endpoint: '/erp/sale-returns/page', preferredStatus: 'APPROVED' },
  SALE_RETURN_DRAFT: { endpoint: '/erp/sale-returns/draft/page' },
  SALE_RETURN_APPROVED: { endpoint: '/erp/sale-returns/approved/page', preferredStatus: 'APPROVED' },
  PURCHASE_RETURN: { endpoint: '/erp/purchase-returns/page', preferredStatus: 'APPROVED' },
  PURCHASE_RETURN_DRAFT: { endpoint: '/erp/purchase-returns/draft/page' },
  PURCHASE_RETURN_APPROVED: { endpoint: '/erp/purchase-returns/approved/page' },
  RECEIPT: { endpoint: '/erp/receipts/page' },
  PAYMENT: { endpoint: '/erp/payments/page' },
  ACCOUNTS_RECEIVABLE: { endpoint: '/erp/ar/page' },
  ACCOUNTS_PAYABLE: { endpoint: '/erp/ap/page' },
  STOCK_COUNT: { endpoint: '/erp/stock-counts/page', preferredStatus: 'APPROVED' },
  STOCK_TRANSFER: { endpoint: '/erp/stock-transfers/page', preferredStatus: 'APPROVED' },
  STOCK_INIT: { endpoint: '/erp/stock-inits/page', preferredStatus: 'APPROVED' }
};

const printPathMap: Record<PrintDocType, string> = {
  SALE_ORDER: 'sale-orders',
  SALE_ORDER_DRAFT: 'sale-orders/draft',
  SALE_ORDER_APPROVED: 'sale-orders/approved',
  PURCHASE_ORDER: 'purchase-orders',
  PURCHASE_ORDER_DRAFT: 'purchase-orders/draft',
  PURCHASE_ORDER_APPROVED: 'purchase-orders/approved',
  SALE_RETURN: 'sale-returns',
  SALE_RETURN_DRAFT: 'sale-returns/draft',
  SALE_RETURN_APPROVED: 'sale-returns/approved',
  PURCHASE_RETURN: 'purchase-returns',
  PURCHASE_RETURN_DRAFT: 'purchase-returns/draft',
  PURCHASE_RETURN_APPROVED: 'purchase-returns/approved',
  RECEIPT: 'receipts',
  PAYMENT: 'payments',
  ACCOUNTS_RECEIVABLE: 'ar',
  ACCOUNTS_PAYABLE: 'ap',
  STOCK_COUNT: 'stock-counts',
  STOCK_TRANSFER: 'stock-transfers',
  STOCK_INIT: 'stock-inits'
};

const headerFieldOptions = computed(() => buildFieldOptions(formData.docType, true));
const detailFieldOptions = computed(() => buildFieldOptions(formData.docType, false));

const selectedHeaderFields = computed(() =>
  formData.headerFields
    .map((key) => headerFieldOptions.value.find((item) => item.key === key))
    .filter((item): item is FieldOption => Boolean(item))
);

const availableHeaderFields = computed(() =>
  headerFieldOptions.value.filter((item) => !formData.headerFields.includes(item.key))
);

const selectedDetailFields = computed(() =>
  formData.detailColumns
    .map((key) => detailFieldOptions.value.find((item) => item.key === key))
    .filter((item): item is FieldOption => Boolean(item))
);

const availableDetailFields = computed(() =>
  detailFieldOptions.value.filter((item) => !formData.detailColumns.includes(item.key))
);

const previewSampleLabel = computed(() => previewSamples.value.find((item) => item.id === previewSampleId.value)?.label || '');
const previewReady = computed(() => Boolean(designerVisible.value && previewSampleId.value && previewFrameUrl.value));

const previewFrameUrl = computed(() => {
  if (!designerVisible.value || !previewSampleId.value || !previewConfigKey.value) return '';
  const query = new URLSearchParams({
    preview: '1',
    previewConfigKey: previewConfigKey.value,
    rev: String(previewRevision.value)
  });
  return `/erp/${printPathMap[formData.docType]}/${previewSampleId.value}/print?${query.toString()}`;
});

const buildFieldOptions = (docType: string, isHeader: boolean): FieldOption[] => {
  if (isHeader) {
    switch (docType) {
      case 'SALE_ORDER':
      case 'SALE_ORDER_DRAFT':
      case 'SALE_ORDER_APPROVED':
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
      case 'PURCHASE_ORDER':
      case 'PURCHASE_ORDER_DRAFT':
      case 'PURCHASE_ORDER_APPROVED':
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
      case 'SALE_RETURN_DRAFT':
      case 'SALE_RETURN_APPROVED':
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
      case 'PURCHASE_RETURN_DRAFT':
      case 'PURCHASE_RETURN_APPROVED':
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
          { key: 'adjustmentReason', label: t('field.adjustmentReason') },
          { key: 'status', label: t('field.status') },
          { key: 'printCount', label: t('field.printCount') },
          { key: 'lastPrintedAt', label: t('field.lastPrintedAt') },
          { key: 'remark', label: t('field.remark') }
        ];
      case 'STOCK_TRANSFER':
        return [
          { key: 'transferNo', label: t('field.transferNo') },
          { key: 'transferAt', label: t('field.transferAt') },
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
    case 'SALE_ORDER':
    case 'SALE_ORDER_DRAFT':
    case 'SALE_ORDER_APPROVED':
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
        ...(docType === 'STOCK_INIT'
          ? [
              { key: 'initUnitCost', label: t('field.initUnitCost') },
              { key: 'initTotalAmount', label: t('field.initTotalAmount') }
            ]
          : []),
        { key: 'diffQty', label: t('field.diffQty') },
        { key: 'remark', label: t('field.remark') }
      ];
    case 'STOCK_TRANSFER':
      return [
        { key: 'productCode', label: t('field.code') },
        { key: 'productName', label: t('field.product') },
        { key: 'fromWarehouse', label: t('field.fromWarehouse') },
        { key: 'fromLocation', label: t('field.fromLocation') },
        { key: 'toWarehouse', label: t('field.toWarehouse') },
        { key: 'toLocation', label: t('field.toLocation') },
        { key: 'qty', label: t('field.quantity') },
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

const buildPreviewLabel = (docType: PrintDocType, row: Record<string, any>) => {
  const codeFieldMap: Record<PrintDocType, string> = {
    SALE_ORDER: 'orderNo',
    SALE_ORDER_DRAFT: 'orderNo',
    SALE_ORDER_APPROVED: 'orderNo',
    PURCHASE_ORDER: 'orderNo',
    PURCHASE_ORDER_DRAFT: 'orderNo',
    PURCHASE_ORDER_APPROVED: 'orderNo',
    SALE_RETURN: 'orderNo',
    SALE_RETURN_DRAFT: 'orderNo',
    SALE_RETURN_APPROVED: 'orderNo',
    PURCHASE_RETURN: 'orderNo',
    PURCHASE_RETURN_DRAFT: 'orderNo',
    PURCHASE_RETURN_APPROVED: 'orderNo',
    RECEIPT: 'receiptNo',
    PAYMENT: 'paymentNo',
    ACCOUNTS_RECEIVABLE: 'receivableNo',
    ACCOUNTS_PAYABLE: 'payableNo',
    STOCK_COUNT: 'countNo',
    STOCK_TRANSFER: 'transferNo',
    STOCK_INIT: 'countNo'
  };
  const code = row[codeFieldMap[docType]] || `#${row.id}`;
  const secondary = row.customerName || row.supplierName || row.status || '';
  return secondary ? `${code} / ${secondary}` : String(code);
};

const buildPreviewPayload = () => ({
  headerTitle: formData.headerTitle,
  subTitle: formData.subTitle,
  footerNote: formData.footerNote,
  fieldConfig: JSON.stringify({
    headerFields: formData.headerFields,
    detailColumns: formData.detailColumns,
    showTotals: formData.showTotals,
    columnWidths: formData.columnWidths
  })
});

const normalizeColumnWidths = (input: Record<string, number> | undefined, columns: string[], defaults: Record<string, number>) =>
  normalizeTemplateColumnWidths(input, columns, defaults);

const moveArrayItem = (list: string[], index: number, step: -1 | 1) => {
  const target = index + step;
  if (target < 0 || target >= list.length) return list;
  const next = [...list];
  const currentValue = next[index];
  const targetValue = next[target];
  if (currentValue === undefined || targetValue === undefined) {
    return list;
  }
  next[index] = targetValue;
  next[target] = currentValue;
  return next;
};

const addHeaderField = (key: string) => {
  if (!formData.headerFields.includes(key)) {
    formData.headerFields = [...formData.headerFields, key];
  }
};

const removeHeaderField = (key: string) => {
  formData.headerFields = formData.headerFields.filter((item) => item !== key);
};

const moveHeaderField = (index: number, step: -1 | 1) => {
  formData.headerFields = moveArrayItem(formData.headerFields, index, step);
};

const addDetailField = (key: string) => {
  if (!formData.detailColumns.includes(key)) {
    formData.detailColumns = [...formData.detailColumns, key];
  }
};

const removeDetailField = (key: string) => {
  formData.detailColumns = formData.detailColumns.filter((item) => item !== key);
};

const moveDetailField = (index: number, step: -1 | 1) => {
  formData.detailColumns = moveArrayItem(formData.detailColumns, index, step);
};

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

const buildDefaultColumnWidths = (docType: string): Record<string, number> => {
  switch (docType) {
    case 'RECEIPT':
    case 'PAYMENT':
      return { orderNo: 12, allocatedAmount: 10, allocatedDiscount: 10, allocatedTotal: 10 };
    case 'ACCOUNTS_RECEIVABLE':
      return { receiptNo: 12, status: 8, amount: 8, discountAmount: 8, redFlushReason: 12, createdAt: 14 };
    case 'ACCOUNTS_PAYABLE':
      return { paymentNo: 12, status: 8, amount: 8, discountAmount: 8, redFlushReason: 12, createdAt: 14 };
    case 'STOCK_COUNT':
      return { productCode: 8, productName: 14, warehouse: 10, location: 10, systemQty: 6, countedQty: 6, diffQty: 6, remark: 12 };
    case 'STOCK_TRANSFER':
      return { productCode: 8, productName: 14, fromWarehouse: 10, fromLocation: 10, toWarehouse: 10, toLocation: 10, qty: 6, remark: 12 };
    case 'STOCK_INIT':
      return { productCode: 8, productName: 14, warehouse: 10, location: 10, systemQty: 6, countedQty: 6, initUnitCost: 8, initTotalAmount: 10, diffQty: 6, remark: 12 };
    default:
      return { productCode: 8, productName: 14, warehouse: 10, location: 10, qty: 5, price: 8, amount: 8, taxRate: 6, amountInclTax: 10, remark: 12 };
  }
};

const getDocTypeDefaults = (docType: string): PrintFieldConfig => {
  const columnWidths = buildDefaultColumnWidths(docType);
  switch (docType) {
    case 'PURCHASE_ORDER':
    case 'PURCHASE_ORDER_DRAFT':
    case 'PURCHASE_ORDER_APPROVED':
      return {
        headerFields: ['orderNo', 'orderAt', 'supplierName', 'paymentMethod', 'paidAmount', 'discountAmount', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
        showTotals: true,
        columnWidths
      };
    case 'SALE_RETURN':
    case 'SALE_RETURN_DRAFT':
    case 'SALE_RETURN_APPROVED':
      return {
        headerFields: ['orderNo', 'orderAt', 'customerName', 'returnSource', 'returnType', 'saleOrderNo', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'qty', 'price', 'amount', 'taxRate', 'amountInclTax', 'remark'],
        showTotals: true,
        columnWidths
      };
    case 'PURCHASE_RETURN':
    case 'PURCHASE_RETURN_DRAFT':
    case 'PURCHASE_RETURN_APPROVED':
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
        headerFields: ['countNo', 'countAt', 'adjustmentReason', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'systemQty', 'countedQty', 'diffQty', 'remark'],
        showTotals: false,
        columnWidths
      };
    case 'STOCK_TRANSFER':
      return {
        headerFields: ['transferNo', 'transferAt', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'fromWarehouse', 'fromLocation', 'toWarehouse', 'toLocation', 'qty', 'remark'],
        showTotals: false,
        columnWidths
      };
    case 'STOCK_INIT':
      return {
        headerFields: ['stockInitNo', 'countAt', 'status', 'printCount', 'lastPrintedAt', 'remark'],
        detailColumns: ['productCode', 'productName', 'warehouse', 'location', 'systemQty', 'countedQty', 'initUnitCost', 'initTotalAmount', 'diffQty', 'remark'],
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

const parseFieldConfig = (config?: string, docType?: string): PrintFieldConfig => {
  const base = getDocTypeDefaults(docType || 'SALE_ORDER');
  return parsePrintTemplateConfig(config, base);
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
  previewSampleId.value = null;
};

const applyDocTypeDefaults = () => {
  const defaults = parseFieldConfig('', formData.docType);
  formData.headerFields = [...defaults.headerFields];
  formData.detailColumns = [...defaults.detailColumns];
  formData.columnWidths = { ...defaults.columnWidths };
  formData.showTotals = defaults.showTotals;
};

const fetchNextCode = async () => {
  try {
    const res: any = await request.get('/erp/print-templates/next-code');
    if (res.data.code === 200) {
      formData.code = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const applySearch = () => {
  let filtered = allTableData.value.slice();
  if (docTypeFilter.value) filtered = filtered.filter(row => row.docType === docTypeFilter.value);
  if (statusFilter.value !== 'all') filtered = filtered.filter(row => row.enabled === (statusFilter.value === 'enabled'));
  filtered = filterByFuzzyKeyword(filtered, nameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, codeQuery.value, row => [row.code]);
  total.value = filtered.length;
  const start = (page.value - 1) * size.value;
  tableData.value = filtered.slice(start, start + size.value);
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/erp/print-templates');
    if (res.data.code === 200) {
      allTableData.value = res.data.data || [];
      applySearch();
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const fetchPreviewSamples = async (docType: PrintDocType) => {
  const source = previewSourceMap[docType];
  previewSamplesLoading.value = true;
  try {
    const load = async (status?: string) => {
      const params: Record<string, any> = { page: 1, size: 20 };
      if (status) params.status = status;
      const res: any = await request.get(source.endpoint, { params });
      return res.data.data?.items || [];
    };

    let items = await load(source.preferredStatus);
    if (!items.length && source.preferredStatus) items = await load();

    previewSamples.value = items.map((item: Record<string, any>) => ({
      id: item.id,
      label: buildPreviewLabel(docType, item)
    }));
    previewSampleId.value = previewSamples.value[0]?.id || null;
  } catch (error) {
    previewSamples.value = [];
    previewSampleId.value = null;
    notifyError(error);
  } finally {
    previewSamplesLoading.value = false;
  }
};

const syncPreviewConfig = () => {
  if (!designerVisible.value || !previewConfigKey.value) return;
  savePrintTemplatePreview(previewConfigKey.value, buildPreviewPayload());
  previewRevision.value += 1;
};

const schedulePreviewSync = () => {
  if (!designerVisible.value || !previewSampleId.value) return;
  if (previewSyncTimer) clearTimeout(previewSyncTimer);
  previewSyncTimer = setTimeout(() => {
    syncPreviewConfig();
  }, 180);
};

const openAddDesigner = async () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  previewConfigKey.value = `tpl-${Date.now()}`;
  designerVisible.value = true;
  fetchNextCode();
  await fetchPreviewSamples(formData.docType);
  syncPreviewConfig();
};

const openEditDesigner = async (row: PrintTemplate) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.docType = (row.docType || 'SALE_ORDER') as PrintDocType;
  formData.headerTitle = row.headerTitle || '';
  formData.subTitle = row.subTitle || '';
  formData.footerNote = row.footerNote || '';
  formData.sortNo = row.sortNo || 0;
  formData.enabled = row.enabled;
  formData.isDefault = Boolean(row.isDefault);
  formData.remark = row.remark || '';

  const config = parseFieldConfig(row.fieldConfig, formData.docType);
  formData.headerFields = [...config.headerFields];
  formData.detailColumns = [...config.detailColumns];
  formData.columnWidths = { ...config.columnWidths };
  formData.showTotals = config.showTotals;

  previewConfigKey.value = `tpl-${row.id}-${Date.now()}`;
  designerVisible.value = true;
  await fetchPreviewSamples(formData.docType);
  syncPreviewConfig();
};

const closeDesigner = () => {
  designerVisible.value = false;
  resetForm();
  previewConfigKey.value = null;
  previewRevision.value = 0;
  if (previewSyncTimer) clearTimeout(previewSyncTimer);
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
      await fetchList();
      closeDesigner();
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

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  applySearch();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  applySearch();
};

onMounted(() => {
  fetchTenantKeys();
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
    formData.columnWidths = normalizeColumnWidths(formData.columnWidths, columns, defaults);
  },
  { deep: true }
);

watch(
  () => [
    formData.headerTitle,
    formData.subTitle,
    formData.footerNote,
    formData.showTotals,
    formData.headerFields.join('|'),
    formData.detailColumns.join('|'),
    JSON.stringify(formData.columnWidths)
  ],
  () => {
    schedulePreviewSync();
  }
);

watch(
  () => previewSampleId.value,
  () => {
    schedulePreviewSync();
  }
);
</script>

<style scoped>
.print-template-page {
  --designer-border: rgba(30, 41, 59, 0.08);
  --designer-panel: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%);
  --designer-accent: #0f766e;
  --designer-accent-soft: rgba(15, 118, 110, 0.08);
}

.designer-shell {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid var(--designer-border);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.12), transparent 28%),
    linear-gradient(180deg, #fdfefe 0%, #f2f6f8 100%);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.08);
}

.designer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.designer-eyebrow,
.designer-preview__eyebrow {
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.designer-heading,
.designer-preview__title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.designer-topbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.designer-sample-select {
  width: 280px;
}

.designer-layout {
  display: grid;
  grid-template-columns: minmax(420px, 500px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.designer-config,
.designer-preview {
  min-width: 0;
}

.designer-config {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.designer-panel {
  padding: 16px;
  border: 1px solid var(--designer-border);
  border-radius: 20px;
  background: var(--designer-panel);
}

.designer-panel__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.designer-panel__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.designer-panel__hint {
  font-size: 12px;
  color: #64748b;
}

.designer-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.designer-panel :deep(.el-form-item) {
  margin-bottom: 14px;
}

.designer-chooser {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
}

.designer-chooser__label {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.designer-chooser__pool,
.designer-chooser__selected {
  padding: 12px;
  border: 1px dashed rgba(100, 116, 139, 0.35);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
}

.token-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.designer-sort-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.designer-sort-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 14px;
  background: #fff;
}

.designer-sort-item--wide {
  align-items: stretch;
}

.designer-sort-item__main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.designer-sort-item__main--stacked {
  flex: 1 1 auto;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.designer-sort-item__row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.designer-sort-item__row--meta {
  gap: 6px;
}

.designer-sort-item__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: var(--designer-accent-soft);
  color: var(--designer-accent);
  font-size: 12px;
  font-weight: 700;
}

.designer-sort-item__label {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.designer-sort-item__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.designer-width-input {
  width: 72px;
}

.designer-width-unit {
  font-size: 12px;
  color: #64748b;
}

.designer-preview {
  position: sticky;
  top: 18px;
  padding: 18px;
  border: 1px solid var(--designer-border);
  border-radius: 22px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef3f6 100%);
}

.designer-preview__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.designer-preview__status {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.designer-preview__status--ready {
  background: rgba(15, 118, 110, 0.14);
  color: var(--designer-accent);
}

.designer-preview__canvas {
  min-height: 900px;
  padding: 16px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(148, 163, 184, 0.12), transparent 42%),
    linear-gradient(180deg, #f3f5f7 0%, #edf1f4 100%);
}

.designer-preview__frame {
  width: 100%;
  height: 920px;
  border: none;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 20px 32px rgba(15, 23, 42, 0.12);
}

.designer-preview__empty {
  display: flex;
  min-height: 520px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  text-align: center;
  color: #64748b;
}

.designer-preview__empty-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.designer-preview__empty-text {
  max-width: 360px;
  line-height: 1.7;
}

.full-input :deep(.el-input__wrapper) {
  width: 100%;
}

@media (max-width: 1280px) {
  .designer-layout {
    grid-template-columns: 1fr;
  }

  .designer-preview {
    position: static;
  }
}

@media (max-width: 768px) {
  .designer-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .designer-topbar__actions {
    flex-wrap: wrap;
  }

  .designer-sample-select {
    width: 100%;
  }

  .designer-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
