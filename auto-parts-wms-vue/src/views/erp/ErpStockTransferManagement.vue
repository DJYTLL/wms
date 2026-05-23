<template>
  <div class="page-shell page-shell--system" :class="{ 'sale-page-surface': isFormPage, 'stock-doc-view-mode': isFormPage && viewMode }">
    <template v-if="!isFormPage">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpStockTransferManagement') }}</div>
      <div class="erp-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="erp-toolbar__search erp-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="erp-toolbar__search" @change="handleSearch">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.draft')" value="DRAFT" />
              <el-option :label="$t('status.approved')" value="APPROVED" />
              <el-option :label="$t('status.cancelled')" value="CANCELLED" />
            </el-select>
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              :start-placeholder="$t('field.startTime')"
              :end-placeholder="$t('field.endTime')"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="erp-toolbar__date-range table-date-range table-date-range--compact"
              @change="handleDateRangeChange"
            />
          </div>
          <div class="table-actions">
            <el-button type="primary" v-permission="'erp-stock-transfer:add'" @click="openAddModal">
              {{ $t('action.add') }}
            </el-button>
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
         table-key="erp-stock-transfer-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('transferNo')" prop="transferNo" :label="$t('field.transferNo')" min-width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="openViewModal(row)">{{ row.transferNo }}</el-button>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('transferAt')" prop="transferAt" :label="$t('field.transferAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.transferAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('remark')" prop="remark" :label="$t('field.remark')" min-width="220" show-overflow-tooltip />
          <ErpDataTableColumn v-if="canShow('printCount')" prop="printCount" :label="$t('field.printCount')" width="100" />
          <ErpDataTableColumn v-if="canShow('lastPrintedAt')" prop="lastPrintedAt" :label="$t('field.lastPrintedAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.lastPrintedAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="300" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'erp-stock-transfer:edit'"
                :disabled="row.status !== 'DRAFT'"
                @click="openEditModal(row)"
              >
                {{ $t('action.edit') }}
              </el-button>
              <el-button
                link
                type="success"
                size="small"
                v-permission="'erp-stock-transfer:approve'"
                :disabled="row.status !== 'DRAFT'"
                @click="handleApprove(row)"
              >
                {{ $t('action.approve') }}
              </el-button>
              <el-button link type="primary" size="small" @click="openViewModal(row)">
                {{ $t('action.view') }}
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'erp-stock-transfer:view'"
                @click="openPrintDialog(row.id)"
              >
                {{ $t('action.print') }}
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                v-permission="'erp-stock-transfer:cancel'"
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
      <div class="page-header sale-page-header stock-transfer-form-page__header">
        <div class="sale-title-group stock-transfer-form-page__title-group">
          <div class="page-title">{{ formPageTitle }}</div>
          <div class="sale-breadcrumb stock-transfer-form-page__breadcrumb">
            <span>{{ $t('page.erpStockTransferManagement') }}</span>
            <span class="sale-breadcrumb__separator stock-transfer-form-page__separator">/</span>
            <span>{{ formPageTitle }}</span>
          </div>
        </div>
        <div class="table-actions sale-page-toolbar__actions stock-transfer-form-page__actions">
          <el-button class="action-button" :disabled="isSaving" @click="handleBack">{{ $t('action.back') }}</el-button>
          <el-button
            v-if="currentId"
            class="action-button action-button--primary"
            type="primary"
            :disabled="isInitializing"
            @click="openPrintDialog(currentId)"
          >
            {{ $t('action.print') }}
          </el-button>
          <el-button
            v-if="!viewMode"
            class="action-button action-button--save"
            :disabled="isInitializing || isSaving"
            @click="saveData"
          >
            {{ $t('action.save') }}
          </el-button>
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

      <div class="page-toolbar-card sale-header-card stock-transfer-form-page__card">
        <div class="card-section-header">
          <h4>{{ $t('section.saleBasicInfo') }}</h4>
        </div>
        <el-form :model="formData" label-position="top" class="stock-transfer-form-page__form">
          <div class="stock-transfer-grid stock-transfer-grid--page">
            <el-form-item :label="$t('field.transferNo')">
              <div v-if="viewMode" class="readonly-field readonly-field--strong">{{ formData.transferNo || '-' }}</div>
              <el-input v-else v-model="formData.transferNo" :placeholder="$t('placeholder.autoGenerated')" disabled />
            </el-form-item>
            <el-form-item :label="$t('field.transferAt')">
              <div v-if="viewMode" class="readonly-field">{{ formData.transferAt || '-' }}</div>
              <el-date-picker
                v-else
                v-model="formData.transferAt"
                type="datetime"
                :placeholder="$t('field.transferAt')"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <el-form-item :label="$t('field.remark')">
            <div v-if="viewMode" class="readonly-field readonly-field--remark">{{ formData.remark || '-' }}</div>
            <el-input
              v-else
              v-model="formData.remark"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="table-card sale-detail-card stock-transfer-form-page__card">
        <div class="table-body">
          <div class="detail-section">
            <div class="card-section-header detail-header">
              <h4>{{ $t('section.saleDetailInfo') }}</h4>
            </div>
            <div class="detail-table-wrapper">
              <ErpDataTable :data="formData.items" style="width: 100%" border stripe table-key="erp-stock-transfer-items">
                <ErpDataTableColumn type="index" :label="$t('table.index')" width="64" align="center" />
                <ErpDataTableColumn :label="$t('field.product')" min-width="180" column-key="product">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ resolveProductLabel(row.productId) }}</div>
                    <FuzzyProductSelect
                      v-else
                      v-model="row.productId"
                      :options="getSelectableProductOptions(row.productId)"
                      :placeholder="$t('placeholder.selectProduct')"
                      style="width: 100%"
                      @change="() => handleProductChange(row)"
                    />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="sourceWarehouseLocationLabel" min-width="220" column-key="sourceWarehouseLocation">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ resolveWarehouseLocation(row.fromWarehouseId, row.fromLocationId) }}</div>
                    <ProductStockSelect
                      v-else
                      v-model="row.sourceStockKey"
                      :product-id="row.productId"
                      :warehouse-id="row.fromWarehouseId"
                      :location-id="row.fromLocationId"
                      :warehouse-options="warehouseOptions"
                      :location-options="locationOptions"
                      :placeholder="$t('placeholder.selectLocation')"
                      :allow-manual-location-select="true"
                      @selection-change="(payload) => handleSourceSelectionChange(row, payload)"
                    />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.sourceQtyAvailable')" width="130" column-key="sourceQtyAvailable">
                  <template #default="{ row }">
                    {{ row.sourceQtyAvailable }}
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="targetWarehouseLocationLabel" min-width="220" column-key="targetWarehouseLocation">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ resolveWarehouseLocation(row.toWarehouseId, row.toLocationId) }}</div>
                    <ProductStockSelect
                      v-else
                      v-model="row.targetStockKey"
                      :product-id="row.productId"
                      :warehouse-id="row.toWarehouseId"
                      :location-id="row.toLocationId"
                      :warehouse-options="warehouseOptions"
                      :location-options="locationOptions"
                      :placeholder="$t('placeholder.selectLocation')"
                      :allow-manual-location-select="true"
                      @selection-change="(payload) => handleTargetSelectionChange(row, payload)"
                    />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="targetQtyAvailableLabel" width="130" column-key="targetQtyAvailable">
                  <template #default="{ row }">
                    {{ row.targetQtyAvailable }}
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.quantity')" width="140" column-key="quantity">
                  <template #default="{ row }">
                    <div v-if="viewMode" class="readonly-cell">{{ row.qty || '-' }}</div>
                    <DecimalInput v-else v-model="row.qty" :scale="4" input-mode="decimal" />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.remark')" min-width="180" column-key="remark">
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
      doc-type="STOCK_TRANSFER"
      :doc-id="printDocId"
      :title="$t('page.erpStockTransferPrint')"
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
        <p class="doc-success-dialog__no">{{ $t('field.transferNo') }}：{{ successDocNo || '-' }}</p>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue';
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
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import ProductStockSelect from '@/components/ProductStockSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';
import { mergeOptionById } from '@/utils/erpMasterData';

