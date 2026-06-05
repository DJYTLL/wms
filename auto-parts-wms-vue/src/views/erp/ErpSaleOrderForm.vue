<template>
  <div class="page-shell page-shell--system sale-page-surface">
    <div class="page-header sale-page-header">
      <div class="sale-title-group">
        <div class="page-title">{{ pageTitle }}</div>
        <div class="sale-breadcrumb">
          <span>{{ $t('page.erpSaleOrderManagement') }}</span>
          <span class="sale-breadcrumb__separator">/</span>
          <span>{{ $t('page.erpSaleOrder') }}</span>
          <span class="sale-breadcrumb__separator">/</span>
          <span>{{ pageTitle }}</span>
        </div>
      </div>
      <div class="table-actions sale-page-toolbar__actions">
        <el-button class="action-button" @click="handleBack">{{ $t('action.back') }}</el-button>
        <el-button
          v-if="shouldShowCopyButton"
          class="action-button action-button--secondary"
          :disabled="isInitializing || !canCopy"
          @click="handleCopy"
        >
          {{ $t('action.copy') }}
        </el-button>
        <el-button
          v-if="canPrint"
          class="action-button action-button--primary"
          type="primary"
          @click="handlePrint"
        >
          {{ $t('action.print') }}
        </el-button>
        <el-button
          v-if="shouldShowRedFlushButton"
          type="danger"
          plain
          class="action-button action-button--danger"
          :disabled="isInitializing || !canRedFlush"
          @click="handleRedFlush"
        >
          {{ $t('action.redFlush') }}
        </el-button>
        <el-button
          v-if="canCancel"
          type="danger"
          plain
          class="action-button action-button--danger"
          :disabled="isInitializing"
          @click="handleCancel"
        >
          {{ $t('action.cancel') }}
        </el-button>
        <el-button v-if="!isReadOnly" class="action-button action-button--save" :loading="isSaving" :disabled="isSaving" @click="handleSave">{{ $t('action.save') }}</el-button>
        <el-button
          v-if="shouldShowApproveButton"
          type="success"
          plain
          class="action-button action-button--success"
          :loading="isSaving"
          :disabled="isSaving || isInitializing || !canApprove"
          @click="handleApprove"
        >
          {{ $t('action.approve') }}
        </el-button>
        <!-- <el-button v-if="!isReadOnly" type="primary" class="action-button" :loading="isSaving" :disabled="isSaving" @click="handleSaveAndBack">{{ $t('action.saveAndBack') }}</el-button> -->
      </div>
    </div>

    <SaleOrderHeaderForm
      :form-data="formData"
      :is-read-only="isReadOnly"
      :customer-options="customerOptions"
      :delivery-method-options="deliveryMethodOptions"
      :current-customer-name="currentCustomerName"
      :current-delivery-method-name="currentDeliveryMethodName"
      :current-operator-name="currentOperatorName"
      :customer-search-loading="customerSearchLoading"
      :handle-customer-change="handleCustomerChange"
      :search-customers="searchCustomers"
    />

    <SaleOrderDetailTable
      :form-data="formData"
      :is-read-only="isReadOnly"
      :selected-items="selectedItems"
      :product-search-loading="productSearchLoading"
      :can-edit-product-inline="canEditProductInline"
      :can-use-quick-assembly="canUseQuickAssembly"
      :can-show-discount-allocated="canShowDiscountAllocated"
      :can-show-profit="canShowProfit"
      :total-summary="totalSummary"
      :total-profit-text="totalProfitText"
      :total-profit-rate-text="totalProfitRateText"
      :add-item="addItem"
      :remove-selected-items="removeSelectedItems"
      :handle-row-click="handleRowClick"
      :handle-item-selection-change="handleItemSelectionChange"
      :set-product-select-ref="setProductSelectRef"
      :search-products="searchProducts"
      :handle-product-change="handleProductChange"
      :get-selectable-product-options="getSelectableProductOptions"
      :open-product-edit-from-option="openProductEditFromOption"
      :open-history-for-row="openHistoryForRow"
      :is-assembly-product="isAssemblyProduct"
      :open-assembly-for-row="openAssemblyForRow"
      :resolve-product-label="resolveProductLabel"
      :resolve-warehouse-label="resolveWarehouseLabel"
      :resolve-location-label="resolveLocationLabel"
      :handle-stock-location-change="handleStockLocationChange"
      :get-stock-options-for-row="getStockOptionsForRow"
      :format-plain-number="formatPlainNumber"
      :format-money="formatMoney"
      :calc-line-amount="calcLineAmount"
      :calc-line-discount="calcLineDiscount"
      :format-profit-cell="formatProfitCell"
      :remove-item="removeItem"
      :get-discount-amount="getDiscountAmount"
    />

    <div class="table-card payment-card">
      <div class="card-section-header">
        <h4>{{ $t('section.saleSettlementInfo') }}</h4>
      </div>
      <div class="table-body payment-card-body">
        <el-form :model="formData" label-position="top" class="sale-form sale-form--compact payment-form">
          <div class="payment-grid">
            <div class="form-group form-group--settlement">
              <el-form-item :label="$t('field.settlementMethod')" required>
                <div v-if="isReadOnly" class="readonly-inline">{{ currentSettlementMethodName }}</div>
                <el-select v-else v-model="formData.settlementMethod" style="width: 100%">
                  <el-option v-for="item in settlementMethodOptions" :key="item.code" :label="item.name" :value="item.code" />
                </el-select>
              </el-form-item>
            </div>
            <div v-if="!isCreditSettlement" class="form-group form-group--settlement">
              <el-form-item :label="$t('field.receiptMethod')">
                <div v-if="isReadOnly" class="readonly-inline">{{ currentReceiptMethodName }}</div>
                <el-select v-else v-model="formData.receiptMethodCode" clearable style="width: 100%">
                  <el-option v-for="item in receiptMethodOptions" :key="item.code" :label="item.name" :value="item.code" />
                </el-select>
              </el-form-item>
            </div>
            <div v-if="formData.settlementMethod && !isCreditSettlement" class="form-group form-group--amount">
              <el-form-item :label="$t('field.paidAmount')">
                <div v-if="isReadOnly" class="readonly-inline">{{ formatMoney(formData.paidAmount) }}</div>
                <DecimalInput v-else v-model="formData.paidAmount" :scale="2" style="width: 100%" />
              </el-form-item>
            </div>
            <div v-if="formData.settlementMethod" class="form-group form-group--discount">
              <el-form-item :label="$t('field.discountAmount')">
                <div v-if="isReadOnly" class="readonly-inline">{{ formatMoney(formData.discountAmount) }}</div>
                <DecimalInput v-else v-model="formData.discountAmount" :scale="2" style="width: 100%" />
              </el-form-item>
            </div>
            <div v-if="showCustomerDebtTotal" class="form-group form-group--debt-total">
              <el-form-item :label="$t('field.customerDebtTotal')">
                <div class="readonly-inline readonly-inline--debt">{{ formatMoney(formData.customerDebtTotal) }}</div>
              </el-form-item>
            </div>
          </div>
          <div class="payment-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ $t('message.discountAllocationHint') }}</span>
          </div>
        </el-form>
      </div>
    </div>

    <SaleOrderProductHistoryDialog
      v-if="historyDialogVisible"
      v-model:visible="historyDialogVisible"
      v-model:active-tab="historyTab"
      v-model:order-visible="historyOrderDialogVisible"
      :loading="historyLoading"
      :header-items="saleHistoryHeaderItems"
      :tabs="saleHistoryTabs"
      :order-title="historyOrderDialogTitle"
      :order-url="historyOrderDialogUrl"
      @tab-change="handleHistoryTabChange"
      @filter-change="handleHistoryDialogFilterChange"
      @page-change="handleHistoryDialogPageChange"
      @size-change="handleHistoryDialogSizeChange"
    />

    <SaleOrderProductEditBridge
      v-if="productEditDrawerVisible"
      v-model="productEditDrawerVisible"
      :product-id="productEditProductId"
      @saved="handleInlineProductSaved"
    />

    <SaleOrderCustomerChangeDialog
      v-if="showCustomerChangeDialog"
      v-model="showCustomerChangeDialog"
      @apply="applyCustomerChange"
    />

    <SaleOrderSaveFeedbackDialogs
      v-if="saveErrorDialogVisible || saveSuccessDialogVisible"
      v-model:error-visible="saveErrorDialogVisible"
      v-model:success-visible="saveSuccessDialogVisible"
      :error-message="saveErrorMessage"
      :success-title="successDialogTitle"
      :success-message="successDialogMessage"
      :success-order-no="saveSuccessOrderNo"
      :mode="saveSuccessDialogMode"
      :can-print-saved-order="canPrintSavedOrder"
      :can-approve-saved-order="canApproveSavedOrder"
      @success-closed="handleSaveSuccessDialogClosed"
      @continue-create="handleContinueCreate"
      @stay-current="handleStayOnCurrentOrder"
      @back-to-list="handleBackToList"
      @print-saved-order="handlePrintSavedOrder"
      @approve-saved-order="handleApproveSavedOrder"
    />

    <SaleOrderQuickAssemblyDialog
      v-if="assemblyQuickDialogVisible"
      v-model="assemblyQuickDialogVisible"
      :loading="assemblyQuickLoading"
      :saving="assemblyQuickSaving"
      :row="assemblyQuickRow"
      :template-id="assemblyQuickTemplateId"
      :form="assemblyQuickForm"
      :warehouse-options="warehouseOptions"
      :location-options="locationOptions"
      :can-approve="canQuickApproveAssembly"
      :get-templates="getAssemblyTemplatesForProduct"
      :format-template-label="formatAssemblyTemplateOptionLabel"
      :resolve-item-product-label="resolveAssemblyItemProductLabel"
      :format-plain-number="formatPlainNumber"
      @template-change="handleAssemblyTemplateChange"
      @qty-change="handleAssemblyQtyChange"
      @finished-stock-change="handleAssemblyFinishedStockChange"
      @item-stock-change="handleAssemblyItemStockChange"
      @save="saveAssemblyQuickOrder"
    />

    <SaleOrderPrintPreview
      v-if="printDialogVisible"
      v-model="printDialogVisible"
      :doc-type="printDocType"
      :doc-id="printDocId"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onMounted, onBeforeUnmount, onActivated, onDeactivated, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useValidationMessage } from '@/composables/useValidationMessage';
