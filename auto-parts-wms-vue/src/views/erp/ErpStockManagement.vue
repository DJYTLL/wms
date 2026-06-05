<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpStockManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar">
          <div class="table-filters inventory-filters inventory-filters--stock">
            <el-select
              v-model="productFilter"
              filterable
              remote
              clearable
              reserve-keyword
              class="inventory-field--narrow"
              :placeholder="$t('field.product')"
              :remote-method="searchProducts"
              :loading="productSearchLoading"
              @change="handleSearch"
            >
              <el-option
                v-for="item in selectableProductOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <FuzzyProductSelect
              v-model="warehouseFilter"
              :options="warehouseOptions"
              :placeholder="$t('field.warehouse')"
              class="inventory-field--narrow"
              @change="handleWarehouseChange"
            />
            <FuzzyProductSelect
              v-model="locationFilter"
              :options="locationFilterOptions"
              :placeholder="$t('field.location')"
              class="inventory-field--narrow"
              @change="handleSearch"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-stock-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('product')" :label="$t('field.product')" min-width="180" column-key="product">
            <template #default="{ row }">
              {{ row.productName || '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('warehouse')" :label="$t('field.warehouse')" min-width="160" column-key="warehouse">
            <template #default="{ row }">
              {{ row.warehouseName || '-' }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('location')" :label="$t('field.location')" min-width="160" column-key="location">
            <template #default="{ row }">
              {{ row.locationName || getLocationName(row.locationId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('qtyOnHand')" prop="qtyOnHand" :label="$t('field.qtyOnHand')" min-width="120">
            <template #default="{ row }">
              {{ formatNumber(row.qtyOnHand) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('qtyLocked')" :label="$t('field.qtyLocked')" min-width="140">
            <template #default="{ row }">
              <el-popover
                v-if="hasOccupancy(row)"
                trigger="hover"
                placement="top"
                width="420"
                popper-class="stock-occupancy-popover"
                @show="loadOccupancy(row)"
              >
                <template #reference>
                  <span class="stock-occupied-trigger">{{ formatNumber(row.qtyLocked) }}</span>
                </template>
                <div v-loading="occupancyLoadingMap[row.id]">
                  <div v-if="getOccupancyItems(row.id).length === 0" class="stock-occupancy-empty">暂无占用明细</div>
                  <div v-for="item in getOccupancyItems(row.id)" :key="`${item.routeName}-${item.docId}`" class="stock-occupancy-row">
                    <span>{{ formatOccupancyType(item.docType) }}</span>
                    <el-button link type="primary" class="stock-occupancy-doc" @click="openOccupancyDoc(item)">
                      {{ item.docNo }}
                    </el-button>
                    <span class="stock-occupancy-qty">{{ formatNumber(item.qty) }}</span>
                  </div>
                </div>
              </el-popover>
              <span v-else class="stock-occupied-trigger is-empty">{{ formatNumber(row.qtyLocked) }}</span>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('qtyAvailable')" prop="qtyAvailable" :label="$t('field.qtyAvailable')" min-width="120">
            <template #default="{ row }">
              {{ formatNumber(row.qtyAvailable) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('updatedAt')" prop="updatedAt" :label="$t('field.updatedTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import { getCachedLocations, getCachedProductOptions, getCachedWarehouses } from '@/composables/erpBaseDataCache';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface StockBalance {
  id: number;
  productId?: number;
  productName?: string;
  warehouseId?: number;
  warehouseName?: string;
  locationId?: number;
  locationName?: string;
  qtyOnHand?: number;
  qtyLocked?: number;
  qtyAvailable?: number;
  updatedAt?: string;
}

interface StockOccupancyItem {
  docType: string;
  docNo: string;
  docId: number;
  qty: number;
  orderAt?: string;
  routeName: string;
}

const { t } = useI18n();
const router = useRouter();
const { notifyError } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();

const loading = ref(false);
const productSearchLoading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<StockBalance[]>([]);
const productSearchTimer = ref<number | null>(null);

const productOptions = ref<OptionItem[]>([]);
const productSearchOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const productFilter = ref<number | null>(null);
const warehouseFilter = ref<number | null>(null);
const locationFilter = ref<number | null>(null);
const occupancyLoadingMap = ref<Record<number, boolean>>({});
const occupancyMap = ref<Record<number, StockOccupancyItem[]>>({});

const defaultColumns = ['product', 'warehouse', 'location', 'qtyOnHand', 'qtyLocked', 'qtyAvailable', 'updatedAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock', defaultColumns);
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const canShow = (key: string) => isVisible(key);

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
};

const getLocationName = (id?: number) => {
  if (!id) return t('field.unassigned');
  return locationOptions.value.find(item => item.id === id)?.name || '-';
};

const formatNumber = (value?: number) => Number(value || 0).toLocaleString('zh-CN');

const formatOccupancyType = (docType?: string) => {
  if (docType === 'SALE_ORDER') return '销售单';
  if (docType === 'PURCHASE_RETURN') return '采购退货单';
  if (docType === 'ASSEMBLE') return '组装/拆装单';
  return docType || '-';
};

const mergeOptionById = (items: OptionItem[], nextItem: OptionItem) => {
  const nextId = nextItem.id;
  const filtered = items.filter(item => item.id !== nextId);
  return [...filtered, nextItem];
};

const findKnownProduct = (productId?: number | null) => {
  if (!productId) return undefined;
  return productOptions.value.find(item => item.id === productId);
};

const rememberProductOption = (product?: Partial<OptionItem> | null) => {
  if (!product?.id) return;
  productOptions.value = mergeOptionById(productOptions.value, {
    id: product.id,
    name: product.name || String(product.id)
  });
};

const rememberProductOptions = (products: OptionItem[]) => {
  products.forEach(product => rememberProductOption(product));
};

const selectableProductOptions = computed(() => {
  const currentProduct = findKnownProduct(productFilter.value);
  if (currentProduct && !productSearchOptions.value.some(item => item.id === currentProduct.id)) {
    return [currentProduct, ...productSearchOptions.value];
  }
  return productSearchOptions.value;
});

const getLocationOptions = (warehouseId?: number) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const locationFilterOptions = computed(() => {
  const options = getLocationOptions(warehouseFilter.value || undefined);
  return [
    { id: -1, name: t('field.unassigned') },
    ...options
  ];
});

const fetchOptions = async () => {
  try {
    const [warehouses, locations] = await Promise.all([
      getCachedWarehouses(tenantCacheKey.value),
      getCachedLocations(tenantCacheKey.value)
    ]);
    warehouseOptions.value = warehouses;
    locationOptions.value = locations;
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
    if (productFilter.value !== null) params.productId = productFilter.value;
    if (warehouseFilter.value !== null) params.warehouseId = warehouseFilter.value;
    if (locationFilter.value !== null) params.locationId = locationFilter.value;

    const res: any = await request.get('/erp/stock/balances/page', { params });
    if (res.data.code === 200) {
      tableData.value = res.data.data.items || [];
      occupancyMap.value = {};
      occupancyLoadingMap.value = {};
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const loadOccupancy = async (row: StockBalance) => {
  if (!row.id || occupancyMap.value[row.id] || occupancyLoadingMap.value[row.id]) return;
  occupancyLoadingMap.value = {
    ...occupancyLoadingMap.value,
    [row.id]: true
  };
  try {
    const res: any = await request.get(`/erp/stock/balances/${row.id}/occupancy`);
    occupancyMap.value = {
      ...occupancyMap.value,
      [row.id]: res.data?.data || []
    };
  } catch (error) {
    notifyError(error);
  } finally {
    occupancyLoadingMap.value = {
      ...occupancyLoadingMap.value,
      [row.id]: false
    };
  }
};

const getOccupancyItems = (rowId?: number) => {
  if (!rowId) return [];
  return occupancyMap.value[rowId] || [];
};

const hasOccupancy = (row: StockBalance) => Number(row.qtyLocked || 0) > 0;

const openOccupancyDoc = (item: StockOccupancyItem) => {
  const route = router.resolve({
    name: item.routeName,
    params: { id: item.docId }
  });
  window.open(route.href, '_blank', 'noopener,noreferrer');
};

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const searchProductsNow = async (keyword = '') => {
  productSearchLoading.value = true;
  try {
    const res: any = await request.get('/erp/products/page', {
      params: {
        page: 1,
        size: 20,
        keyword: keyword.trim() || undefined,
        enabled: true
      }
    });
    const products = (res.data?.data?.items || []) as OptionItem[];
    rememberProductOptions(products);
    productSearchOptions.value = products;
  } catch (error) {
    notifyError(error);
  } finally {
    productSearchLoading.value = false;
  }
};

const searchProducts = (keyword = '') => {
  const normalizedKeyword = keyword.trim();
  if (!normalizedKeyword) {
    productSearchOptions.value = [];
    if (productSearchTimer.value != null && typeof window !== 'undefined') {
      window.clearTimeout(productSearchTimer.value);
      productSearchTimer.value = null;
    }
    productSearchLoading.value = false;
    return;
  }
  if (productSearchTimer.value != null && typeof window !== 'undefined') {
    window.clearTimeout(productSearchTimer.value);
  }
  if (typeof window === 'undefined') {
    void searchProductsNow(keyword);
    return;
  }
  productSearchTimer.value = window.setTimeout(() => {
    productSearchTimer.value = null;
    void searchProductsNow(keyword);
  }, 250);
};

const handleWarehouseChange = () => {
  locationFilter.value = null;
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
  fetchTenantKeys();
  await fetchOptions();
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
.stock-occupied-trigger {
  color: #1677ff;
  cursor: pointer;
  font-weight: 600;
}

.stock-occupied-trigger.is-empty {
  color: inherit;
  cursor: default;
  font-weight: 400;
}

.stock-occupancy-popover {
  min-width: 340px;
}

.stock-occupancy-empty {
  color: #909399;
}

.stock-occupancy-row {
  display: grid;
  grid-template-columns: 88px 1fr 70px;
  gap: 12px;
  align-items: center;
  padding: 6px 0;
}

.stock-occupancy-doc {
  padding: 0;
  justify-content: flex-start;
  font-weight: 600;
}

.stock-occupancy-qty {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
</style>