interface OptionItem {
  id: number;
  name: string;
  code?: string;
  warehouseId?: number;
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  enabled?: boolean;
}

interface StockTransferRow {
  productId: number | null;
  sourceStockKey: string;
  targetStockKey: string;
  fromWarehouseId: number | null;
  fromLocationId: number | null;
  toWarehouseId: number | null;
  toLocationId: number | null;
  qty: string;
  sourceQtyAvailable: string;
  targetQtyAvailable: string;
  remark: string;
}

interface StockTransferHeader {
  id: number;
  transferNo: string;
  status: string;
  transferAt?: string;
  remark?: string;
  printCount?: number;
  lastPrintedAt?: string;
}

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();
const defaultColumns = ['transferNo', 'status', 'transferAt', 'remark', 'printCount', 'lastPrintedAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock-transfer', defaultColumns);
const canShow = (key: string) => isVisible(key);
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<StockTransferHeader[]>([]);

const searchQuery = ref('');
const statusFilter = ref<'all' | 'DRAFT' | 'APPROVED' | 'CANCELLED'>('all');
const startAt = ref('');
const endAt = ref('');
const dateRange = ref<[string, string] | []>([]);

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const isEditing = ref(false);
const viewMode = ref(false);
const currentId = ref<number | null>(null);
const isInitializing = ref(false);
const isSaving = ref(false);