import DecimalInput from '@/components/DecimalInput.vue';
import { mergeOptionById } from '@/utils/erpMasterData';
import { ElMessageBox } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import {
  getCachedCustomerCategories,
  getCachedEnabledDeliveryMethods,
  getCachedEnabledReceiptMethods,
  getCachedEnabledSettlementMethods,
  getCachedLocationOptions,
  getCachedProductOptions,
  getCachedWarehouseOptions
} from '@/composables/erpBaseDataCache';
import type {
  AssemblyQuickItem,
  AssemblyTemplateOption,
  CodeOptionItem,
  ErpProductDetail,
  HistoryDialogTabState,
  OptionItem,
  PriceHistoryItem,
  ProductOption,
  PurchaseHistoryItem,
  SaleHistoryItem,
  SaleOrderItem,
  StockOption
} from './sale-order/saleOrderTypes';
import {
  calcLineAmount,
  formatMoney,
  formatPlainNumber,
  formatQuickDecimal,
  formatRate,
  parsePositiveDecimal
} from './sale-order/saleOrderNumberUtils';
import { useSaleOrderPricing } from './sale-order/useSaleOrderPricing';
import { useSaleOrderBaseData } from './sale-order/useSaleOrderBaseData';
import { useSaleOrderApprovedActions } from './sale-order/useSaleOrderApprovedActions';
import { useSaleOrderHistory } from './sale-order/useSaleOrderHistory';
import { useSaleOrderProductSelection } from './sale-order/useSaleOrderProductSelection';
import { useSaleOrderQuickAssembly } from './sale-order/useSaleOrderQuickAssembly';
import { useSaleOrderSaveFlow } from './sale-order/useSaleOrderSaveFlow';
import { useSaleOrderStockSelection } from './sale-order/useSaleOrderStockSelection';
import SaleOrderDetailTable from './sale-order/SaleOrderDetailTable.vue';
import SaleOrderHeaderForm from './sale-order/SaleOrderHeaderForm.vue';

const SaleOrderCustomerChangeDialog = defineAsyncComponent(() => import('./SaleOrderCustomerChangeDialog.vue'));
const SaleOrderProductEditBridge = defineAsyncComponent(() => import('./SaleOrderProductEditBridge.vue'));
const SaleOrderProductHistoryDialog = defineAsyncComponent(() => import('./SaleOrderProductHistoryDialog.vue'));
const SaleOrderPrintPreview = defineAsyncComponent(() => import('./SaleOrderPrintPreview.vue'));
const SaleOrderQuickAssemblyDialog = defineAsyncComponent(() => import('./SaleOrderQuickAssemblyDialog.vue'));
const SaleOrderSaveFeedbackDialogs = defineAsyncComponent(() => import('./SaleOrderSaveFeedbackDialogs.vue'));

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const props = defineProps<{
  workspace?: 'draft' | 'approved';
}>();
const isSaleOrderRoute = () => route.path.startsWith('/erp/sale-orders');
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { requiredFieldMessage, positiveRowFieldMessage, invalidRowFieldMessage } = useValidationMessage();
const authStore = useAuthStore();
const isSaving = ref(false);
const tenantCacheKey = computed(() => String(authStore.tenantId ?? authStore.tenantCode ?? 'default'));

const isEditing = computed(() => Boolean(route.params.id));
const isApprovedWorkspace = computed(() => props.workspace === 'approved' || route.path.startsWith('/erp/sale-orders/approved'));
const isDraftWorkspace = computed(() => props.workspace === 'draft' || route.path.startsWith('/erp/sale-orders/draft') || !isApprovedWorkspace.value);
const detailApiBase = computed(() => (isApprovedWorkspace.value ? '/erp/sale-orders/approved' : '/erp/sale-orders/draft'));
const printDocType = computed(() => (isApprovedWorkspace.value ? 'SALE_ORDER_APPROVED' : 'SALE_ORDER_DRAFT'));
const isReadOnly = computed(() => {
  if (isApprovedWorkspace.value) return true;
  if (route.query.mode === 'view') return true;
  if (!formData.status) return false;
  return formData.status !== 'DRAFT';
});

const canCopy = computed(() => {
  return isReadOnly.value
    && (formData.status === 'APPROVED' || formData.status === 'RED_FLUSHED')
    && hasPermission('erp-sale-approved:copy')
    && hasPermission('erp-sale-draft:add');
});
const shouldShowCopyButton = computed(() => {
  if (canCopy.value) return true;
  return isInitializing.value
    && isEditing.value
    && (route.query.mode === 'view' || route.query.from === 'approved')
    && hasPermission('erp-sale-approved:copy');
});

const canPrint = computed(() => {
  if (!isEditing.value) return !isApprovedWorkspace.value && hasPermission('erp-sale-draft:print');
  return hasPermission(isApprovedWorkspace.value ? 'erp-sale-approved:print' : 'erp-sale-draft:print');
});
const canPrintSavedOrder = computed(() => {
  return Boolean(saveSuccessOrderId.value) && hasPermission('erp-sale-draft:print');
});

