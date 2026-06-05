<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpStockWarningManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar inventory-toolbar--fixed-actions">
          <div class="table-filters inventory-filters inventory-filters--stock-warning">
            <el-select
              v-model="selectedWarehouseId"
              clearable
              filterable
              :placeholder="$t('field.warehouseScope')"
              @change="handleSearch"
            >
              <el-option
                v-for="option in warehouseOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select
              v-model="selectedStatus"
              clearable
              :placeholder="$t('field.status')"
              @change="handleSearch"
            >
              <el-option
                v-for="option in statusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select
              v-model="selectedPolicySource"
              clearable
              :placeholder="$t('field.stockWarningLevel')"
              @change="handleSearch"
            >
              <el-option
                v-for="option in policySourceOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-input
              v-model="keyword"
              :placeholder="$t('action.search')"
              class="inventory-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="inventory-actions inventory-actions--stock-warning">
            <el-button @click="handleRefresh">{{ $t('action.refresh') }}</el-button>
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button @click="handleReset">{{ $t('action.resetDefault') }}</el-button>
            <el-button @click="openAnomalyDrawer">{{ $t('action.manageAnomalies') }}</el-button>
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
          :row-class-name="warningRowClass"
         table-key="erp-stock-warning-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('productCode')" prop="productCode" :label="$t('field.code')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('productName')" prop="productName" :label="$t('field.product')" min-width="180">
            <template #default="{ row }">
              <el-button v-if="canEditProduct" link type="primary" @click="handleAction('editProduct', row)">
                {{ row.productName || '-' }}
              </el-button>
              <span v-else>{{ row.productName || '-' }}</span>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('categoryName')" prop="categoryName" :label="$t('field.category')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('unitName')" prop="unitName" :label="$t('field.unit')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('totalQty')" prop="totalQty" :label="$t('field.qtyOnHand')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('safetyStock')" prop="safetyStock" :label="$t('field.safetyStock')" min-width="150">
            <template #default="{ row }">
              <div class="stock-warning-cell">
                <div>{{ formatDisplayValue(row.safetyStock) }}</div>
                <div class="stock-warning-cell__hint">{{ $t('field.safetyStockReferenceHint') }}</div>
              </div>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('minStock')" prop="minStock" :label="$t('field.minStock')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('maxStock')" prop="maxStock" :label="$t('field.maxStock')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('stockWarningLevel')" :label="$t('field.stockWarningLevel')" min-width="150">
            <template #default="{ row }">
              {{ stockWarningLevelLabel(row) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('warehouseScope')" :label="$t('field.warehouseScope')" min-width="160">
            <template #default="{ row }">
              {{ warehouseScopeLabel(row) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('location')" prop="locationName" :label="$t('field.location')" min-width="160">
            <template #default="{ row }">
              {{ row.locationName || $t('field.unassignedLocation') }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('anomaly')" :label="$t('field.anomaly')" min-width="220" column-key="anomaly">
            <template #default="{ row }">
              <div class="stock-warning-cell">
                <div>{{ anomalySummaryLabel(row) }}</div>
                <el-button link type="primary" size="small" @click="openAnomalyDrawer()">
                  {{ $t('action.manageAnomalies') }}
                </el-button>
              </div>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="250" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-product:edit'" @click="handleAction('editProduct', row)">
                {{ row.policySource === 'PRODUCT_FALLBACK' ? $t('action.editProductStockPolicy') : $t('action.editWarehouseStockPolicy') }}
              </el-button>
              <el-button link type="primary" size="small" @click="handleAction('replenish', row)">
                {{ $t('action.replenish') }}
              </el-button>
              <el-button link type="primary" size="small" @click="handleAction('transfer', row)">
                {{ $t('action.transfer') }}
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

    <el-dialog
      v-model="productDialogVisible"
      :title="productDialogTitle"
      width="460px"
      @closed="editingWarningRow = null"
      destroy-on-close
    >
      <el-form label-position="top" class="stock-warning-product-form" v-loading="productDialogLoading">
        <el-form-item :label="$t('field.code')">
          <el-input v-model="productForm.code" disabled />
        </el-form-item>
        <el-form-item :label="$t('field.name')">
          <el-input v-model="productForm.name" disabled />
        </el-form-item>
        <div class="stock-warning-product-form__hint">{{ stockWarningPolicyHint(editingWarningRow) }}</div>
        <el-form-item :label="$t('field.warehouseScope')">
          <el-input v-model="productForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item :label="$t('field.minStock')">
          <DecimalInput v-model="productForm.minStock" :scale="4" :placeholder="$t('field.minStock')" />
        </el-form-item>
        <el-form-item :label="$t('field.maxStock')">
          <DecimalInput v-model="productForm.maxStock" :scale="4" :placeholder="$t('field.maxStock')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" :loading="productSaving" :disabled="productDialogLoading || !editingProduct" @click="saveProductStockLimit">
          {{ $t('action.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="anomalyDrawerVisible"
      :title="$t('field.stockWarningAnomalies')"
      size="720px"
      destroy-on-close
    >
      <div class="stock-warning-anomaly-drawer">
        <div class="stock-warning-anomaly-drawer__hint">{{ $t('field.anomalyGovernanceHint') }}</div>
        <ErpDataTable
          :data="anomalyTableData"
          style="width: 100%"
          stripe
          v-loading="anomalyLoading"
          :empty-text="$t('table.empty')"
          table-key="erp-stock-warning-anomalies"
        >
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn prop="productCode" :label="$t('field.code')" min-width="120" />
          <ErpDataTableColumn prop="productName" :label="$t('field.product')" min-width="160" />
          <ErpDataTableColumn prop="warehouseName" :label="$t('field.warehouse')" min-width="140" />
          <ErpDataTableColumn prop="anomalyType" :label="$t('field.anomaly')" min-width="160">
            <template #default="{ row }">
              {{ row.anomalyType || row.anomalyCode || '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn prop="anomalyRemark" :label="$t('field.description')" min-width="220">
            <template #default="{ row }">
              {{ row.anomalyRemark || row.remark || '-' }}
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
        <div class="table-pagination">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="anomalyTotal"
            :current-page="anomalyPage"
            :page-size="anomalySize"
            @current-change="handleAnomalyPageChange"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { getCachedWarehouseOptions } from '@/composables/erpBaseDataCache';
import { useAuthStore } from '@/stores/auth';
import DecimalInput from '@/components/DecimalInput.vue';

interface StockWarning {
  productId: number;
  productCode?: string;
  productName?: string;
  categoryName?: string;
  unitName?: string;
  warehouseId?: number;
  warehouseName?: string;
  locationId?: number;
  locationName?: string;
  totalQty?: number;
  safetyStock?: number;
  minStock?: number;
  maxStock?: number;
  status?: string;
  policySource?: string;
  anomalyCount?: number;
  anomalyFlag?: boolean;
  hasAnomaly?: boolean;
  anomalySummary?: string;
}

interface ErpProduct {
  id: number;
  code?: string;
  name?: string;
  minStock?: number | null;
  maxStock?: number | null;
  stockPolicies?: ProductStockPolicy[];
  [key: string]: any;
}

interface ProductStockPolicy {
  warehouseId?: number | null;
  safetyStock?: number | null;
  minStock?: number | null;
  maxStock?: number | null;
}

interface SelectOption {
  label: string;
  value: string | number;
}

interface StockWarningAnomaly {
  id?: number | string;
  productCode?: string;
  productName?: string;
  warehouseName?: string;
  anomalyType?: string;
  anomalyCode?: string;
  anomalyRemark?: string;
  remark?: string;
}

const { t } = useI18n();
const router = useRouter();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<StockWarning[]>([]);
const warehouseOptions = ref<SelectOption[]>([]);
const keyword = ref('');
const selectedWarehouseId = ref<string | number | undefined>();
const selectedStatus = ref<string | undefined>();
const selectedPolicySource = ref<string | undefined>();
const productDialogVisible = ref(false);
const productDialogLoading = ref(false);
const productSaving = ref(false);
const editingProduct = ref<ErpProduct | null>(null);
const editingWarningRow = ref<StockWarning | null>(null);
const anomalyDrawerVisible = ref(false);
const anomalyLoading = ref(false);
const anomalyPage = ref(1);
const anomalySize = ref(10);
const anomalyTotal = ref(0);
const anomalyTableData = ref<StockWarningAnomaly[]>([]);
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');
const productForm = ref({
  code: '',
  name: '',
  warehouseName: '',
  minStock: '',
  maxStock: ''
});

const defaultColumns = ['productCode', 'productName', 'categoryName', 'unitName', 'totalQty', 'safetyStock', 'minStock', 'maxStock', 'status', 'stockWarningLevel', 'warehouseScope', 'location', 'anomaly'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock-warning', defaultColumns);

const canShow = (key: string) => isVisible(key);
const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const canEditProduct = computed(() => hasPermission('erp-product:edit'));
const statusOptions = computed(() => ([
  { label: t('stockWarningStatus.low'), value: 'LOW' },
  { label: t('stockWarningStatus.high'), value: 'HIGH' },
  { label: t('stockWarningStatus.normal'), value: 'NORMAL' }
]));
const policySourceOptions = computed(() => ([
  { label: t('policySource.warehousePolicy'), value: 'WAREHOUSE_POLICY' },
  { label: t('policySource.productFallback'), value: 'PRODUCT_FALLBACK' }
]));
const PRODUCT_LEVEL_SCOPE_VALUE = 'PRODUCT_LEVEL';
const productDialogTitle = computed(() => (
  editingWarningRow.value?.policySource === 'PRODUCT_FALLBACK'
    ? t('action.editProductStockPolicy')
    : t('action.editWarehouseStockPolicy')
));

const statusLabel = (status?: string) => {
  if (!status) return '-';
  const key = `stockWarningStatus.${String(status).toLowerCase()}`;
  const translated = t(key);
  return translated === key ? status : translated;
};

const policySourceLabel = (policySource?: string) => {
  if (!policySource) return '-';
  const mapping: Record<string, string> = {
    WAREHOUSE_POLICY: 'policySource.warehousePolicy',
    PRODUCT_FALLBACK: 'policySource.productFallback'
  };
  const key = mapping[policySource];
  if (!key) return policySource;
  const translated = t(key);
  return translated === key ? policySource : translated;
};

const stockWarningLevelLabel = (row: StockWarning) => {
  if (row.policySource === 'PRODUCT_FALLBACK') return t('field.productLevel');
  if (row.policySource === 'WAREHOUSE_POLICY') return t('field.warehouseLevel');
  return policySourceLabel(row.policySource);
};

const warehouseScopeLabel = (row: StockWarning) => {
  if (row.policySource === 'PRODUCT_FALLBACK') return t('field.productLevelScope');
  return row.warehouseName || '-';
};

const stockWarningPolicyHint = (row?: StockWarning | null) => {
  if (row?.policySource === 'PRODUCT_FALLBACK') {
    return t('field.stockWarningProductPolicyHint');
  }
  return t('field.stockWarningPolicyHint');
};

const statusTagType = (status?: string) => {
  if (status === 'LOW') return 'danger';
  if (status === 'HIGH') return 'warning';
  return 'info';
};

const formatDisplayValue = (value: string | number | null | undefined) => {
  if (value == null || value === '') return '-';
  return String(value);
};

const anomalySummaryLabel = (row: StockWarning) => {
  if (row.anomalySummary) return row.anomalySummary;
  const anomalyCount = Number(row.anomalyCount || 0);
  if (anomalyCount > 0) return t('message.stockWarningAnomalyCount', { count: anomalyCount });
  if (row.anomalyFlag || row.hasAnomaly) return t('message.stockWarningAnomalyDetected');
  return t('message.stockWarningNoAnomaly');
};

const fetchWarehouseOptions = async () => {
  try {
    const warehouses = await getCachedWarehouseOptions(tenantCacheKey.value);
    warehouseOptions.value = [
      {
        label: t('field.productLevel'),
        value: PRODUCT_LEVEL_SCOPE_VALUE
      },
      ...warehouses.map(item => ({
        label: item.name,
        value: item.id
      }))
    ];
  } catch (error) {
    notifyError(error);
  }
};

const applyScopeFilters = (params: Record<string, any>) => {
  if (selectedWarehouseId.value === PRODUCT_LEVEL_SCOPE_VALUE) {
    params.policySource = 'PRODUCT_FALLBACK';
    return;
  }
  if (selectedWarehouseId.value != null && selectedWarehouseId.value !== '') {
    params.warehouseId = selectedWarehouseId.value;
  }
  if (selectedPolicySource.value) {
    params.policySource = selectedPolicySource.value;
  }
};

const warningRowClass = ({ row }: { row: StockWarning }) => {
  if (row.status === 'LOW') return 'warning-row--low';
  if (row.status === 'HIGH') return 'warning-row--high';
  return '';
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    };
    if (keyword.value) params.keyword = keyword.value.trim();
    if (selectedStatus.value) params.status = selectedStatus.value;
    applyScopeFilters(params);
    const res: any = await request.get('/erp/stock-warnings/page', { params });
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

const handleRefresh = () => {
  fetchList();
};

const handleReset = () => {
  keyword.value = '';
  selectedWarehouseId.value = undefined;
  selectedStatus.value = undefined;
  selectedPolicySource.value = undefined;
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

const normalizeNumber = (value: string | number | null | undefined) => {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

const fetchAnomalyList = async () => {
  if (selectedPolicySource.value === 'WAREHOUSE_POLICY') {
    anomalyTableData.value = [];
    anomalyTotal.value = 0;
    return;
  }
  anomalyLoading.value = true;
  try {
    const params: Record<string, any> = {
      page: anomalyPage.value,
      size: anomalySize.value,
    };
    if (keyword.value) params.keyword = keyword.value.trim();
    if (selectedStatus.value) params.status = selectedStatus.value;
    applyScopeFilters(params);
    if (params.policySource === 'PRODUCT_FALLBACK') params.anomalyType = 'PRODUCT_FALLBACK_ONLY';
    const res: any = await request.get('/erp/stock-warnings/anomalies/page', { params });
    if (res.data.code === 200) {
      anomalyTableData.value = res.data.data.items || [];
      anomalyTotal.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    anomalyLoading.value = false;
  }
};

const openAnomalyDrawer = () => {
  anomalyDrawerVisible.value = true;
  anomalyPage.value = 1;
  fetchAnomalyList();
};

const handleAnomalyPageChange = (newPage: number) => {
  anomalyPage.value = newPage;
  fetchAnomalyList();
};

const openProductEditDialog = async (row: StockWarning) => {
  productDialogVisible.value = true;
  productDialogLoading.value = true;
  editingProduct.value = null;
  editingWarningRow.value = row;
  productForm.value = {
    code: row.productCode || '',
    name: row.productName || '',
    warehouseName: warehouseScopeLabel(row),
    minStock: row.minStock == null ? '' : String(row.minStock),
    maxStock: row.maxStock == null ? '' : String(row.maxStock)
  };
  try {
    const res: any = await request.get(`/erp/products/${row.productId}`);
    if (res.data.code === 200 && res.data.data) {
      const product = res.data.data as ErpProduct;
      const matchedPolicy = (product.stockPolicies || []).find(item => item.warehouseId === row.warehouseId);
      editingProduct.value = product;
      productForm.value = {
        code: product.code || row.productCode || '',
        name: product.name || row.productName || '',
        warehouseName: warehouseScopeLabel(row),
        minStock: matchedPolicy?.minStock == null
          ? (product.minStock == null ? '' : String(product.minStock))
          : String(matchedPolicy.minStock),
        maxStock: matchedPolicy?.maxStock == null
          ? (product.maxStock == null ? '' : String(product.maxStock))
          : String(matchedPolicy.maxStock)
      };
    }
  } catch (error) {
    notifyError(error);
    productDialogVisible.value = false;
    editingWarningRow.value = null;
  } finally {
    productDialogLoading.value = false;
  }
};

const saveProductStockLimit = async () => {
  if (!editingProduct.value) return;
  const minStock = normalizeNumber(productForm.value.minStock);
  const maxStock = normalizeNumber(productForm.value.maxStock);
  if (minStock != null && maxStock != null && minStock > maxStock) {
    notifyWarning(t('message.stockLimitInvalid'));
    return;
  }
  productSaving.value = true;
  try {
    const warehouseId = editingWarningRow.value?.warehouseId ?? null;
    const stockPolicies = [...(editingProduct.value.stockPolicies || [])];
    if (warehouseId != null) {
      const policyIndex = stockPolicies.findIndex(item => item.warehouseId === warehouseId);
      const nextPolicy = policyIndex >= 0
        ? { ...stockPolicies[policyIndex], minStock, maxStock }
        : { warehouseId, safetyStock: null, minStock, maxStock };
      if (policyIndex >= 0) {
        stockPolicies.splice(policyIndex, 1, nextPolicy);
      } else {
        stockPolicies.push(nextPolicy);
      }
    }
    const payload = {
      ...editingProduct.value,
      minStock: warehouseId == null ? minStock : editingProduct.value.minStock ?? null,
      maxStock: warehouseId == null ? maxStock : editingProduct.value.maxStock ?? null,
      stockPolicies
    };
    const res: any = await request.put(`/erp/products/${editingProduct.value.id}`, payload);
    if (res.data.code === 200) {
      notifySuccess();
      productDialogVisible.value = false;
      editingWarningRow.value = null;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  } finally {
    productSaving.value = false;
  }
};

const handleAction = (action: string, row: StockWarning) => {
  if (action === 'replenish') {
    router.push({
      path: '/erp/purchase-orders/create',
      query: {
        productId: row.productId,
        warehouseId: row.warehouseId,
        warningSource: 'stock-warning'
      }
    });
  } else if (action === 'transfer') {
    router.push({
      path: '/erp/stock-transfers/create',
      query: {
        productId: row.productId,
        warehouseId: row.warehouseId,
        warningSource: 'stock-warning'
      }
    });
  } else if (action === 'editProduct') {
    openProductEditDialog(row);
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

onMounted(() => {
  fetchTenantKeys();
  fetchWarehouseOptions();
  if (pageSizeSyncReady.value) {
    fetchList();
  } else {
    pendingInitialLoad.value = true;
  }
});

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchList();
});
</script>

<style scoped>
:deep(.warning-row--low td) {
  background: #fff5f5;
}

:deep(.warning-row--high td) {
  background: #fff8e7;
}

.stock-warning-product-form {
  padding-top: 4px;
}

.stock-warning-product-form__hint {
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.inventory-toolbar--fixed-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.inventory-filters--stock-warning {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.inventory-filters--stock-warning > * {
  flex: 0 0 180px;
  min-width: 180px;
}

.inventory-actions--stock-warning {
  display: flex;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
}

.stock-warning-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-warning-cell__hint,
.stock-warning-anomaly-drawer__hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.stock-warning-anomaly-drawer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 1280px) {
  .inventory-toolbar--fixed-actions {
    grid-template-columns: minmax(0, 1fr) auto;
  }
}
</style>