const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const initializedFormPath = ref('');
const successDialogVisible = ref(false);
const successDialogMode = ref<'save' | 'approve'>('save');
const successDocId = ref<number | null>(null);
const successDocNo = ref('');
const pendingPrintAfterSuccess = ref(false);

const formData = reactive({
  transferNo: '',
  transferAt: '',
  status: 'APPROVED',
  remark: '',
  items: [] as StockTransferRow[]
});

const isFormPage = computed(() => route.meta.pageMode === 'form');
const formMode = computed(() => String(route.meta.formMode || 'create'));
const formPageTitle = computed(() => {
  if (formMode.value === 'edit') return t('action.edit');
  if (formMode.value === 'view') return t('action.view');
  return t('action.add');
});

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
};

const statusLabel = (status?: string) => {
  if (status === 'DRAFT') return t('status.draft');
  if (status === 'APPROVED') return t('status.approved');
  if (status === 'CANCELLED') return t('status.cancelled');
  return status || t('status.draft');
};

const statusTagType = (status?: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'CANCELLED') return 'danger';
  return 'warning';
};

const createItem = (): StockTransferRow => ({
  productId: null,
  sourceStockKey: '',
  targetStockKey: '',
  fromWarehouseId: null,
  fromLocationId: null,
  toWarehouseId: null,
  toLocationId: null,
  qty: '',
  sourceQtyAvailable: '-',
  targetQtyAvailable: '-',
  remark: ''
});

const getSelectableProductOptions = (currentProductId?: number | null) =>
  productOptions.value.filter(item => item.enabled !== false || item.id === currentProductId);