const currentOperatorName = computed(() => {
  const user = authStore.user as any;
  return user?.displayName || user?.username || user?.name || 'system';
});

const canRedFlush = computed(() => {
  return isReadOnly.value && formData.status === 'APPROVED' && hasPermission('erp-sale-approved:redflush');
});
const canCancel = computed(() => {
  return isReadOnly.value && formData.status === 'APPROVED' && hasPermission('erp-sale-approved:cancel');
});
const shouldShowRedFlushButton = computed(() => {
  if (canRedFlush.value) return true;
  return isInitializing.value
    && isEditing.value
    && (route.query.mode === 'view' || route.query.from === 'approved')
    && hasPermission('erp-sale-approved:redflush');
});

const pageTitle = computed(() => {
  if (isReadOnly.value) return t('page.erpSaleOrder');
  return isEditing.value ? t('page.erpSaleOrderEdit') : t('page.erpSaleOrderCreate');
});

const customerOptions = ref<OptionItem[]>([]);
const productOptions = ref<ProductOption[]>([]);
const productSearchOptions = ref<ProductOption[]>([]);
const productSearchLoading = ref(false);
const productSearchTimer = ref<number | null>(null);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);
const settlementMethodOptions = ref<CodeOptionItem[]>([]);
const receiptMethodOptions = ref<CodeOptionItem[]>([]);
const deliveryMethodOptions = ref<CodeOptionItem[]>([]);
const productStockMap = ref<Record<number, StockOption[]>>({});

const formData = reactive({
  orderNo: '',
  orderAt: '',
  status: '',
  customerId: null as number | null,
  settlementMethod: '',
  receiptMethodCode: '',
  deliveryMethod: '',
  paidAmount: '',
  discountAmount: '',
  customerDebtTotal: '',
  createdBy: '',
  updatedBy: '',
  remark: '',
  items: [] as SaleOrderItem[]
});

const pagePath = ref(route.path);
const lastRouteKey = ref(route.fullPath);
let loadDetailSeq = 0;
const showCustomerChangeDialog = ref(false);
const pendingCustomerId = ref<number | null>(null);
const lastCustomerId = ref<number | null>(null);
const isInitializing = ref(false);
const needsReload = ref(false);
const isPageActive = ref(false);
const showProfitColumn = ref(false);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const pendingPrintDocId = ref<number | null>(null);
const saveErrorDialogVisible = ref(false);
const saveErrorMessage = ref('');
const saveSuccessDialogVisible = ref(false);
const saveSuccessOrderId = ref<number | null>(null);
const saveSuccessOrderNo = ref('');
const saveSuccessDialogMode = ref<'save' | 'approve'>('save');
const activeRowIndex = ref<number | null>(null);
const selectedItems = ref<SaleOrderItem[]>([]);
const productSelectRefs = ref<any[]>([]);
const customerCategoryOptions = ref<OptionItem[]>([]);
const assemblyTemplateMap = ref<Record<number, AssemblyTemplateOption[]>>({});
const productEditDrawerVisible = ref(false);
const productEditProductId = ref<number | null>(null);
const productEditRow = ref<SaleOrderItem | null>(null);
const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canApprove = computed(() => {
  return !isReadOnly.value && formData.status === 'DRAFT' && hasPermission('erp-sale-draft:approve');
});

const shouldShowApproveButton = computed(() => {
  if (canApprove.value) return true;
  if (!isInitializing.value) return false;
  if (route.query.mode === 'view') return false;
  return hasPermission('erp-sale-draft:approve');
});

const canApproveSavedOrder = computed(() => {
  return Boolean(saveSuccessOrderId.value) && hasPermission('erp-sale-draft:approve');
});
const successDialogTitle = computed(() => {
  return saveSuccessDialogMode.value === 'approve'
    ? t('message.approveSuccess')
    : t('message.saveSuccess');
});
const successDialogMessage = computed(() => {
  return saveSuccessDialogMode.value === 'approve'
    ? t('message.approveSuccessNextStep')
    : t('message.saveSuccessNextStep');
});

const canViewProfit = computed(() => {
  return hasPermission('column:erp-sale-form:profit')
    && (hasPermission('erp-product:cost:view') || hasPermission('erp-product:cost:edit'));
});

const canShowProfit = computed(() => canViewProfit.value && showProfitColumn.value);
const canShowDiscountAllocated = computed(() => hasPermission('column:erp-sale-form:discountAllocated'));
const canEditProductInline = computed(() => !isReadOnly.value && hasPermission('erp-product:edit'));
const canUseQuickAssembly = computed(() => {
  return !isReadOnly.value
    && hasPermission('erp-assemble-order:view')
    && hasPermission('erp-assemble-order:add');
});
const canQuickApproveAssembly = computed(() => hasPermission('erp-assemble-order:approve'));
const showCustomerDebtTotal = computed(() => formData.status === 'APPROVED');
const currentCustomerName = computed(() => {
  if (!formData.customerId) return '-';
  return customerOptions.value.find(item => item.id === formData.customerId)?.name || '-';
});
const currentDeliveryMethodName = computed(() => {
  if (!formData.deliveryMethod) return '-';
  return deliveryMethodOptions.value.find(item => item.code === formData.deliveryMethod)?.name || formData.deliveryMethod;
});
const currentSettlementMethodName = computed(() => {
  if (!formData.settlementMethod) return '-';
  return settlementMethodOptions.value.find(item => item.code === formData.settlementMethod)?.name || formData.settlementMethod;
});
const currentReceiptMethodName = computed(() => {
  if (!formData.receiptMethodCode) return '-';
  return receiptMethodOptions.value.find(item => item.code === formData.receiptMethodCode)?.name || formData.receiptMethodCode;
});
const isCreditSettlement = computed(() => {
  if (!formData.settlementMethod) return false;
  const code = String(formData.settlementMethod).toUpperCase();
  if (code === 'CREDIT' || code === 'ON_ACCOUNT' || code === 'AR') return true;
  const selected = settlementMethodOptions.value.find(item => item.code === formData.settlementMethod);
  if (selected?.fundInputMode === 'HIDDEN') return true;
  if (!selected || !selected.name) return false;
  return String(selected.name).includes('挂账');
});

const isTypingTarget = (target: EventTarget | null) => {
  if (!target || !(target instanceof HTMLElement)) return false;
  const tag = target.tagName.toLowerCase();
  if (tag === 'input' || tag === 'textarea' || tag === 'select') return true;
  return target.isContentEditable;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (isTypingTarget(event.target)) return;
  if (event.key && event.key.toLowerCase() === 'h' && (event.ctrlKey || event.metaKey)) {
    event.preventDefault();
    if (activeRowIndex.value == null) return;
    const row = formData.items[activeRowIndex.value];
    if (row) {
      openHistoryForRow(row);
    }
    return;
  }
  if (!canViewProfit.value) return;
  if (event.key && event.key.toLowerCase() === 'u') {
    if (event.repeat) return;
    event.preventDefault();
    showProfitColumn.value = !showProfitColumn.value;
  }
};

const normalizeArray = <T>(value: any): T[] => {
  if (Array.isArray(value)) return value as T[];
  if (Array.isArray(value?.items)) return value.items as T[];
  if (Array.isArray(value?.list)) return value.list as T[];
  return [];
};

const getAssemblyTemplatesForProduct = (productId?: number | null) => {
  if (!productId) return [];
  return assemblyTemplateMap.value[productId] || [];
};

