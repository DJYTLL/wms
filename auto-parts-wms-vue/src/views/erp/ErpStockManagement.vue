<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpStockManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar">
          <div class="table-filters inventory-filters inventory-filters--stock">
            <el-select v-model="productFilter" :placeholder="$t('field.product')" class="inventory-field--narrow" clearable @change="handleSearch">
              <el-option v-for="item in productOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="warehouseFilter" :placeholder="$t('field.warehouse')" class="inventory-field--narrow" clearable @change="handleWarehouseChange">
              <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="locationFilter" :placeholder="$t('field.location')" class="inventory-field--narrow" clearable @change="handleSearch">
              <el-option :label="$t('field.unassigned')" :value="-1" />
              <el-option v-for="item in getLocationOptions(warehouseFilter || undefined)" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
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
              {{ getProductName(row.productId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('warehouse')" :label="$t('field.warehouse')" min-width="160" column-key="warehouse">
            <template #default="{ row }">
              {{ getWarehouseName(row.warehouseId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('location')" :label="$t('field.location')" min-width="160" column-key="location">
            <template #default="{ row }">
              {{ getLocationName(row.locationId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('qty')" prop="qtyOnHand" :label="$t('field.qtyOnHand')" min-width="120" />
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
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import { getCachedLocations, getCachedProductOptions, getCachedWarehouses } from '@/composables/erpBaseDataCache';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface StockBalance {
  id: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qtyOnHand?: number;
  updatedAt?: string;
}

const { t } = useI18n();
const { notifyError } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const tableData = ref<StockBalance[]>([]);

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const productFilter = ref<number | null>(null);
const warehouseFilter = ref<number | null>(null);
const locationFilter = ref<number | null>(null);

const defaultColumns = ['product', 'warehouse', 'location', 'qty', 'updatedAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock', defaultColumns);
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const canShow = (key: string) => isVisible(key);

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
};

const getProductName = (id?: number) => productOptions.value.find(item => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';
const getLocationName = (id?: number) => {
  if (!id) return t('field.unassigned');
  return locationOptions.value.find(item => item.id === id)?.name || '-';
};

const getLocationOptions = (warehouseId?: number) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const fetchOptions = async () => {
  try {
    const [products, warehouses, locations] = await Promise.all([
      getCachedProductOptions(tenantCacheKey.value),
      getCachedWarehouses(tenantCacheKey.value),
      getCachedLocations(tenantCacheKey.value)
    ]);
    productOptions.value = products;
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