const sourceWarehouseLocationLabel = computed(() => `${t('field.fromWarehouse')}/${t('field.location')}`);
const targetWarehouseLocationLabel = computed(() => `${t('field.toWarehouse')}/${t('field.location')}`);
const targetQtyAvailableLabel = computed(() => `目标${t('field.qtyAvailable')}`);
const canApproveCurrent = computed(() => (
  !viewMode.value
  && authStore.hasPermission('erp-stock-transfer:approve')
  && (formMode.value === 'create' || formData.status === 'DRAFT')
));
const shouldShowApproveButton = computed(() => (
  !viewMode.value
  && authStore.hasPermission('erp-stock-transfer:approve')
));
const canApproveSavedDoc = computed(() => Boolean(successDocId.value) && authStore.hasPermission('erp-stock-transfer:approve'));
const successDialogTitle = computed(() => (
  successDialogMode.value === 'approve' ? t('message.approveSuccess') : t('message.saveSuccess')
));
const successDialogMessage = computed(() => (
  successDialogMode.value === 'approve' ? t('message.approveSuccessNextStep') : t('message.saveSuccessNextStep')
));

const getReturnPath = () => {
  const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo.trim() : '';
  return returnTo || '/erp/stock-transfers';
};

const resolveProductLabel = (productId?: number | null) => {
  if (!productId) return '-';
  return productOptions.value.find(item => item.id === productId)?.name || `#${productId}`;
};

const resolveWarehouseLocation = (warehouseId?: number | null, locationId?: number | null) => {
  if (!warehouseId) return '-';
  const warehouseName = warehouseOptions.value.find(item => item.id === warehouseId)?.name || '-';
  const locationName = locationId == null
    ? t('field.unassignedLocation')
    : (locationOptions.value.find(item => item.id === locationId)?.name || t('field.unassignedLocation'));
  return `${warehouseName} / ${locationName}`;
};

const buildStockKey = (warehouseId?: number | null, locationId?: number | null) => {
  const warehouseKey = warehouseId == null ? 0 : warehouseId;
  const locationKey = locationId == null ? 0 : locationId;
  return `${warehouseKey}:${locationKey}`;
};