const findKnownProduct = (productId?: number | null) => {
  if (!productId) return undefined;
  return productOptions.value.find(item => item.id === productId);
};

const {
  handleHistoryDialogFilterChange,
  handleHistoryDialogPageChange,
  handleHistoryDialogSizeChange,
  handleHistoryTabChange,
  historyDialogVisible,
  historyLoading,
  historyOrderDialogTitle,
  historyOrderDialogUrl,
  historyOrderDialogVisible,
  historyTab,
  openHistoryForRow,
  saleHistoryHeaderItems,
  saleHistoryTabs
} = useSaleOrderHistory({
  activeRowIndex,
  customerCategoryOptions,
  findKnownProduct,
  formData,
  formatHistoryDate: (value: any) => normalizeDateTimeValue(value),
  formatMoney,
  normalizeArray,
  notifyError,
  notifyWarning,
  request,
  router,
  t
});

const {
  applyProductDefaults,
  buildLocationOnlyOptions,
  buildStockKey,
  buildStockOptionLabel,
  ensureStockBinding,
  fetchStockOptions,
  getLocationOptions,
  getStockOptionsForRow,
  resolveLocationLabel,
  resolveProductLabel,
  resolveWarehouseLabel,
  syncStockKey
} = useSaleOrderStockSelection({
  locationOptions,
  notifyError,
  productOptions,
  productStockMap,
  request,
  t,
  warehouseOptions,
  findKnownProduct
});

const {
  applyDefaultMethods,
  applyMethodsForCustomer,
  customerSearchLoading,
  ensureCustomerOption,
  ensureLocationOption,
  ensureWarehouseOption,
  fetchCustomerCategories,
  fetchDeliveryMethods,
  fetchLocations,
  fetchReceiptMethods,
  fetchSettlementMethods,
  fetchWarehouses,
  getDefaultDeliveryMethod,
  getDefaultReceiptMethod,
  getDefaultSettlementMethod,
  resolveSettlementMethodCode,
  searchCustomers
} = useSaleOrderBaseData({
  applyProductDefaults,
  customerCategoryOptions,
  customerOptions,
  deliveryMethodOptions,
  formData,
  getCachedCustomerCategories,
  getCachedEnabledDeliveryMethods,
  getCachedEnabledReceiptMethods,
  getCachedEnabledSettlementMethods,
  getCachedLocationOptions,
  getCachedWarehouseOptions,
  isCreditSettlement,
  isEditing,
  locationOptions,
  notifyError,
  receiptMethodOptions,
  request,
  settlementMethodOptions,
  tenantCacheKey,
  warehouseOptions
});

const isAssemblyProduct = (row: SaleOrderItem) => {
  if (!row.productId) return false;
  const product = findKnownProduct(row.productId);
  return product?.productType === 'ASSEMBLY';
};

const formatAssemblyTemplateOptionLabel = (item: AssemblyTemplateOption) => {
  if (!item.remark) return item.name;
  return `${item.name} / ${item.remark}`;
};

const fetchAssemblyTemplatesByProductId = async (productId?: number | null, force = false): Promise<AssemblyTemplateOption[]> => {
  if (!productId || !canUseQuickAssembly.value) return [];
  if (!force && Object.prototype.hasOwnProperty.call(assemblyTemplateMap.value, productId)) {
    return assemblyTemplateMap.value[productId] || [];
  }
  try {
    const res: any = await request.get('/erp/assembly-templates/by-finished-product', {
      params: { orderType: 'ASSEMBLE', productId }
    });
    const templates = normalizeArray<AssemblyTemplateOption>(res?.data?.data);
    assemblyTemplateMap.value = {
      ...assemblyTemplateMap.value,
      [productId]: templates
    };
    return templates;
  } catch (error) {
    assemblyTemplateMap.value = {
      ...assemblyTemplateMap.value,
      [productId]: []
    };
    notifyError(error);
    return [];
  }
};

const getReturnPath = () => {
  const returnTo = route.query.returnTo;
  if (typeof returnTo === 'string' && returnTo.trim()) {
    return returnTo.trim();
  }
  if (route.query.from === 'draft') {
    return '/erp/sale-orders/draft';
  }
  if (route.query.from === 'approved' || route.query.mode === 'view' || formData.status === 'APPROVED') {
    return '/erp/sale-orders/approved';
  }
  return '/erp/sale-orders/draft';
};

const closePage = (redirectPath = getReturnPath()) => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path: route.path, redirectPath } }));
  }
};

const closeTagByPath = (path: string) => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path } }));
  }
};

const {
  handleCancel,
  handleCopy,
  handleRedFlush
} = useSaleOrderApprovedActions({
  closePage,
  ensureStockBinding,
  formData,
  formatDateTime: (date: Date) => formatDateTime(date),
  hasPermission,
  isEditing,
  notifyError,
  notifySuccess,
  notifyWarning,
  request,
  route,
  router,
  t
});

