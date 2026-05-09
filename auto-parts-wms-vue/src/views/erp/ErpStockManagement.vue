<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpStockManagement') }}</h2>
      <div class="table-toolbar">
        <div class="table-filters">
          <el-select v-model="productFilter" :placeholder="$t('field.product')" class="table-search" clearable @change="handleSearch">
            <el-option v-for="item in productOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="warehouseFilter" :placeholder="$t('field.warehouse')" class="table-search" clearable @change="handleSearch">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="locationFilter" :placeholder="$t('field.location')" class="table-search" clearable @change="handleSearch">
            <el-option :label="$t('field.unassigned')" :value="-1" />
            <el-option v-for="item in getLocationOptions(warehouseFilter || undefined)" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('product')" :label="$t('field.product')" min-width="180">
            <template #default="{ row }">
              {{ getProductName(row.productId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('warehouse')" :label="$t('field.warehouse')" min-width="160">
            <template #default="{ row }">
              {{ getWarehouseName(row.warehouseId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('location')" :label="$t('field.location')" min-width="160">
            <template #default="{ row }">
              {{ getLocationName(row.locationId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('qty')" prop="qtyOnHand" :label="$t('field.qtyOnHand')" min-width="120" />
          <el-table-column v-if="canShow('qtyLocked')" prop="qtyLocked" :label="$t('field.qtyLocked')" min-width="120" />
          <el-table-column v-if="canShow('qtyAvailable')" prop="qtyAvailable" :label="$t('field.qtyAvailable')" min-width="120" />
          <el-table-column v-if="canShow('updatedAt')" prop="updatedAt" :label="$t('field.updatedTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

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
  qtyLocked?: number;
  qtyAvailable?: number;
  updatedAt?: string;
}

const { t } = useI18n();
const { notifyError } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<StockBalance[]>([]);

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const productFilter = ref<number | null>(null);
const warehouseFilter = ref<number | null>(null);
const locationFilter = ref<number | null>(null);

const defaultColumns = ['product', 'warehouse', 'location', 'qty', 'qtyLocked', 'qtyAvailable', 'updatedAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock', defaultColumns);

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
    const [productsRes, warehousesRes, locationsRes] = await Promise.all([
      request.get('/erp/products'),
      request.get('/erp/warehouses'),
      request.get('/erp/locations')
    ]);
    productOptions.value = productsRes.data.data || [];
    warehouseOptions.value = warehousesRes.data.data || [];
    locationOptions.value = locationsRes.data.data || [];
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
    if (productFilter.value) params.productId = productFilter.value;
    if (warehouseFilter.value) params.warehouseId = warehouseFilter.value;
    if (locationFilter.value) params.locationId = locationFilter.value;

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

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchList();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchList();
};

onMounted(() => {
  fetchOptions();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchOptions();
  fetchList();
});
</script>