const getCurrentDateTimeString = () => {
  const date = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const queryQtyAvailable = async (productId: number, warehouseId: number, locationId: number | null) => {
  const params: Record<string, any> = { productId, warehouseId };
  params.locationId = locationId == null ? -1 : locationId;
  const res: any = await request.get('/erp/stock/balances/qty', { params });
  return String(res.data.data ?? 0);
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
        code: product.code,
        defaultWarehouseId: product.defaultWarehouseId,
        defaultLocationId: product.defaultLocationId,
        enabled: product.enabled
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

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
  if (!locationId || locationOptions.value.some(item => item.id === locationId)) return;
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
    if (searchQuery.value.trim()) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') params.status = statusFilter.value;
    if (startAt.value) params.startAt = startAt.value;
    if (endAt.value) params.endAt = endAt.value;

    const res: any = await request.get('/erp/stock-transfers/page', { params });
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

const handleDateRangeChange = (value: [string, string] | null) => {
  startAt.value = value?.[0] || '';
  endAt.value = value?.[1] || '';
  handleSearch();
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
  formData.transferNo = '';
  formData.transferAt = '';
  formData.status = 'DRAFT';
  formData.remark = '';
  formData.items = [];
  currentId.value = null;
  isEditing.value = false;
  viewMode.value = false;
  initializedFormPath.value = '';
};

const addItem = () => {
  formData.items.push(createItem());
};

const removeItem = (index: number) => {
  formData.items.splice(index, 1);
};

const handleProductChange = async (row: StockTransferRow) => {
  row.sourceStockKey = '';
  row.fromWarehouseId = null;
  row.fromLocationId = null;
  row.sourceQtyAvailable = '-';
  row.targetStockKey = '';
  row.toWarehouseId = null;
  row.toLocationId = null;
  row.targetQtyAvailable = '-';
};

const handleSourceSelectionChange = async (
  row: StockTransferRow,
  payload: { stockKey: string; warehouseId: number | null; locationId: number | null }
) => {
  row.sourceStockKey = payload.stockKey;
  row.fromWarehouseId = payload.warehouseId;
  row.fromLocationId = payload.locationId;
  row.sourceQtyAvailable = '-';
  if (!row.productId || !payload.warehouseId) {
    return;
  }
  try {
    row.sourceQtyAvailable = await queryQtyAvailable(row.productId, payload.warehouseId, payload.locationId);
  } catch (error) {
    row.sourceQtyAvailable = '-';
    notifyError(error, 'message.stockBalanceLoadFailed');
  }
};

const handleTargetSelectionChange = async (
  row: StockTransferRow,
  payload: { stockKey: string; warehouseId: number | null; locationId: number | null }
) => {
  row.targetStockKey = payload.stockKey;
  row.toWarehouseId = payload.warehouseId;
  row.toLocationId = payload.locationId;
  row.targetQtyAvailable = '-';
  if (!row.productId || !payload.warehouseId) {
    return;
  }
  try {
    row.targetQtyAvailable = await queryQtyAvailable(row.productId, payload.warehouseId, payload.locationId);
  } catch (error) {
    row.targetQtyAvailable = '-';
    notifyError(error, 'message.stockBalanceLoadFailed');
  }
};

const fetchNextTransferNo = async () => {
  try {
    const res: any = await request.get('/erp/stock-transfers/next-transfer-no');
    formData.transferNo = res.data.data || '';
  } catch (error) {
    notifyError(error);
  }
};

const loadDetail = async (id: number, mode: 'edit' | 'view') => {
  resetForm();
  try {
    isEditing.value = mode === 'edit';
    viewMode.value = mode === 'view';
    currentId.value = id;
    const res: any = await request.get(`/erp/stock-transfers/${id}`);
    const detail = res.data.data;
    formData.transferNo = detail.transfer?.transferNo || '';
    formData.transferAt = detail.transfer?.transferAt || '';
    formData.status = detail.transfer?.status || (mode === 'view' ? 'APPROVED' : 'DRAFT');
    formData.remark = detail.transfer?.remark || '';
    formData.items = (detail.items || []).map((item: any) => ({
      productId: item.productId || null,
      sourceStockKey: buildStockKey(item.fromWarehouseId || null, item.fromLocationId || null),
      targetStockKey: buildStockKey(item.toWarehouseId || null, item.toLocationId || null),
      fromWarehouseId: item.fromWarehouseId || null,
      fromLocationId: item.fromLocationId || null,
      toWarehouseId: item.toWarehouseId || null,
      toLocationId: item.toLocationId || null,
      qty: String(item.qty ?? ''),
      sourceQtyAvailable: '-',
      targetQtyAvailable: '-',
      remark: item.remark || ''
    }));
    await hydrateFormItems();
  } catch (error) {
    notifyError(error);
  }
};

const initializeFormPage = async () => {
  if (initializedFormPath.value === route.fullPath) return;
  isInitializing.value = true;
  try {
    resetForm();
    await fetchOptions();
    if (formMode.value === 'create') {
      formData.transferAt = getCurrentDateTimeString();
      addItem();
      await fetchNextTransferNo();
      initializedFormPath.value = route.fullPath;
      return;
    }
    const id = Number(route.params.id);
    if (Number.isFinite(id) && id > 0) {
      await loadDetail(id, formMode.value === 'edit' ? 'edit' : 'view');
      initializedFormPath.value = route.fullPath;
    }
  } finally {
    isInitializing.value = false;
  }
};

const handleBack = () => {
  const target = getReturnPath();
  initializedFormPath.value = '';
  router.push(target);
};

const openAddModal = async () => {
  await router.push({ path: '/erp/stock-transfers/create', query: { returnTo: route.fullPath } });
};

const openEditModal = async (row: StockTransferHeader) => {
  await router.push({ path: `/erp/stock-transfers/${row.id}/edit`, query: { returnTo: route.fullPath } });
};

const openViewModal = async (row: StockTransferHeader) => {
  await router.push({ path: `/erp/stock-transfers/${row.id}`, query: { returnTo: route.fullPath } });
};

const openPrintDialog = (id?: number | null) => {
  if (!id) return;
  printDocId.value = id;
  printDialogVisible.value = true;
};

const validateTransferForm = () => {
  if (!formData.transferNo) {
    notifyWarning(t('message.required'));
    return false;
  }
  if (!formData.items.length) {
    notifyWarning(t('message.noItems'));
    return false;
  }

  const seen = new Set<string>();
  for (const [index, item] of formData.items.entries()) {
    const rowIndex = index + 1;
    if (!item.productId) {
      notifyWarning(t('message.requiredWithFieldRow', { row: rowIndex, field: t('field.product') }));
      return false;
    }
    if (!item.fromWarehouseId) {
      notifyWarning(t('message.requiredWithFieldRow', { row: rowIndex, field: t('field.fromWarehouse') }));
      return false;
    }
    if (!item.toWarehouseId) {
      notifyWarning(t('message.requiredWithFieldRow', { row: rowIndex, field: t('field.toWarehouse') }));
      return false;
    }
    const qty = Number(item.qty);
    if (!Number.isFinite(qty) || qty <= 0) {
      notifyWarning(t('message.mustBePositiveWithFieldRow', { row: rowIndex, field: t('field.quantity') }));
      return false;
    }
    if (item.fromWarehouseId === item.toWarehouseId && (item.fromLocationId || null) === (item.toLocationId || null)) {
      notifyWarning(t('message.stockTransferPathSameRow', { row: rowIndex }));
      return false;
    }
    const duplicateKey = `${item.productId}|${item.fromWarehouseId}|${item.fromLocationId ?? 'null'}|${item.toWarehouseId}|${item.toLocationId ?? 'null'}`;
    if (seen.has(duplicateKey)) {
      notifyWarning(t('message.duplicateStockTransferItem'));
      return false;
    }
    seen.add(duplicateKey);
  }
  return true;
};

const buildTransferPayload = () => ({
  transferNo: formData.transferNo,
  transferAt: formData.transferAt || null,
  remark: formData.remark || '',
  items: formData.items.map(item => ({
    productId: item.productId,
    fromWarehouseId: item.fromWarehouseId,
    fromLocationId: item.fromLocationId,
    toWarehouseId: item.toWarehouseId,
    toLocationId: item.toLocationId,
    qty: Number(item.qty),
    remark: item.remark || ''
  }))
});

const extractSavedTransferId = (response: any) => {
  return Number(
    response?.data?.data?.transfer?.id
    ?? response?.data?.data?.id
    ?? currentId.value
    ?? 0
  ) || null;
};

const saveCurrentTransfer = async (options: { silentSuccess?: boolean } = {}) => {
  if (!validateTransferForm()) return null;
  if (isSaving.value) return null;
  isSaving.value = true;
  try {
    const payload = buildTransferPayload();
    let response: any;
    if (isEditing.value && currentId.value) {
      response = await request.put(`/erp/stock-transfers/${currentId.value}`, payload);
    } else {
      response = await request.post('/erp/stock-transfers', payload);
    }
    const savedId = extractSavedTransferId(response);
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
  const savedId = await saveCurrentTransfer();
  if (!savedId) return;
  if (isFormPage.value) {
    const savedNo = formData.transferNo;
    await router.replace({
      path: `/erp/stock-transfers/${savedId}/edit`,
      query: { returnTo: getReturnPath(), from: 'draft' }
    });
    openSuccessDialog(savedId, savedNo, 'save');
  } else {
    fetchList();
  }
};

const hydrateFormItems = async () => {
  await Promise.all(formData.items.flatMap(item => [
    ensureProductOption(item.productId),
    ensureWarehouseOption(item.fromWarehouseId),
    ensureWarehouseOption(item.toWarehouseId),
    ensureLocationOption(item.fromLocationId),
    ensureLocationOption(item.toLocationId)
  ]));
  await Promise.all(formData.items.map(async (item) => {
    if (item.productId && item.fromWarehouseId) {
      try {
        item.sourceQtyAvailable = await queryQtyAvailable(item.productId, item.fromWarehouseId, item.fromLocationId);
      } catch {
        item.sourceQtyAvailable = '-';
      }
    }
    if (item.productId && item.toWarehouseId) {
      try {
        item.targetQtyAvailable = await queryQtyAvailable(item.productId, item.toWarehouseId, item.toLocationId);
      } catch {
        item.targetQtyAvailable = '-';
      }
    }
  }));
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
    const savedId = await saveCurrentTransfer({ silentSuccess: true });
    if (!savedId) return;
    isSaving.value = true;
    await request.post(`/erp/stock-transfers/${savedId}/approve`);
    notifySuccess(t('message.approveSuccess'));
    await router.replace({
      path: `/erp/stock-transfers/${savedId}`,
      query: {
        returnTo: getReturnPath(),
        from: 'approved',
        status: 'APPROVED'
      }
    });
    openSuccessDialog(savedId, formData.transferNo, 'approve');
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
  await router.replace({ path: '/erp/stock-transfers/create', query: { returnTo: getReturnPath(), from: 'draft' } });
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
    await request.post(`/erp/stock-transfers/${savedId}/approve`);
    notifySuccess(t('message.approveSuccess'));
    await router.replace({
      path: `/erp/stock-transfers/${savedId}`,
      query: { returnTo: getReturnPath(), from: 'approved', status: 'APPROVED' }
    });
    openSuccessDialog(savedId, successDocNo.value || formData.transferNo, 'approve');
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

const handleApprove = async (row: StockTransferHeader) => {
  try {
    await ElMessageBox.confirm(
      `${t('action.approve')} ${row.transferNo} ?`,
      t('action.approve'),
      {
        type: 'warning',
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel')
      }
    );
    await request.post(`/erp/stock-transfers/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    notifyError(error);
  }
};

const handleCancel = async (row: StockTransferHeader) => {
  try {
    await request.post(`/erp/stock-transfers/${row.id}/cancel`);
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
  fetchTenantKeys();
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
  fetchTenantKeys();
  fetchList();
});

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

.stock-doc-view-mode .stock-transfer-grid--page {
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

.erp-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-sizing: border-box;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.table-filters {
  display: grid;
  grid-template-columns: 220px 180px minmax(320px, 380px);
  align-items: center;
  justify-content: start;
  gap: 12px;
  min-width: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
}

.stock-transfer-form-page__title-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 18px;
}

.stock-transfer-form-page__breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
  white-space: nowrap;
}

.stock-transfer-form-page__separator {
  margin: 0;
}

.stock-transfer-form-page__card {
  margin-bottom: 16px;
}

.stock-transfer-grid--page {
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.stock-transfer-form {
  margin-bottom: 12px;
}

.stock-transfer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}

.detail-section {
  margin-top: 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.detail-header h4 {
  margin: 0;
  font-size: 16px;
}

.detail-table-wrapper {
  width: 100%;
}

.detail-actions {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .erp-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: 1fr;
  }

  .table-filters {
    grid-template-columns: 200px 160px minmax(280px, 360px);
  }

  .table-actions {
    justify-content: flex-start;
  }

  .stock-transfer-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  :deep(.erp-toolbar__search--wide) {
    width: 200px;
  }

  :deep(.erp-toolbar__date-range) {
    width: 360px;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 360px;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 360px !important;
    min-width: 360px !important;
  }
}

@media (max-width: 768px) {
  .table-filters {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .stock-transfer-form-page__actions {
    justify-content: flex-start;
  }

  .stock-transfer-grid {
    grid-template-columns: 1fr;
  }

  .detail-actions {
    justify-content: stretch;
  }

  .detail-actions :deep(.el-button) {
    width: 100%;
  }

  :deep(.erp-toolbar__search--wide),
  :deep(.erp-toolbar__date-range) {
    width: 100%;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 100%;
  }
}
</style>
