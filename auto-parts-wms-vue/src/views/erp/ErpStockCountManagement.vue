<template>
  <div class="page-shell page-shell--system" :class="{ 'sale-page-surface': isFormPage, 'stock-doc-view-mode': isFormPage && viewMode }">
    <template v-if="!isFormPage">
    <div class="page-header">
      <div class="page-title">{{ pageTitle }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar">
          <div class="table-filters inventory-filters inventory-filters--count">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="inventory-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="inventory-field--narrow" @change="handleSearch">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.draft')" value="DRAFT" />
              <el-option :label="$t('status.approved')" value="APPROVED" />
              <el-option :label="$t('status.cancelled')" value="CANCELLED" />
              <el-option :label="$t('status.redFlushed')" value="RED_FLUSHED" />
            </el-select>
          </div>
          <div class="table-actions inventory-actions">
            <el-button v-if="countType === 'COUNT'" @click="exportTemplate">{{ $t('action.export') }}</el-button>
            <el-button v-if="countType === 'COUNT'" @click="triggerImport">{{ $t('action.import') }}</el-button>
            <el-button v-if="countType === 'INIT'" v-permission="permAdd" @click="triggerImport">导入期初库存</el-button>
            <el-button type="primary" v-permission="permAdd" @click="openAddModal">{{ $t('action.add') }}</el-button>
            <input ref="importInputRef" type="file" :accept="countType === 'INIT' ? '.xls,.xlsx' : '.csv,text/csv'" class="stock-count-import-input" @change="handleImportFile" />
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="rowClassName"
         table-key="erp-stock-count-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('countNo')" prop="countNo" :label="countNoLabel" min-width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openViewModal(row)">{{ row.countNo }}</el-button>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'APPROVED' ? 'success' : row.status === 'CANCELLED' ? 'danger' : row.status === 'RED_FLUSHED' ? 'info' : 'warning'"
                size="small"
              >
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('countAt')" prop="countAt" :label="$t('field.countAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.countAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="countType === 'COUNT' && canShow('adjustmentReason')" prop="adjustmentReason" :label="$t('field.adjustmentReason')" min-width="140">
            <template #default="{ row }">
              {{ adjustmentReasonLabel(row.adjustmentReason) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('remark')" prop="remark" :label="$t('field.remark')" min-width="200" />
          <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="280" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                v-permission="permView"
                @click="openPrintPage(row)"
              >
                {{ $t('action.print') }}
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                v-permission="permEdit"
                :disabled="row.status !== 'DRAFT'"
                @click="openEditModal(row)"
              >
                {{ $t('action.edit') }}
              </el-button>
              <el-button
                link
                type="success"
                size="small"
                v-permission="permApprove"
                :disabled="row.status !== 'DRAFT'"
                @click="handleApprove(row)"
              >
                {{ $t('action.approve') }}
              </el-button>
              <el-button
                v-if="countType === 'INIT'"
                link
                type="warning"
                size="small"
                v-permission="permRedFlush"
                :disabled="row.status !== 'APPROVED'"
                @click="handleRedFlush(row)"
              >
                {{ $t('action.redFlush') }}
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                v-permission="permCancel"
                :disabled="row.status !== 'DRAFT'"
                @click="handleCancel(row)"
              >
                {{ $t('action.cancel') }}
              </el-button>
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
    </template>

    <template v-else>
      <div class="page-header sale-page-header stock-count-form-page__header">
        <div class="sale-title-group stock-count-form-page__title-group">
          <div class="page-title">{{ formPageTitle }}</div>
          <div class="sale-breadcrumb stock-count-form-page__breadcrumb">
            <span>{{ pageTitle }}</span>
            <span class="sale-breadcrumb__separator stock-count-form-page__separator">/</span>
            <span>{{ formPageTitle }}</span>
          </div>
        </div>
        <div class="table-actions sale-page-toolbar__actions stock-count-form-page__actions">
          <el-button class="action-button" :disabled="isSaving" @click="handleBack">{{ $t('action.back') }}</el-button>
          <el-button v-if="countType === 'COUNT' && !viewMode" class="action-button action-button--secondary" :disabled="isInitializing || isSaving" @click="exportTemplate">{{ $t('action.export') }}</el-button>
          <el-button v-if="countType === 'COUNT' && !viewMode" class="action-button action-button--secondary" :disabled="isInitializing || isSaving" @click="triggerImport">{{ $t('action.import') }}</el-button>
          <el-button v-if="currentId" class="action-button action-button--primary" type="primary" :disabled="isInitializing" @click="openPrintPage({ id: currentId } as StockCount)">
            {{ $t('action.print') }}
          </el-button>
          <el-button v-if="!viewMode" class="action-button action-button--save" :disabled="isInitializing || isSaving" @click="saveData">{{ $t('action.save') }}</el-button>
          <el-button
            v-if="shouldShowApproveButton"
            type="success"
            plain
            class="action-button action-button--success"
            :disabled="isInitializing || isSaving || !canApproveCurrent"
            @click="handleApproveCurrent"
          >
            {{ $t('action.approve') }}
          </el-button>
        </div>
      </div>

      <div class="page-toolbar-card sale-header-card stock-count-form-page__card">
        <div class="card-section-header">
          <h4>{{ $t('section.saleBasicInfo') }}</h4>
        </div>
        <el-alert
          v-if="countType === 'INIT'"
          :title="$t('message.stockInitOneTimeHint')"
          type="warning"
          :closable="false"
          class="stock-count-hint"
        />
        <el-form :model="formData" label-position="top" class="stock-count-form-page__form">
          <div class="stock-count-form-grid">
            <el-form-item :label="countNoLabel" required>
              <div v-if="viewMode" class="readonly-field readonly-field--strong">{{ formData.countNo || '-' }}</div>
              <el-input v-else v-model="formData.countNo" :placeholder="$t('placeholder.autoGenerated')" disabled />
            </el-form-item>
            <el-form-item :label="$t('field.countAt')">
              <div v-if="viewMode" class="readonly-field">{{ formData.countAt || '-' }}</div>
              <el-date-picker
                v-else
                v-model="formData.countAt"
                type="datetime"
                :placeholder="$t('field.countAt')"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="$t('field.remark')">
              <div v-if="viewMode" class="readonly-field readonly-field--remark">{{ formData.remark || '-' }}</div>
              <el-input v-else v-model="formData.remark" />
            </el-form-item>
            <el-form-item v-if="countType === 'COUNT'" :label="$t('field.adjustmentReason')" required>
              <div v-if="viewMode" class="readonly-field">{{ adjustmentReasonLabel(formData.adjustmentReason) }}</div>
              <el-select
                v-else
                v-model="formData.adjustmentReason"
                :placeholder="$t('placeholder.selectAdjustmentReason')"
                style="width: 100%"
              >
                <el-option
                  v-for="option in adjustmentReasonOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div class="table-card sale-detail-card stock-count-form-page__card">
        <div class="table-body">
          <div class="detail-section">
            <div class="card-section-header detail-header">
              <h4>{{ $t('section.saleDetailInfo') }}</h4>
            </div>
            <div class="detail-table-wrapper">
              <ErpDataTable :data="formData.items" style="width: 100%" border stripe table-key="erp-stock-count-items">
                <ErpDataTableColumn type="index" :label="$t('table.index')" width="64" align="center" />
                <ErpDataTableColumn :label="$t('field.product')" min-width="180" column-key="product">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ resolveProductLabel(row.productId) }}</div>
                    <el-select
                      v-else
                      v-model="row.productId"
                      filterable
                      :placeholder="$t('placeholder.selectProduct')"
                      style="width: 100%"
                      @change="handleProductChange(row)"
                    >
                      <el-option v-for="p in getSelectableProductOptions(row.productId)" :key="p.id" :label="p.name" :value="p.id" />
                    </el-select>
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.warehouseLocation')" min-width="220" column-key="warehouseLocation">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ resolveWarehouseLocation(row.warehouseId, row.locationId) }}</div>
                    <ProductStockSelect
                      v-else
                      v-model="row.stockKey"
                      :product-id="row.productId"
                      :warehouse-id="row.warehouseId ?? null"
                      :location-id="normalizeLocationId(row.locationId ?? null)"
                      :warehouse-options="warehouseOptions"
                      :location-options="locationOptions"
                      :placeholder="$t('placeholder.selectLocation')"
                      :allow-manual-location-select="true"
                      @selection-change="(payload) => handleStockSelectionChange(row, payload)"
                    />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.systemQty')" min-width="120" column-key="systemQty">
                  <template #default="{ row }">
                    <div class="readonly-cell">{{ row.systemQty || '0' }}</div>
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.countedQty')" min-width="120" column-key="countedQty">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ row.countedQty || '-' }}</div>
                    <DecimalInput v-else v-model="row.countedQty" :scale="4" input-mode="decimal" />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn v-if="countType === 'INIT'" :label="$t('field.initUnitCost')" min-width="120" column-key="initUnitCost">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ row.initUnitCost || '-' }}</div>
                    <DecimalInput v-else v-model="row.initUnitCost" :scale="4" input-mode="decimal" />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn v-if="countType === 'INIT'" :label="$t('field.initTotalAmount')" min-width="140" column-key="initTotalAmount">
                  <template #default="{ row }">
                    <div class="readonly-cell">{{ calcInitTotal(row) }}</div>
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.diffQty')" min-width="120" column-key="diffQty">
                  <template #default="{ row }">
                    <div class="readonly-cell">{{ calcDiff(row) }}</div>
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.remark')" min-width="160" column-key="remark">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ row.remark || '-' }}</div>
                    <el-input v-else v-model="row.remark" />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn v-if="!viewMode" label="" width="80" align="center" column-key="actions">
                  <template #default="{ $index }">
                    <el-button type="danger" circle size="small" @click="removeItem($index)">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </el-button>
                  </template>
                </ErpDataTableColumn>
              </ErpDataTable>
            </div>
            <div v-if="!viewMode" class="detail-actions">
              <el-button type="primary" @click="addItem">
                + {{ $t('action.addItem') }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <PrintPreviewDialog
      v-model="printDialogVisible"
      :doc-type="printDocType"
      :doc-id="printDocId"
      :title="printTitle"
    />
    <el-dialog
      v-model="successDialogVisible"
      append-to-body
      width="460px"
      class="doc-success-dialog"
      :title="successDialogTitle"
      @closed="handleSuccessDialogClosed"
    >
      <div class="doc-success-dialog__body">
        <p>{{ successDialogMessage }}</p>
        <p class="doc-success-dialog__no">{{ countNoLabel }}：{{ successDocNo || '-' }}</p>
      </div>
      <template #footer>
        <el-button @click="handleContinueAddFromSuccess">{{ $t('action.continueCreate') }}</el-button>
        <el-button @click="handleStayFromSuccess">{{ $t('action.stayCurrent') }}</el-button>
        <el-button @click="handleReturnFromSuccess">{{ $t('action.backToList') }}</el-button>
        <el-button v-if="successDialogMode === 'save' && canApproveSavedDoc" type="success" plain @click="handleApproveSavedDoc">
          {{ $t('action.approve') }}
        </el-button>
        <el-button v-if="successDialogMode === 'approve'" type="primary" @click="handlePrintFromSuccess">
          {{ $t('action.print') }}
        </el-button>
      </template>
    </el-dialog>
    <input ref="importInputRef" type="file" :accept="countType === 'INIT' ? '.xls,.xlsx' : '.csv,text/csv'" class="stock-count-import-input" @change="handleImportFile" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { getCachedLocationOptions, getCachedProductOptions, getCachedWarehouseOptions } from '@/composables/erpBaseDataCache';
import { useAuthStore } from '@/stores/auth';
import DecimalInput from '@/components/DecimalInput.vue';
import ProductStockSelect from '@/components/ProductStockSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';
import { mergeOptionById } from '@/utils/erpMasterData';
import { exportToCsv } from '@/utils/csv';

interface OptionItem {
  id: number;
  name: string;
  code?: string;
  warehouseId?: number;
  enabled?: boolean;
}

interface StockCountItem {
  productId?: number;
  warehouseId?: number;
  locationId?: number | null;
  stockKey?: string;
  systemQty?: string;
  countedQty?: string;
  initUnitCost?: string;
  initTotalAmount?: string;
  remark?: string;
}

interface StockCount {
  id: number;
  countNo: string;
  status: string;
  adjustmentReason?: string;
  countAt?: string;
  remark?: string;
  createdAt?: string;
}

interface StockInitImportResult {
  countId: number;
  countNo: string;
  totalCount: number;
  warningCount: number;
  warnings: string[];
}

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();
const stockCountColumns = ['countNo', 'status', 'countAt', 'adjustmentReason', 'remark', 'createdAt'];
const stockInitColumns = ['countNo', 'status', 'countAt', 'remark', 'createdAt'];
const stockCountColumnSettings = useColumnSettings('erp-stock-count', stockCountColumns);
const stockInitColumnSettings = useColumnSettings('erp-stock-init', stockInitColumns);

const countType = computed(() => (route.meta.countType as string) || 'COUNT');
const apiPrefix = computed(() => (countType.value === 'INIT' ? '/erp/stock-inits' : '/erp/stock-counts'));
const permPrefix = computed(() => (countType.value === 'INIT' ? 'erp-stock-init' : 'erp-stock-count'));
const pageTitle = computed(() => (countType.value === 'INIT' ? t('page.erpStockInitManagement') : t('page.erpStockCountManagement')));

const permAdd = computed(() => `${permPrefix.value}:add`);
const permView = computed(() => `${permPrefix.value}:view`);
const permEdit = computed(() => `${permPrefix.value}:edit`);
const permApprove = computed(() => `${permPrefix.value}:approve`);
const permRedFlush = computed(() => (countType.value === 'INIT' ? 'erp-stock-init:redflush' : ''));
const permCancel = computed(() => `${permPrefix.value}:cancel`);
const countNoLabel = computed(() => (countType.value === 'INIT' ? t('field.stockInitNo') : t('field.stockCountNo')));
const printDocType = computed(() => (countType.value === 'INIT' ? 'STOCK_INIT' : 'STOCK_COUNT'));
const printTitle = computed(() => (countType.value === 'INIT' ? t('page.erpStockInitPrint') : t('page.erpStockCountPrint')));
const isFormPage = computed(() => route.meta.pageMode === 'form');
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');
const formMode = computed(() => String(route.meta.formMode || 'create'));
const formPageTitle = computed(() => {
  if (formMode.value === 'edit') return t('action.edit');
  if (formMode.value === 'view') return t('action.view');
  return t('action.add');
});
const canShow = (key: string) => {
  return countType.value === 'INIT'
    ? stockInitColumnSettings.isVisible(key)
    : stockCountColumnSettings.isVisible(key);
};
const fetchListColumnKeys = () => {
  if (countType.value === 'INIT') {
    stockInitColumnSettings.fetchTenantKeys();
    return;
  }
  stockCountColumnSettings.fetchTenantKeys();
};

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<StockCount[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const successDialogVisible = ref(false);
const successDialogMode = ref<'save' | 'approve'>('save');
const successDocId = ref<number | null>(null);
const successDocNo = ref('');
const pendingPrintAfterSuccess = ref(false);

const searchQuery = ref('');
const statusFilter = ref<'all' | 'DRAFT' | 'APPROVED' | 'CANCELLED' | 'RED_FLUSHED'>('all');

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);
const rowBalanceError = ref<Record<number, boolean>>({});
const importInputRef = ref<HTMLInputElement | null>(null);

const isEditing = ref(false);
const viewMode = ref(false);
const currentId = ref<number | null>(null);
const isInitializing = ref(false);
const isSaving = ref(false);
const initializedFormPath = ref('');

const formData = reactive({
  countNo: '',
  adjustmentReason: '',
  countAt: '',
  remark: '',
  items: [] as StockCountItem[]
});

const adjustmentReasonOptions = computed(() => [
  { value: 'PROFIT', label: t('adjustmentReason.PROFIT') },
  { value: 'LOSS', label: t('adjustmentReason.LOSS') },
  { value: 'CORRECTION', label: t('adjustmentReason.CORRECTION') },
  { value: 'MIGRATION', label: t('adjustmentReason.MIGRATION') },
  { value: 'OTHER', label: t('adjustmentReason.OTHER') }
]);
const canApproveCurrent = computed(() => (
  !viewMode.value
  && authStore.hasPermission(permApprove.value)
  && (formMode.value === 'create' || isEditing.value)
));
const shouldShowApproveButton = computed(() => (
  !viewMode.value
  && authStore.hasPermission(permApprove.value)
));
const canApproveSavedDoc = computed(() => Boolean(successDocId.value) && authStore.hasPermission(permApprove.value));
const successDialogTitle = computed(() => (
  successDialogMode.value === 'approve' ? t('message.approveSuccess') : t('message.saveSuccess')
));
const successDialogMessage = computed(() => (
  successDialogMode.value === 'approve' ? t('message.approveSuccessNextStep') : t('message.saveSuccessNextStep')
));

const getReturnPath = () => {
  const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo.trim() : '';
  return returnTo || (countType.value === 'INIT' ? '/erp/stock-inits' : '/erp/stock-counts');
};

const resolveProductLabel = (productId?: number | null) => {
  if (!productId) return '-';
  return productOptions.value.find(item => item.id === productId)?.name || `#${productId}`;
};

const resolveWarehouseLocation = (warehouseId?: number | null, locationId?: number | null) => {
  if (!warehouseId) return '-';
  const warehouseName = warehouseOptions.value.find(item => item.id === warehouseId)?.name || '-';
  const normalizedLocationId = normalizeLocationId(locationId);
  const locationName = normalizedLocationId == null
    ? t('field.unassignedLocation')
    : (locationOptions.value.find(item => item.id === normalizedLocationId)?.name || t('field.unassignedLocation'));
  return `${warehouseName} / ${locationName}`;
};

const statusLabel = (status?: string) => {
  if (status === 'APPROVED') return t('status.approved');
  if (status === 'CANCELLED') return t('status.cancelled');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return t('status.draft');
};

const adjustmentReasonLabel = (reason?: string) => {
  if (!reason) return '-';
  const key = `adjustmentReason.${reason}`;
  const translated = t(key);
  return translated === key ? reason : translated;
};

const rowClassName = ({ row }: { row: StockCount }) => {
  return row.status === 'RED_FLUSHED' ? 'row-red-flushed' : '';
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
};

const fetchOptions = async () => {
  try {
    const [products, warehouses, locations] = await Promise.all([
      getCachedProductOptions(tenantCacheKey.value),
      getCachedWarehouseOptions(tenantCacheKey.value),
      getCachedLocationOptions(tenantCacheKey.value)
    ]);
    productOptions.value = products;
    warehouseOptions.value = warehouses;
    locationOptions.value = locations;
  } catch (error) {
    notifyError(error);
  }
};

const ensureProductOption = async (productId?: number | null) => {
  if (!productId || productOptions.value.some(item => item.id === productId)) return;
  try {
    const res: any = await request.get(`/erp/products/${productId}`);
    const product = res.data.data;
    if (product) {
      productOptions.value = mergeOptionById(productOptions.value, {
        id: product.id,
        name: product.name,
        enabled: product.enabled
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const getSelectableProductOptions = (currentProductId?: number | null) =>
  productOptions.value.filter(item => item.enabled !== false || item.id === currentProductId);

const ensureWarehouseOption = async (warehouseId?: number | null) => {
  if (!warehouseId || warehouseOptions.value.some(item => item.id === warehouseId)) return;
  try {
    const res: any = await request.get(`/erp/warehouses/${warehouseId}`);
    const warehouse = res.data.data;
    if (warehouse) {
      warehouseOptions.value = mergeOptionById(warehouseOptions.value, {
        id: warehouse.id,
        name: warehouse.name
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const ensureLocationOption = async (locationId?: number | null) => {
  if (locationId == null || locationId === -1 || locationOptions.value.some(item => item.id === locationId)) return;
  try {
    const res: any = await request.get(`/erp/locations/${locationId}`);
    const location = res.data.data;
    if (location) {
      locationOptions.value = mergeOptionById(locationOptions.value, {
        id: location.id,
        name: location.name,
        warehouseId: location.warehouseId
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') params.status = statusFilter.value;

    const res: any = await request.get(`${apiPrefix.value}/page`, { params });
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

const resetForm = () => {
  formData.countNo = '';
  formData.adjustmentReason = '';
  formData.countAt = '';
  formData.remark = '';
  formData.items = [];
  isEditing.value = false;
  viewMode.value = false;
  currentId.value = null;
  rowBalanceError.value = {};
  initializedFormPath.value = '';
};

const addItem = () => {
  formData.items.push({
    productId: undefined,
    warehouseId: undefined,
    locationId: countType.value === 'COUNT' ? -1 : null,
    stockKey: '',
    systemQty: '0',
    countedQty: '',
    initUnitCost: '',
    initTotalAmount: ''
  });
};

const removeItem = (index: number) => {
  formData.items.splice(index, 1);
};

const normalizeLocationId = (value?: number | null) => {
  if (value === -1) return null;
  return value == null ? null : value;
};

const buildStockKey = (warehouseId?: number | null, locationId?: number | null) => {
  const normalizedWarehouseId = warehouseId == null ? null : warehouseId;
  const normalizedLocationId = normalizeLocationId(locationId ?? null);
  if (normalizedWarehouseId == null && normalizedLocationId == null) {
    return '';
  }
  return `${normalizedWarehouseId ?? 0}:${normalizedLocationId ?? 0}`;
};

const syncRowStockKey = (row: StockCountItem) => {
  row.stockKey = buildStockKey(row.warehouseId, row.locationId ?? null);
};

const fetchBalanceForRow = async (row: StockCountItem) => {
  const rowIndex = formData.items.indexOf(row);
  if (!row.productId) {
    row.systemQty = '0';
    if (rowIndex >= 0) rowBalanceError.value[rowIndex] = false;
    return;
  }
  try {
    const params: Record<string, any> = { productId: row.productId };
    if (row.warehouseId) params.warehouseId = row.warehouseId;
    if (row.locationId === -1) {
      params.locationId = -1;
    } else {
      const locationId = normalizeLocationId(row.locationId ?? null);
      if (locationId != null) params.locationId = locationId;
    }
    const res: any = await request.get('/erp/stock/balances/qty', { params });
    row.systemQty = res.data.data != null ? String(res.data.data) : '0';
    if (rowIndex >= 0) rowBalanceError.value[rowIndex] = false;
  } catch (error) {
    row.systemQty = '';
    if (rowIndex >= 0) rowBalanceError.value[rowIndex] = true;
    notifyError(error, 'message.stockBalanceLoadFailed');
  }
};

const handleRowChange = (row: StockCountItem) => {
  if (countType.value === 'COUNT' && row.warehouseId && row.locationId && row.locationId !== -1) {
    const matchedLocation = locationOptions.value.find((option) => option.id === row.locationId);
    if (matchedLocation && matchedLocation.warehouseId !== row.warehouseId) {
      row.locationId = -1;
    }
  }
  if (countType.value === 'COUNT' && row.locationId == null) {
    row.locationId = -1;
  }
  syncRowStockKey(row);
  fetchBalanceForRow(row);
};

const handleProductChange = (row: StockCountItem) => {
  row.warehouseId = undefined;
  row.locationId = countType.value === 'COUNT' ? -1 : null;
  row.stockKey = '';
  fetchBalanceForRow(row);
};

const handleStockSelectionChange = (
  row: StockCountItem,
  payload: { stockKey: string; warehouseId: number | null; locationId: number | null }
) => {
  row.stockKey = payload.stockKey;
  row.warehouseId = payload.warehouseId ?? undefined;
  row.locationId = countType.value === 'COUNT'
    ? (payload.locationId ?? -1)
    : payload.locationId;
  handleRowChange(row);
};

const calcDiff = (row: StockCountItem) => {
  const system = Number(row.systemQty || 0);
  const counted = Number(row.countedQty || 0);
  const diff = counted - system;
  return Number.isFinite(diff) ? diff.toFixed(4).replace(/\.?0+$/, '') : '0';
};

const calcInitTotal = (row: StockCountItem) => {
  const counted = Number(row.countedQty || 0);
  const unitCost = Number(row.initUnitCost || 0);
  const totalAmount = counted * unitCost;
  return Number.isFinite(totalAmount) ? totalAmount.toFixed(4).replace(/\.?0+$/, '') : '0';
};

const handleBack = () => {
  const target = getReturnPath();
  initializedFormPath.value = '';
  router.push(target);
};

const openAddModal = async () => {
  await router.push({
    path: countType.value === 'INIT' ? '/erp/stock-inits/create' : '/erp/stock-counts/create',
    query: { returnTo: route.fullPath }
  });
};

const initializeFormPage = async () => {
  if (initializedFormPath.value === route.fullPath) return;
  isInitializing.value = true;
  try {
    resetForm();
    await fetchOptions();
    if (formMode.value === 'create') {
      addItem();
      await fetchNextCountNo();
      if (route.query.autoImport === '1') {
        setTimeout(() => importInputRef.value?.click(), 0);
      }
      initializedFormPath.value = route.fullPath;
      return;
    }
    const id = Number(route.params.id);
    if (Number.isFinite(id) && id > 0) {
      isEditing.value = formMode.value === 'edit';
      viewMode.value = formMode.value === 'view';
      currentId.value = id;
      await loadDetail(id);
      initializedFormPath.value = route.fullPath;
    }
  } finally {
    isInitializing.value = false;
  }
};

const loadDetail = async (id: number) => {
  const res: any = await request.get(`${apiPrefix.value}/${id}`);
  if (res.data.code === 200) {
    const detail = res.data.data;
    formData.countNo = detail.count.countNo;
    formData.adjustmentReason = detail.count.adjustmentReason || '';
    formData.countAt = detail.count.countAt || '';
    formData.remark = detail.count.remark || '';
    formData.items = (detail.items || []).map((item: any) => ({
      productId: item.productId,
      warehouseId: item.warehouseId,
      locationId: countType.value === 'COUNT' ? (item.locationId ?? -1) : item.locationId,
      stockKey: buildStockKey(item.warehouseId, countType.value === 'COUNT' ? (item.locationId ?? -1) : item.locationId),
      systemQty: String(item.systemQty ?? 0),
      countedQty: String(item.countedQty ?? ''),
      initUnitCost: item.initUnitCost != null ? String(item.initUnitCost) : '',
      initTotalAmount: item.initTotalAmount != null ? String(item.initTotalAmount) : '',
      remark: item.remark || ''
    }));
    await Promise.all(formData.items.flatMap(item => [
      ensureProductOption(item.productId),
      ensureWarehouseOption(item.warehouseId),
      ensureLocationOption(item.locationId)
    ]));
  }
};

const openEditModal = async (row: StockCount) => {
  await router.push({
    path: countType.value === 'INIT' ? `/erp/stock-inits/${row.id}/edit` : `/erp/stock-counts/${row.id}/edit`,
    query: { returnTo: route.fullPath }
  });
};

const openViewModal = async (row: StockCount) => {
  await router.push({
    path: countType.value === 'INIT' ? `/erp/stock-inits/${row.id}` : `/erp/stock-counts/${row.id}`,
    query: { returnTo: route.fullPath }
  });
};

const openPrintPage = (row: StockCount) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const fetchNextCountNo = async () => {
  try {
    const res: any = await request.get(`${apiPrefix.value}/next-count-no`);
    if (res.data.code === 200) {
      formData.countNo = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const validateCountForm = () => {
  if (!formData.countNo || formData.items.length === 0) {
    notifyWarning(t('message.required'));
    return false;
  }
  if (countType.value === 'COUNT' && !formData.adjustmentReason) {
    notifyWarning(t('message.stockAdjustmentReasonRequired'));
    return false;
  }
  for (const item of formData.items) {
    if (!item.productId) {
      notifyWarning(t('message.required'));
      return false;
    }
    if (countType.value === 'COUNT' && !item.warehouseId) {
      notifyWarning(t('message.stockAdjustmentWarehouseRequired'));
      return false;
    }
    if (countType.value === 'COUNT' && (item.locationId === undefined || item.locationId === null)) {
      notifyWarning(t('message.stockAdjustmentLocationRequired'));
      return false;
    }
    const countedQty = item.countedQty == null || item.countedQty === '' ? null : Number(item.countedQty);
    if (countedQty == null || Number.isNaN(countedQty) || countedQty < 0) {
      notifyWarning(t('message.invalidNumber'));
      return false;
    }
    if (countType.value === 'INIT') {
      const initUnitCost = item.initUnitCost == null || item.initUnitCost === '' ? null : Number(item.initUnitCost);
      if (initUnitCost == null || Number.isNaN(initUnitCost) || initUnitCost < 0) {
        notifyWarning(t('message.stockInitUnitCostRequired'));
        return false;
      }
    }
  }
  const duplicateKeys = new Set<string>();
  for (const item of formData.items) {
    const duplicateKey = `${item.productId ?? ''}|${item.warehouseId ?? ''}|${normalizeLocationId(item.locationId ?? null) ?? 'null'}`;
    if (duplicateKeys.has(duplicateKey)) {
      notifyWarning(t('message.duplicateStockCountItem'));
      return false;
    }
    duplicateKeys.add(duplicateKey);
  }
  if (Object.values(rowBalanceError.value).some(Boolean)) {
    notifyWarning(t('message.stockBalanceLoadFailed'));
    return false;
  }
  return true;
};

const buildCountPayload = () => ({
  countNo: formData.countNo,
  adjustmentReason: formData.adjustmentReason || null,
  countAt: formData.countAt,
  remark: formData.remark,
  items: formData.items.map(item => ({
    productId: item.productId,
    warehouseId: item.warehouseId,
    locationId: normalizeLocationId(item.locationId ?? null),
    countedQty: item.countedQty ? Number(item.countedQty) : 0,
    initUnitCost: countType.value === 'INIT' && item.initUnitCost ? Number(item.initUnitCost) : null,
    initTotalAmount: countType.value === 'INIT' ? Number(calcInitTotal(item)) : null,
    systemQty: item.systemQty ? Number(item.systemQty) : 0,
    remark: item.remark || ''
  }))
});

const extractSavedCountId = (response: any) => {
  return Number(
    response?.data?.data?.count?.id
    ?? response?.data?.data?.id
    ?? currentId.value
    ?? 0
  ) || null;
};

const saveCurrentCount = async (options: { silentSuccess?: boolean } = {}) => {
  if (!validateCountForm()) return null;
  if (isSaving.value) return null;
  isSaving.value = true;
  try {
    const payload = buildCountPayload();
    let response: any;
    if (isEditing.value && currentId.value) {
      response = await request.put(`${apiPrefix.value}/${currentId.value}`, payload);
    } else {
      response = await request.post(apiPrefix.value, payload);
    }
    const savedId = extractSavedCountId(response);
    if (savedId) {
      currentId.value = savedId;
      isEditing.value = true;
    }
    if (!options.silentSuccess) notifySuccess();
    return savedId;
  } catch (error) {
    notifyError(error);
    return null;
  } finally {
    isSaving.value = false;
  }
};

const saveData = async () => {
  const savedId = await saveCurrentCount();
  if (!savedId) return;
  if (isFormPage.value) {
    const savedNo = formData.countNo;
    await router.replace({
      path: countType.value === 'INIT' ? `/erp/stock-inits/${savedId}/edit` : `/erp/stock-counts/${savedId}/edit`,
      query: { returnTo: getReturnPath(), from: 'draft' }
    });
    openSuccessDialog(savedId, savedNo, 'save');
  } else {
    fetchList();
  }
};

const handleApproveCurrent = async () => {
  if (!canApproveCurrent.value) return;
  try {
    await ElMessageBox.confirm(
      '确认审核当前单据吗？系统会先保存当前修改并审核，审核后将影响库存。',
      t('action.approve'),
      {
        type: 'warning',
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel')
      }
    );
    const savedId = await saveCurrentCount({ silentSuccess: true });
    if (!savedId) return;
    isSaving.value = true;
    await request.post(`${apiPrefix.value}/${savedId}/approve`);
    notifySuccess(t('message.approveSuccess'));
    await router.replace({
      path: countType.value === 'INIT' ? `/erp/stock-inits/${savedId}` : `/erp/stock-counts/${savedId}`,
      query: {
        returnTo: getReturnPath(),
        from: 'approved',
        status: 'APPROVED'
      }
    });
    openSuccessDialog(savedId, formData.countNo, 'approve');
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    notifyError(error);
  } finally {
    isSaving.value = false;
  }
};

const openSuccessDialog = (id: number | null, docNo: string, mode: 'save' | 'approve') => {
  successDocId.value = id;
  successDocNo.value = docNo;
  successDialogMode.value = mode;
  successDialogVisible.value = true;
};

const closeSuccessDialog = () => {
  successDialogVisible.value = false;
};

const handleContinueAddFromSuccess = async () => {
  closeSuccessDialog();
  initializedFormPath.value = '';
  await router.replace({
    path: countType.value === 'INIT' ? '/erp/stock-inits/create' : '/erp/stock-counts/create',
    query: { returnTo: getReturnPath(), from: 'draft' }
  });
};

const handleStayFromSuccess = () => {
  closeSuccessDialog();
};

const handleReturnFromSuccess = async () => {
  closeSuccessDialog();
  initializedFormPath.value = '';
  await router.push(getReturnPath());
};

const handleApproveSavedDoc = async () => {
  const savedId = successDocId.value;
  if (!savedId) return;
  try {
    closeSuccessDialog();
    isSaving.value = true;
    await request.post(`${apiPrefix.value}/${savedId}/approve`);
    notifySuccess(t('message.approveSuccess'));
    await router.replace({
      path: countType.value === 'INIT' ? `/erp/stock-inits/${savedId}` : `/erp/stock-counts/${savedId}`,
      query: { returnTo: getReturnPath(), from: 'approved', status: 'APPROVED' }
    });
    openSuccessDialog(savedId, successDocNo.value || formData.countNo, 'approve');
  } catch (error) {
    notifyError(error);
  } finally {
    isSaving.value = false;
  }
};

const handlePrintFromSuccess = () => {
  pendingPrintAfterSuccess.value = true;
  closeSuccessDialog();
};

const handleSuccessDialogClosed = () => {
  if (pendingPrintAfterSuccess.value && successDocId.value) {
    printDocId.value = successDocId.value;
    printDialogVisible.value = true;
  }
  pendingPrintAfterSuccess.value = false;
};

const handleApprove = async (row: StockCount) => {
  try {
    if (countType.value === 'COUNT') {
      const res: any = await request.get(`${apiPrefix.value}/${row.id}`);
      const detail = res.data.data;
      const summary = (detail.items || []).reduce((acc: { increase: number; decrease: number; same: number }, item: any) => {
        const diff = Number(item.countedQty || 0) - Number(item.systemQty || 0);
        if (diff > 0) acc.increase += 1;
        else if (diff < 0) acc.decrease += 1;
        else acc.same += 1;
        return acc;
      }, { increase: 0, decrease: 0, same: 0 });
      await ElMessageBox.confirm(
        [
          `${t('message.stockAdjustmentApproveSummary')}`,
          `${t('field.adjustmentReason')}: ${adjustmentReasonLabel(detail.count?.adjustmentReason)}`,
          `${t('field.items')}: ${(detail.items || []).length}`,
          `${t('field.adjustmentIncreaseLines')}: ${summary.increase}`,
          `${t('field.adjustmentDecreaseLines')}: ${summary.decrease}`,
          `${t('field.adjustmentSameLines')}: ${summary.same}`
        ].join('\n'),
        t('action.approve'),
        {
          type: 'warning',
          confirmButtonText: t('action.confirm'),
          cancelButtonText: t('action.cancel')
        }
      );
    }
    await request.post(`${apiPrefix.value}/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleRedFlush = async (row: StockCount) => {
  try {
    const redFlushReasonMessage = t('message.enterRedFlushReason') || '请输入红冲原因';
    const { value } = await ElMessageBox.prompt(
      redFlushReasonMessage,
      t('action.redFlush'),
      {
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel'),
        inputValue: '',
        inputValidator: (input: string) => Boolean(input && input.trim()),
        inputErrorMessage: redFlushReasonMessage
      }
    );
    await request.post(`${apiPrefix.value}/${row.id}/red-flush`, { reason: String(value).trim() });
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleCancel = async (row: StockCount) => {
  try {
    await request.post(`${apiPrefix.value}/${row.id}/cancel`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

bindPageSizeSync(size, fetchList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: () => {
    pageSizeSyncReady.value = true;
    if (pendingInitialLoad.value) {
      pendingInitialLoad.value = false;
      fetchList();
    }
  }
});

onMounted(async () => {
  if (isFormPage.value) {
    await initializeFormPage();
    return;
  }
  fetchListColumnKeys();
  await fetchOptions();
  if (pageSizeSyncReady.value) {
    fetchList();
  } else {
    pendingInitialLoad.value = true;
  }
});

onActivated(async () => {
  if (isFormPage.value) {
    await initializeFormPage();
    return;
  }
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchListColumnKeys();
  fetchList();
});

const exportTemplate = () => {
  exportToCsv(
    'stock-adjustment-template.csv',
    [
      { key: 'productId', label: '商品ID' },
      { key: 'productCode', label: '商品编码' },
      { key: 'productName', label: '商品名称' },
      { key: 'warehouseId', label: '仓库ID' },
      { key: 'warehouseName', label: '仓库名称' },
      { key: 'locationId', label: '库位ID' },
      { key: 'locationName', label: '库位名称' },
      { key: 'countedQty', label: '调整后数量' },
      { key: 'remark', label: '备注' }
    ],
    formData.items.length > 0
      ? formData.items.map((item) => ({
          productId: item.productId ?? '',
          productCode: productOptions.value.find((option) => option.id === item.productId)?.code || '',
          productName: productOptions.value.find((option) => option.id === item.productId)?.name || '',
          warehouseId: item.warehouseId ?? '',
          warehouseName: warehouseOptions.value.find((option) => option.id === item.warehouseId)?.name || '',
          locationId: normalizeLocationId(item.locationId ?? null) ?? '',
          locationName: normalizeLocationId(item.locationId ?? null) == null
            ? t('field.unassignedLocation')
            : locationOptions.value.find((option) => option.id === normalizeLocationId(item.locationId ?? null))?.name || '',
          countedQty: item.countedQty ?? '',
          remark: item.remark || ''
        }))
      : [
          {
            productId: '',
            productCode: '',
            productName: '',
            warehouseId: '',
            warehouseName: '',
            locationId: '',
            locationName: '',
            countedQty: '',
            remark: ''
          }
        ]
  );
};

const triggerImport = async () => {
  if (countType.value === 'INIT') {
    importInputRef.value?.click();
    return;
  }
  if (!isFormPage.value) {
    await router.push({
      path: '/erp/stock-counts/create',
      query: { returnTo: route.fullPath, autoImport: '1' }
    });
    return;
  }
  importInputRef.value?.click();
};

const parseCsvLine = (line: string) => {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i += 1;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === ',' && !inQuotes) {
      result.push(current);
      current = '';
    } else {
      current += char;
    }
  }
  result.push(current);
  return result;
};

const handleImportFile = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    if (countType.value === 'INIT') {
      const formData = new FormData();
      formData.append('file', file);
      const res: any = await request.post('/erp/stock-inits/import', formData);
      const result = res.data.data as StockInitImportResult;
      if (result.warningCount > 0) {
        notifySuccess(`导入完成：生成单据 ${result.countNo}，共 ${result.totalCount} 行，告警 ${result.warningCount} 条`);
      } else {
        notifySuccess(`导入完成：生成单据 ${result.countNo}，共 ${result.totalCount} 行`);
      }
      await fetchList();
      return;
    }
    const content = await file.text();
    const lines = content.split(/\r?\n/).filter((line) => line.trim());
    if (lines.length < 2) {
      notifyWarning(t('message.importNoRows'));
      return;
    }
    const headerLine = lines[0];
    if (!headerLine) {
      notifyWarning(t('message.importNoRows'));
      return;
    }
    const headers = parseCsvLine(headerLine).map((value) => value.trim());
    const rows = lines.slice(1).map((line) => {
      const values = parseCsvLine(line);
      const record: Record<string, string> = {};
      headers.forEach((header, index) => {
        record[header] = (values[index] || '').trim();
      });
      return record;
    });
    const importedItems: StockCountItem[] = rows.map((row, index) => {
      const productId = Number(row['商品ID']);
      const warehouseId = Number(row['仓库ID']);
      const locationRaw = row['库位ID'];
      const locationId = locationRaw === '' ? -1 : Number(locationRaw);
      const countedQty = row['调整后数量'];
      if (!Number.isFinite(productId) || productId <= 0) {
        throw new Error(t('message.importInvalidRow', { row: index + 2, field: '商品ID' }));
      }
      if (!Number.isFinite(warehouseId) || warehouseId <= 0) {
        throw new Error(t('message.importInvalidRow', { row: index + 2, field: '仓库ID' }));
      }
      if (locationRaw !== '' && (!Number.isFinite(locationId) || locationId <= 0)) {
        throw new Error(t('message.importInvalidRow', { row: index + 2, field: '库位ID' }));
      }
      if (countedQty === '' || Number.isNaN(Number(countedQty))) {
        throw new Error(t('message.importInvalidRow', { row: index + 2, field: '调整后数量' }));
      }
      return {
        productId,
        warehouseId,
        locationId,
        stockKey: buildStockKey(warehouseId, locationId),
        systemQty: '0',
        countedQty,
        remark: row['备注'] || ''
      };
    });
    formData.items = importedItems;
    await Promise.all(formData.items.map(async (item) => {
      await Promise.all([
        ensureProductOption(item.productId),
        ensureWarehouseOption(item.warehouseId),
        ensureLocationOption(normalizeLocationId(item.locationId ?? null))
      ]);
      await fetchBalanceForRow(item);
    }));
    notifySuccess(t('message.importSuccess'));
  } catch (error) {
    notifyError(error);
  } finally {
    input.value = '';
  }
};

watch(() => route.fullPath, async () => {
  if (!isFormPage.value) return;
  await initializeFormPage();
});
</script>

<style scoped>
:global(.content-area:has(.sale-page-surface)) {
  background: #ffffff;
}

.sale-page-surface {
  min-height: 100%;
  padding: 16px 20px;
  box-sizing: border-box;
  background: transparent;
}

.sale-page-surface .page-title {
  color: #17233c;
  font-weight: 800;
  font-size: 24px;
  line-height: 32px;
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
  color: #7d889b;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.sale-page-surface .card-section-header h4 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #17233c;
  font-size: 15px;
  font-weight: 700;
}

.sale-page-surface .card-section-header h4::before {
  content: '';
  width: 4px;
  height: 20px;
  border-radius: 999px;
  background: #1677ff;
}

.sale-page-surface .action-button {
  min-width: 78px;
  height: 36px;
  border-radius: 8px;
}

.sale-page-surface .action-button--secondary {
  border-color: #b8d2ff;
  background: #f3f8ff;
  color: #155ec9;
}

.sale-page-surface .action-button--save {
  border-color: #b8d2ff;
  background: #f3f8ff;
  color: #155ec9;
}

.sale-page-surface .action-button--primary.el-button--primary {
  background: #1677ff;
  border-color: #1677ff;
  color: #ffffff;
}

.sale-page-surface .action-button--success.el-button--success.is-plain {
  border-color: #a8e7c2;
  background: #f0fff6;
  color: #15803d;
}

.sale-page-surface .action-button.is-disabled,
.sale-page-surface .action-button.is-disabled:hover {
  opacity: 1;
}

.sale-page-surface .sale-header-card,
.sale-page-surface .sale-detail-card {
  border: 1px solid #e3eaf4;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 16px 36px rgba(28, 45, 76, 0.08), 0 4px 12px rgba(28, 45, 76, 0.04);
}

.sale-page-surface .sale-header-card {
  padding: 20px 22px;
}

.sale-page-surface .sale-detail-card {
  padding: 20px 22px 24px;
}

.sale-page-surface .sale-detail-card .table-body {
  padding: 0;
}

.sale-page-surface .detail-table-wrapper {
  border: 1px solid #e1e9f4;
  border-radius: 10px;
  background: #fbfdff;
  overflow: hidden;
}

.sale-page-surface .readonly-field,
.sale-page-surface .readonly-cell {
  color: #17233c;
}

.stock-doc-view-mode .stock-count-form-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px 38px;
}

.stock-doc-view-mode :deep(.el-form-item) {
  margin-bottom: 16px;
}

.stock-doc-view-mode :deep(.el-form-item__label) {
  margin-bottom: 14px;
  padding: 0;
  color: #697b96;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
}

.stock-doc-view-mode .readonly-field,
.stock-doc-view-mode .readonly-cell {
  min-height: 28px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #25324a;
  font-size: 15px;
  line-height: 1.55;
}

.stock-doc-view-mode .readonly-field--strong {
  color: #152238;
  font-weight: 800;
}

.stock-doc-view-mode .readonly-field--remark {
  min-height: 28px;
  padding-top: 2px;
  white-space: pre-wrap;
}

.stock-doc-view-mode .sale-header-card {
  padding: 24px 28px 26px;
}

.stock-doc-view-mode .sale-detail-card {
  padding: 20px 22px 24px;
}

:deep(.doc-success-dialog .el-dialog__body) {
  padding-top: 6px;
}

.doc-success-dialog__body {
  color: #2f3a4f;
  line-height: 1.8;
}

.doc-success-dialog__no {
  margin: 8px 0 0;
  color: #667085;
}

.stock-count-import-input {
  display: none;
}

.table-card :deep(.row-red-flushed td) {
  background-color: #fff1f0;
}

.table-card :deep(.row-red-flushed:hover > td) {
  background-color: #ffe7e6;
}

.stock-count-form-page__title-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 18px;
}

.stock-count-form-page__breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
  white-space: nowrap;
}

.stock-count-form-page__separator {
  margin: 0;
}

.stock-count-form-page__card {
  margin-bottom: 16px;
}

.stock-count-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.readonly-field,
.readonly-cell {
  min-height: 40px;
  padding: 9px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  color: #111827;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.readonly-field--strong {
  font-weight: 600;
}

.readonly-field--remark {
  align-items: flex-start;
  white-space: pre-wrap;
}

.detail-section {
  margin-top: 0;
}

.stock-count-hint {
  margin-bottom: 12px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.detail-table-wrapper {
  width: 100%;
}

.detail-actions {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .stock-count-form-page__actions {
    justify-content: flex-start;
  }

  .stock-count-form-grid {
    grid-template-columns: 1fr;
  }

  .detail-actions {
    justify-content: stretch;
  }

  .detail-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