const handleBack = async () => {
  if (isEditing.value) {
    resetForm();
    closePage();
    return;
  }

  const hasItems = formData.items.some(item => item.productId);
  if (hasItems) {
    try {
      await ElMessageBox.confirm(
        t('message.confirmSaveDraft'),
        t('action.confirm'),
        {
          confirmButtonText: t('action.save'),
          cancelButtonText: t('action.cancel'),
          type: 'warning'
        }
      );
      await saveData();
    } catch {
      resetForm();
      closePage();
    }
    return;
  }

  try {
    await ElMessageBox.confirm(
      t('message.confirmClosePage'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
    resetForm();
    closePage();
  } catch {
    return;
  }
};

const getCurrentCustomerCategoryId = () => {
  if (!formData.customerId) return undefined;
  return customerOptions.value.find(item => item.id === formData.customerId)?.categoryId;
};

const {
  applyPriceForRow,
  calcLineDiscount,
  calcLineNetAmount,
  calcLineProfit,
  formatProfitCell,
  getDiscountAmount,
  totalProfitRateText,
  totalProfitText,
  totalSummary
} = useSaleOrderPricing({
  formData,
  request,
  notifyError,
  normalizeArray,
  findKnownProduct,
  getCurrentCustomerCategoryId
});

const {
  ensureProductOption,
  fetchProducts,
  getSelectableProductOptions,
  handleInlineProductSaved,
  openProductEditFromOption,
  rememberProductOption,
  rememberProductOptions,
  searchProducts
} = useSaleOrderProductSelection({
  activeRowIndex,
  applyPriceForRow,
  applyProductDefaults,
  canEditProductInline,
  fetchAssemblyTemplatesByProductId,
  fetchStockOptions,
  findKnownProduct,
  formData,
  getCachedProductOptions,
  notifyError,
  productEditDrawerVisible,
  productEditProductId,
  productEditRow,
  productOptions,
  productSearchLoading,
  productSearchOptions,
  productSearchTimer,
  request,
  syncStockKey,
  tenantCacheKey
});

const handleProductChange = async (row: SaleOrderItem) => {
  if (!formData.customerId) {
    row.productId = undefined;
    row.warehouseId = undefined;
    row.locationId = undefined;
    row.stockKey = '';
    row.price = '';
    row._priceRequestSeq = (row._priceRequestSeq || 0) + 1;
    return;
  }
  activeRowIndex.value = formData.items.indexOf(row);
  row.warehouseId = undefined;
  row.locationId = undefined;
  row.stockKey = '';
  row.price = '';
  row._priceRequestSeq = (row._priceRequestSeq || 0) + 1;
  applyProductDefaults(row, true);
  await fetchStockOptions(row.productId, true);
  syncStockKey(row);
  await fetchAssemblyTemplatesByProductId(row.productId);
  await applyPriceForRow(row, true);
  await ensureNextItemAfterCompletedProduct(row);
};

const handleRowClick = (row: SaleOrderItem) => {
  activeRowIndex.value = formData.items.indexOf(row);
};

const handleStockLocationChange = (row: SaleOrderItem) => {
  if (!row.productId || !row.stockKey) {
    row.warehouseId = undefined;
    row.locationId = undefined;
    return;
  }
  const options = getStockOptionsForRow(row);
  const selected = options.find(item => item.key === row.stockKey);
  if (!selected) {
    row.warehouseId = undefined;
    row.locationId = undefined;
    return;
  }
  row.warehouseId = selected.warehouseId ?? undefined;
  row.locationId = selected.locationId ?? undefined;
};

const {
  assemblyQuickDialogVisible,
  assemblyQuickForm,
  assemblyQuickLoading,
  assemblyQuickRow,
  assemblyQuickSaving,
  assemblyQuickTemplateId,
  getProductNameById,
  handleAssemblyFinishedStockChange,
  handleAssemblyItemStockChange,
  handleAssemblyQtyChange,
  handleAssemblyTemplateChange,
  openAssemblyForRow,
  resetAssemblyQuickForm,
  resolveAssemblyItemProductLabel,
  saveAssemblyQuickOrder
} = useSaleOrderQuickAssembly({
  activeRowIndex,
  buildStockKey,
  ensureLocationOption,
  ensureProductOption,
  ensureWarehouseOption,
  fetchAssemblyTemplatesByProductId,
  fetchStockOptions,
  findKnownProduct,
  formData,
  formatDateTime: (date: Date) => formatDateTime(date),
  locationOptions,
  normalizeArray,
  notifyError,
  notifySuccess,
  notifyWarning,
  request,
  resolveProductLabel,
  route,
  syncStockKey,
  t
});

const handleCustomerChange = (value: number | null) => {
  if (isInitializing.value) {
    lastCustomerId.value = value;
    return;
  }
  if (value === lastCustomerId.value) return;
  const hasItems = formData.items.some(item => item.productId);
  if (hasItems) {
    pendingCustomerId.value = value;
    showCustomerChangeDialog.value = true;
    return;
  }
  lastCustomerId.value = value;
  applyMethodsForCustomer();
};

const applyCustomerChange = async (action: 'price' | 'clear' | 'cancel') => {
  const targetCustomerId = pendingCustomerId.value;
  showCustomerChangeDialog.value = false;
  if (action === 'cancel') {
    formData.customerId = lastCustomerId.value;
    pendingCustomerId.value = null;
    return;
  }
  lastCustomerId.value = targetCustomerId;
  pendingCustomerId.value = null;
  if (action === 'clear') {
    formData.items = [];
    addItem();
    applyMethodsForCustomer();
    return;
  }
  for (const item of formData.items) {
    await applyPriceForRow(item, true);
  }
  applyMethodsForCustomer();
};

const loadDetail = async () => {
  const seq = ++loadDetailSeq;
  isInitializing.value = true;
  const editing = isEditing.value;
  try {
    if (!editing) {
      resetForm();
      initializeDraftShell();
      isInitializing.value = false;
      void fetchNextOrderNo(seq);
      return;
    }
    resetForm();
    const id = route.params.id;
    const res: any = await request.get(`${detailApiBase.value}/${id}`);
    if (seq !== loadDetailSeq) return;
    if (res.data.code === 200) {
      const data = res.data.data || {};
      formData.orderNo = data.order?.orderNo || data.orderNo || '';
      formData.status = data.order?.status || data.status || '';
      formData.customerId = data.order?.customerId || data.customerId || null;
      await ensureCustomerOption(formData.customerId);
      formData.orderAt = normalizeDateTimeValue(data.order?.orderAt || data.orderAt) || formatDateTime(new Date());
      formData.remark = data.order?.remark || data.remark || '';
      formData.settlementMethod = data.order?.settlementMethod || data.settlementMethod || '';
      formData.receiptMethodCode = data.order?.receiptMethodCode || data.receiptMethodCode || '';
      formData.deliveryMethod = data.order?.deliveryMethod || data.deliveryMethod || '';
      formData.paidAmount = String(data.order?.paidAmount ?? data.paidAmount ?? '');
      formData.discountAmount = String(data.order?.discountAmount ?? data.discountAmount ?? '');
      formData.customerDebtTotal = String(data.customerDebtTotal ?? data.order?.customerDebtTotal ?? '');
      formData.createdBy = data.order?.createdBy || data.createdBy || '';
      formData.updatedBy = data.order?.updatedBy || data.updatedBy || formData.createdBy;
      formData.items = (data.items || data.order?.items || []).map((item: any) => ({
        id: item.id,
        productId: item.productId,
        productName: item.productName,
        warehouseId: item.warehouseId,
        locationId: item.locationId,
        stockKey: '',
        qty: item.qty == null ? '' : String(item.qty),
        price: item.price == null ? '' : String(item.price),
        unitCost: item.unitCost == null ? undefined : Number(item.unitCost),
        taxRate: item.taxRate,
        remark: item.remark
      }));
      await Promise.all(formData.items.flatMap(item => [
        ensureProductOption(item.productId),
        ensureWarehouseOption(item.warehouseId),
        ensureLocationOption(item.locationId)
      ]));
      for (const item of formData.items) {
        await fetchStockOptions(item.productId);
        syncStockKey(item);
      }
      await Promise.all(formData.items.map(item => fetchAssemblyTemplatesByProductId(item.productId)));
      if (!formData.items.length) addItem();
      lastCustomerId.value = formData.customerId;
    }
  } catch (error) {
    if (seq === loadDetailSeq) {
      notifyError(error);
    }
  } finally {
    if (seq === loadDetailSeq) {
      isInitializing.value = false;
    }
  }
};

const addItem = () => {
  const item: SaleOrderItem = {
    productId: undefined,
    warehouseId: undefined,
    locationId: undefined,
    stockKey: '',
    qty: '',
    price: '',
    unitCost: undefined,
    taxRate: 0,
    remark: ''
  };
  formData.items.push(item);
  return item;
};

const setProductSelectRef = (el: any, index: number) => {
  productSelectRefs.value[index] = el;
};

const focusProductSelectAt = async (index: number) => {
  await nextTick();
  productSelectRefs.value[index]?.focus?.();
};

const ensureNextItemAfterCompletedProduct = async (row: SaleOrderItem) => {
  if (isReadOnly.value || !row.productId) return;
  const rowIndex = formData.items.indexOf(row);
  if (rowIndex === -1 || rowIndex !== formData.items.length - 1) return;
  addItem();
  activeRowIndex.value = rowIndex + 1;
  await focusProductSelectAt(rowIndex + 1);
};

const handleItemSelectionChange = (rows: SaleOrderItem[]) => {
  selectedItems.value = rows;
};

const removeItem = (index: number) => {
  const [removed] = formData.items.splice(index, 1);
  selectedItems.value = selectedItems.value.filter(item => item !== removed);
  if (!formData.items.length && !isReadOnly.value) {
    addItem();
  }
};

const removeSelectedItems = () => {
  if (!selectedItems.value.length) return;
  const selectedSet = new Set(selectedItems.value);
  formData.items = formData.items.filter(item => !selectedSet.has(item));
  selectedItems.value = [];
  if (!formData.items.length) {
    addItem();
  }
};

const resetForm = () => {
  formData.orderNo = '';
  formData.orderAt = '';
  formData.status = '';
  formData.customerId = null;
  formData.settlementMethod = '';
  formData.receiptMethodCode = '';
  formData.deliveryMethod = '';
  formData.paidAmount = '';
  formData.discountAmount = '';
  formData.customerDebtTotal = '';
  formData.createdBy = '';
  formData.updatedBy = '';
  formData.remark = '';
  formData.items = [];
  productSearchOptions.value = [];
  if (productSearchTimer.value != null && typeof window !== 'undefined') {
    window.clearTimeout(productSearchTimer.value);
    productSearchTimer.value = null;
  }
  productSearchLoading.value = false;
  selectedItems.value = [];
  assemblyQuickDialogVisible.value = false;
  resetAssemblyQuickForm();
};

const initializeDraftShell = () => {
  formData.orderAt = formatDateTime(new Date());
  formData.status = 'DRAFT';
  if (!formData.items.length) {
    addItem();
  }
  applyDefaultMethods();
  lastCustomerId.value = formData.customerId;
};

const fetchNextOrderNo = async (seq?: number) => {
  try {
    const res: any = await request.get('/erp/sale-orders/draft/next-order-no');
    if (seq != null && seq !== loadDetailSeq) {
      return;
    }
    if (res.data.code === 200) {
      formData.orderNo = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const {
  handleApprove,
  handleApproveSavedOrder,
  handleBackToList,
  handleContinueCreate,
  handlePrint,
  handlePrintSavedOrder,
  handleSave,
  handleSaveAndBack,
  handleSaveSuccessDialogClosed,
  handleStayOnCurrentOrder,
  openSaveSuccessDialog,
  saveData
} = useSaleOrderSaveFlow({
  closePage,
  closeTagByPath,
  ensureStockBinding,
  formData,
  getReturnPath,
  invalidRowFieldMessage,
  isCreditSettlement,
  isEditing,
  isReadOnly,
  isSaving,
  loadDetail,
  nextTick,
  notifyError,
  notifySuccess,
  notifyWarning,
  pendingPrintDocId,
  positiveRowFieldMessage,
  printDialogVisible,
  printDocId,
  request,
  requiredFieldMessage,
  route,
  router,
  saveErrorDialogVisible,
  saveErrorMessage,
  saveSuccessDialogMode,
  saveSuccessDialogVisible,
  saveSuccessOrderId,
  saveSuccessOrderNo,
  t
});

const formatDateTime = (date: Date) => {
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const normalizeDateTimeValue = (value: any) => {
  if (!value) return '';
  if (value instanceof Date) {
    return formatDateTime(value);
  }
  const raw = String(value);
  if (!raw) return '';
  if (raw.includes('T')) {
    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
      return '';
    }
    return formatDateTime(parsed);
  }
  return raw;
};

const handleTagClosing = (event: Event) => {
  const customEvent = event as CustomEvent<{ path?: string }>;
  if (customEvent.detail?.path === pagePath.value) {
    needsReload.value = true;
    resetForm();
  }
};

watch(
  () => route.fullPath,
  (newPath) => {
    if (newPath === lastRouteKey.value) return;
    if (!isPageActive.value) return;
    if (!isSaleOrderRoute()) return;
    lastRouteKey.value = newPath;
    pagePath.value = route.path;
    loadDetail();
  }
);

watch(
  () => formData.settlementMethod,
  () => {
    if (isCreditSettlement.value) {
      formData.receiptMethodCode = '';
      formData.paidAmount = '0';
    } else if (!formData.receiptMethodCode) {
      formData.receiptMethodCode = getDefaultReceiptMethod();
    }
  }
);

onMounted(() => {
  isPageActive.value = true;
  pagePath.value = route.path;
  fetchCustomerCategories();
  fetchWarehouses();
  fetchLocations();
  fetchSettlementMethods();
  fetchReceiptMethods();
  fetchDeliveryMethods();
  loadDetail();
  if (typeof window !== 'undefined') {
    window.addEventListener('tags:closing', handleTagClosing as EventListener);
    window.addEventListener('tags:close', handleTagClosing as EventListener);
    window.addEventListener('keydown', handleKeydown);
  }
});

onActivated(() => {
  isPageActive.value = true;
  if (!isSaleOrderRoute()) return;
  if (!needsReload.value) return;
  needsReload.value = false;
  loadDetail();
});

onDeactivated(() => {
  isPageActive.value = false;
});

onBeforeUnmount(() => {
  isPageActive.value = false;
  if (typeof window !== 'undefined') {
    window.removeEventListener('tags:closing', handleTagClosing as EventListener);
    window.removeEventListener('tags:close', handleTagClosing as EventListener);
    window.removeEventListener('keydown', handleKeydown);
  }
  resetForm();
});
</script>

<style scoped>
:global(.content-area:has(.sale-page-surface)) {
  background: #ffffff;
}

.sale-page-surface {
  --sale-page-bg: #ffffff;
  --sale-card-bg: #ffffff;
  --sale-card-border: #e3eaf4;
  --sale-card-shadow: 0 16px 36px rgba(28, 45, 76, 0.08), 0 4px 12px rgba(28, 45, 76, 0.04);
  --sale-card-radius: 12px;
  --sale-text: #17233c;
  --sale-muted: #6d7b91;
  --sale-primary: #1677ff;
  --sale-danger: #ff4d4f;
  --sale-control-border: #d7e0ec;
  min-height: 100%;
  height: auto;
  padding: 16px 20px;
  box-sizing: border-box;
  background: transparent;
  color: var(--sale-text);
}

.sale-page-surface .page-title {
  color: var(--sale-text);
  font-weight: 800;
  font-size: 24px;
  line-height: 32px;
  letter-spacing: 0;
}

.sale-page-surface .sale-header-card,
.sale-page-surface .sale-detail-card,
.sale-page-surface .payment-card {
  border: 1px solid var(--sale-card-border);
  border-radius: var(--sale-card-radius);
  background: var(--sale-card-bg);
  box-shadow: var(--sale-card-shadow);
  flex: 0 0 auto;
}

.sale-page-surface .sale-header-card {
  padding: 20px 22px;
}

.sale-page-surface .sale-header-card,
.sale-page-surface .sale-detail-card,
.sale-page-surface .payment-card {
  overflow: visible;
}

.sale-page-surface .sale-header-stack {
  gap: 16px;
}

.sale-page-surface .sale-header-body,
.sale-page-surface .sale-detail-body,
.sale-page-surface .payment-card-body {
  background: transparent;
}

.sale-page-surface .card-section-header h4 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--sale-text);
  font-size: 15px;
  font-weight: 700;
}

.sale-page-surface .card-section-header h4::before {
  content: '';
  width: 4px;
  height: 20px;
  border-radius: 999px;
  background: var(--sale-primary);
}

.sale-page-surface :deep(.el-form-item__label) {
  color: var(--sale-muted);
  font-weight: 600;
  line-height: 18px;
  padding-bottom: 6px;
}

.sale-page-surface .detail-table-wrapper {
  border: 1px solid #e1e9f4;
  border-radius: 10px;
  background: #fbfdff;
  overflow: hidden;
}

.sale-page-surface :deep(.el-table) {
  --el-table-border-color: #e1e9f4;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #fbfdff;
  color: var(--sale-text);
}

.sale-page-surface :deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #26344f;
  font-weight: 700;
}

.sale-page-surface .readonly-field,
.sale-page-surface .readonly-inline,
.sale-page-surface .readonly-cell {
  color: var(--sale-text);
}

.sale-page-surface .readonly-inline--debt {
  color: var(--sale-danger);
}

.sale-page-surface .summary-item:last-child,
.sale-page-surface .detail-summary .summary-item:first-child {
  color: var(--sale-primary);
  font-weight: 700;
}

.sale-page-header {
  align-items: center !important;
  margin-bottom: 16px !important;
  gap: 14px;
}

.sale-title-group {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.sale-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: #7d889b;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.sale-breadcrumb__separator {
  color: #a8b2c1;
}

.card-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.sale-header-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sale-page-toolbar__filters {
  flex: 1 1 220px;
}

.sale-page-toolbar__actions {
  justify-content: flex-end;
}

.sale-page-surface .sale-page-toolbar__actions {
  gap: 16px;
}

.sale-page-surface .action-button {
  min-width: 78px;
  height: 36px;
  border-radius: 8px;
  border-color: #d9e2ef;
  background: #ffffff;
  color: #26344f;
  font-size: 14px;
  box-shadow: none;
}

.sale-page-surface .action-button:hover,
.sale-page-surface .action-button:focus-visible {
  border-color: #9eb2ce;
  background: #f8fbff;
  color: #17233d;
}

.sale-page-surface .action-button--secondary {
  border-color: #b8d2ff;
  background: #f3f8ff;
  color: #155ec9;
}

.sale-page-surface .action-button--secondary:hover,
.sale-page-surface .action-button--secondary:focus-visible {
  border-color: #7faeff;
  background: #eaf3ff;
  color: #0f56bd;
}

.sale-page-surface .action-button--secondary.is-disabled,
.sale-page-surface .action-button--secondary.is-disabled:hover {
  border-color: #c9dcff;
  background: #f3f8ff;
  color: #6f95cf;
  opacity: 0.72;
}

.sale-page-surface .action-button--save {
  border-color: #b8d2ff;
  background: #f3f8ff;
  color: #155ec9;
}

.sale-page-surface .action-button--save:hover,
.sale-page-surface .action-button--save:focus-visible {
  border-color: #7faeff;
  background: #eaf3ff;
  color: #0f56bd;
}

.sale-page-surface .action-button--save.is-disabled,
.sale-page-surface .action-button--save.is-disabled:hover {
  border-color: #c9dcff;
  background: #f3f8ff;
  color: #6f95cf;
  opacity: 0.72;
}

.sale-page-surface .action-button--primary.el-button--primary {
  background: #1677ff;
  border-color: #1677ff;
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.16);
}

.sale-page-surface .action-button--primary.el-button--primary:hover,
.sale-page-surface .action-button--primary.el-button--primary:focus-visible {
  background: #0f68e8;
  border-color: #0f68e8;
  color: #ffffff;
}

.sale-page-surface .action-button--success.el-button--success.is-plain {
  background: #eef8ee;
  border-color: #b9dfb8;
  color: #2f7d32;
}

.sale-page-surface .action-button--success.el-button--success.is-plain:hover,
.sale-page-surface .action-button--success.el-button--success.is-plain:focus-visible {
  background: #e1f2df;
  border-color: #8ecf8c;
  color: #256a28;
}

.sale-page-surface .action-button--success.el-button--success.is-plain.is-disabled,
.sale-page-surface .action-button--success.el-button--success.is-plain.is-disabled:hover {
  background: #eef8ee;
  border-color: #c8e3c7;
  color: #6a9a6c;
  opacity: 0.72;
}

.sale-page-surface :deep(.el-input__wrapper),
.sale-page-surface :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--sale-control-border) inset;
}

