<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpStockTxnManagement') }}</h2>
      <div class="table-toolbar">
        <div class="table-filters">
          <el-input
            v-model="bizIdFilter"
            :placeholder="$t('field.bizId')"
            class="table-search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="bizTypeFilter"
            :placeholder="$t('field.bizType')"
            class="table-search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <FuzzyProductSelect
            v-model="productFilter"
            :options="productOptions"
            :placeholder="$t('field.product')"
            class="table-search"
            @change="handleSearch"
          />
        </div>
      </div>
    </div>

    <div class="table-card stock-txn-card">
      <div class="table-body stock-txn-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('txnNo')" prop="txnNo" :label="$t('field.txnNo')" min-width="160" />
          <el-table-column v-if="canShow('bizType')" prop="bizType" :label="$t('field.bizType')" min-width="200">
            <template #default="{ row }">
              {{ formatBizType(row.bizType) }}
            </template>
          </el-table-column>
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
          <el-table-column v-if="canShow('qtyDelta')" prop="qtyDelta" :label="$t('field.qtyDelta')" min-width="120" />
          <el-table-column v-if="canShow('qtyBefore')" prop="qtyBefore" :label="$t('field.qtyBefore')" min-width="120" />
          <el-table-column v-if="canShow('qtyAfter')" prop="qtyAfter" :label="$t('field.qtyAfter')" min-width="120" />
          <el-table-column v-if="canShow('unitCost')" :label="$t('field.unitCost')" min-width="120">
            <template #default="{ row }">
              {{ formatMoney(row.unitCost) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('totalCost')" :label="$t('field.totalCost')" min-width="140">
            <template #default="{ row }">
              {{ formatMoney(row.totalCost) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
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
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';

interface OptionItem {
  id: number;
  name: string;
  code?: string;
  warehouseId?: number;
}

interface StockTxn {
  id: number;
  txnNo?: string;
  bizType?: string;
  bizId?: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qtyDelta?: number;
  qtyBefore?: number;
  qtyAfter?: number;
  unitCost?: number;
  totalCost?: number;
  createdAt?: string;
}

const { t } = useI18n();
const { notifyError } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<StockTxn[]>([]);

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const productFilter = ref<number | null>(null);
const bizTypeFilter = ref('');
const bizIdFilter = ref('');

const defaultColumns = ['txnNo', 'bizType', 'product', 'warehouse', 'location', 'qtyDelta', 'qtyBefore', 'qtyAfter', 'unitCost', 'totalCost', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock-txn', defaultColumns);

const canShow = (key: string) => isVisible(key);

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

const formatMoney = (value?: number) => {
  if (value == null || Number.isNaN(value)) return '-';
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4
  });
};

const formatBizType = (value?: string) => {
  if (!value) return '-';
  const key = `bizType.${value}`;
  const translated = t(key);
  return translated === key ? value : translated;
};

const getProductName = (id?: number) => productOptions.value.find(item => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';
const getLocationName = (id?: number) => {
  if (id == null) return t('field.unassignedLocation');
  return locationOptions.value.find(item => item.id === id)?.name || t('field.unassignedLocation');
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
    if (bizTypeFilter.value) params.bizType = bizTypeFilter.value.trim();
    if (bizIdFilter.value) params.bizId = bizIdFilter.value.trim();

    const res: any = await request.get('/erp/stock/txns/page', { params });
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

<style scoped>
.stock-txn-body {
  max-height: 100%;
  overflow: auto;
}
</style>