.sale-page-surface :deep(.el-input__wrapper:hover),
.sale-page-surface :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #a9c7f3 inset;
}

.sale-page-surface :deep(.el-input__wrapper.is-focus),
.sale-page-surface :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--sale-primary) inset, 0 0 0 3px rgba(22, 119, 255, 0.08);
}

.sale-page-surface :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f7f9fc;
  box-shadow: 0 0 0 1px var(--sale-control-border) inset;
}

.sale-page-surface :deep(.el-input__inner),
.sale-page-surface :deep(.el-select__placeholder),
.sale-page-surface :deep(.el-select__selected-item) {
  font-size: 14px;
}

.sale-page-surface :deep(.el-textarea__inner) {
  border-radius: 6px;
  border: none;
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--sale-control-border) inset;
  padding: 10px 12px;
  font-size: 14px;
  resize: vertical;
}

.sale-page-surface :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--sale-primary) inset, 0 0 0 3px rgba(22, 119, 255, 0.08);
}

.history-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.history-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
  color: #2c3e50;
}

.history-header__item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.history-header__hint {
  margin-left: auto;
  font-size: 12px;
  color: #8c8c8c;
}

.history-grid {
  display: grid;
  gap: 16px;
}

.history-grid--tabs {
  grid-template-columns: 1fr;
}

.history-card {
  border: 1px solid #e6e8ee;
  border-radius: 12px;
  padding: 12px;
  background: #fafbfc;
}

.history-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.history-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.history-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.history-search {
  width: 220px;
}

.history-date {
  width: 260px;
}

.history-pagination {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.history-order-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.history-order-frame {
  width: 100%;
  height: 78vh;
  border: none;
  display: block;
}

.action-button {
  min-width: 72px;
  font-weight: 600;
  border-radius: 10px;
}

.action-button--success {
  background: #f0f9eb;
  border-color: #b7e1a1;
  color: #4e8f2b;
}

.action-button--success:hover,
.action-button--success:focus-visible {
  background: #e3f4d8;
  border-color: #95d475;
  color: #3f7d21;
}

.sale-page-surface .action-button--danger.el-button--danger.is-plain {
  background: #fff2f2;
  border-color: #f2b8b8;
  color: #b4232a;
}

.sale-page-surface .action-button--danger.el-button--danger.is-plain:hover,
.sale-page-surface .action-button--danger.el-button--danger.is-plain:focus-visible {
  background: #ffe4e4;
  border-color: #ec8f8f;
  color: #991b1f;
}

.sale-page-surface .action-button--danger.el-button--danger.is-plain.is-disabled,
.sale-page-surface .action-button--danger.el-button--danger.is-plain.is-disabled:hover {
  background: #fff2f2;
  border-color: #f2c7c7;
  color: #c56a6e;
  opacity: 0.72;
}

.sale-form :deep(.el-form-item) {
  margin-bottom: 4px;
}

.sale-form--compact :deep(.el-form-item) {
  margin-bottom: 0;
}

.form-grid {
  display: flex;
  flex-wrap: nowrap;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.sale-header-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(190px, 1fr));
  gap: 18px 32px;
  overflow: visible;
  padding-bottom: 0;
}

.sale-page-surface .sale-header-grid .form-group {
  min-width: 0;
}

.sale-page-surface .sale-remark-item {
  width: calc(((100% - 120px) / 4) * 3 + 80px);
  max-width: 100%;
  margin-top: 18px;
}

.compact-card .form-grid {
  padding-bottom: 0;
}

.form-group {
  min-width: 220px;
}

.compact-card .table-body {
  padding: 6px 12px;
}

.compact-card {
  flex: 0 0 auto;
}

.compact-card .table-body {
  flex: 0 0 auto;
  overflow: visible;
}

.sale-header-body {
  padding: 0;
}

.sale-page-surface .sale-header-card + .sale-detail-card {
  margin-top: 18px;
}

.save-error-dialog__content {
  line-height: 1.6;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.save-success-dialog__content {
  display: flex;
  flex-direction: column;
  gap: 14px;
  line-height: 1.6;
}

.save-success-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.save-success-dialog__icon {
  color: #67c23a;
  font-size: 22px;
  flex: 0 0 auto;
}

.save-success-dialog__title {
  color: var(--el-text-color-primary);
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.save-success-dialog__message {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 500;
}

.save-success-dialog__order-no {
  color: var(--el-text-color-regular);
  font-size: 16px;
}

:deep(.save-success-dialog .el-dialog__header) {
  display: none;
}

:deep(.save-success-dialog .el-dialog__body) {
  padding: 20px 24px 18px;
}

:deep(.save-success-dialog .el-dialog__footer) {
  padding: 0 24px 24px;
}

.save-success-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
}

.save-success-dialog__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.save-success-dialog__actions--secondary {
  justify-content: flex-start;
  flex: 1 1 auto;
  min-width: 0;
}

.save-success-dialog__actions--primary {
  flex: 0 0 auto;
  margin-left: auto;
  justify-content: flex-end;
}

.save-success-dialog__actions :deep(.el-button) {
  min-width: 0;
  height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 500;
  margin-left: 0;
}

.save-success-dialog__actions--secondary :deep(.el-button) {
  width: 96px;
}

.save-success-dialog__actions--primary :deep(.el-button) {
  width: 92px;
}

@media (max-width: 640px) {
  :deep(.save-success-dialog .el-dialog) {
    width: min(520px, calc(100vw - 24px)) !important;
  }

  .save-success-dialog__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .save-success-dialog__actions {
    justify-content: center;
    flex-wrap: wrap;
  }

  .save-success-dialog__actions--primary {
    margin-left: 0;
  }
}

.sale-detail-card {
  flex: 0 0 auto;
  min-height: 0;
  padding: 18px 22px 18px;
}

.sale-detail-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: visible;
}

.detail-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
  gap: 16px;
}

.detail-header {
  margin-bottom: 8px;
}

.detail-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-toolbar-button {
  height: 32px;
  border-radius: 6px;
  border-color: #d8e1ed;
  color: #4c5b70;
  font-weight: 600;
}

.detail-toolbar-button--primary {
  border-color: var(--sale-primary);
  color: var(--sale-primary);
  background: #ffffff;
}

.detail-table-wrapper {
  flex: 0 0 auto;
  min-height: 0;
  overflow: auto;
}

.sale-page-surface :deep(.el-table td.el-table__cell) {
  height: 48px;
  color: #1f2b3d;
}

.sale-page-surface :deep(.el-table .cell) {
  line-height: 1.4;
}

.required-table-label::before {
  content: '*';
  margin-right: 4px;
  color: var(--sale-danger);
  font-weight: 700;
}

.sale-page-surface :deep(.el-table .el-input__wrapper),
.sale-page-surface :deep(.el-table .el-select__wrapper) {
  min-height: 34px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-cell__label {
  flex: 1 1 auto;
  min-width: 0;
}

.product-cell__select {
  flex: 1 1 auto;
  min-width: 0;
}

.product-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.product-option-row__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-option-row__edit {
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  min-height: 24px;
  padding: 0;
  border-radius: 4px;
}

.product-option-row__edit :deep(.el-icon) {
  font-size: 14px;
}

.history-inline {
  flex: 0 0 auto;
}

.history-tag {
  cursor: pointer;
  user-select: none;
}

.history-tag--inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 6px;
  line-height: 32px;
  background: #f7fbff;
}

.history-icon {
  font-size: 14px;
}

.assembly-inline-button {
  width: 32px;
  height: 32px;
  min-height: 32px;
  padding: 0;
  border-radius: 6px;
  background: #f1fbf4;
}

.assembly-inline-button :deep(.el-icon) {
  font-size: 16px;
}

.row-delete-button {
  font-size: 18px;
  padding: 0;
}

.history-tag.el-tag--info {
  cursor: not-allowed;
  opacity: 0.6;
}

.assembly-quick {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assembly-quick__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  padding: 10px 12px;
  border: 1px solid #e1e9f4;
  border-radius: 8px;
  background: #f8fbff;
  color: #17233c;
}

.assembly-quick__label {
  margin-right: 8px;
  color: #6d7b91;
  font-size: 13px;
}

.assembly-quick__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 12px;
}

.assembly-quick__items {
  width: 100%;
}

.readonly-field {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 5px 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
}

.sale-page-surface .readonly-field {
  min-height: 28px;
  padding: 2px 0;
}

.readonly-field--strong {
  font-weight: 600;
}

.readonly-field--remark {
  min-height: 40px;
  white-space: pre-wrap;
}

.readonly-inline {
  min-width: 92px;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  color: #303133;
  font-weight: 600;
}

.readonly-inline--debt {
  color: #c45656;
  font-size: 16px;
}

.readonly-cell {
  color: #303133;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 2px;
}

.detail-actions {
  display: flex;
  justify-content: flex-start;
}

.detail-add-item-button {
  height: 34px;
  border-radius: 6px;
  border-color: var(--sale-primary);
  background: #ffffff;
  color: var(--sale-primary);
  font-weight: 700;
}

.detail-summary {
  display: flex;
  align-items: baseline;
  gap: 26px;
  justify-content: flex-end;
  margin-left: auto;
  font-size: 14px;
  color: #26344f;
  font-weight: 600;
}

.summary-item {
  white-space: nowrap;
}

.summary-item--total strong {
  margin-left: 6px;
  color: var(--sale-primary);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0;
}

.payment-card {
  margin-top: 18px;
  padding: 18px 22px 18px;
}

.payment-card-body {
  padding: 0 !important;
  overflow: visible;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 16px 24px;
  align-items: start;
}

.payment-card .form-group {
  min-width: 0;
}

.payment-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: #4f93e8;
  font-size: 13px;
  font-weight: 500;
}

.payment-hint .el-icon {
  font-size: 15px;
}

.order-time-picker {
  width: 100%;
}

.order-time-picker :deep(.el-input__wrapper) {
  width: 100%;
  box-sizing: border-box;
}

.order-time-picker :deep(.el-input__inner) {
  width: 100%;
  padding-right: 48px;
}

.order-time-picker :deep(.el-input__suffix) {
  width: 44px;
  display: flex;
  justify-content: flex-end;
}

.table-card.compact-card--inline {
  flex: 0 0 auto !important;
  height: auto !important;
  min-height: 0 !important;
}

.table-card.compact-card--inline .compact-card-body {
  flex: 0 0 auto !important;
  height: auto !important;
  min-height: 0 !important;
  padding: 6px 12px !important;
  overflow: visible !important;
}

.sale-form--inline .form-grid {
  align-items: center;
}

.sale-form--inline :deep(.el-form-item__label) {
  line-height: 20px;
  padding-bottom: 2px;
}

.sale-form--inline :deep(.el-form-item__content) {
  line-height: 28px;
}

.assembly-template-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.assembly-template-option__name {
  flex: 0 0 auto;
  color: #1f2b3d;
  font-weight: 600;
}

.assembly-template-option__remark {
  min-width: 0;
  overflow: hidden;
  color: #7a889c;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .sale-header-grid,
  .assembly-quick__grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .sale-page-surface .sale-remark-item {
    width: 100%;
  }

  .detail-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-summary {
    flex-wrap: wrap;
    justify-content: flex-start;
    margin-left: 0;
  }
}

@media (max-width: 1024px) {
  .payment-grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 768px) {
  .sale-page-surface {
    padding: 16px;
  }

  .sale-page-header,
  .sale-title-group,
  .sale-page-toolbar__actions {
    align-items: flex-start !important;
    flex-direction: column;
  }

  .sale-page-toolbar__actions {
    width: 100%;
    margin-left: 0;
  }

  .sale-page-surface .action-button {
    width: 100%;
  }

  .sale-breadcrumb {
    flex-wrap: wrap;
    white-space: normal;
  }

  .sale-header-grid,
  .payment-grid,
  .assembly-quick__grid {
    grid-template-columns: 1fr;
  }
}
</style>
