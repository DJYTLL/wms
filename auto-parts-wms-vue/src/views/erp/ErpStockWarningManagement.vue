<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpStockWarningManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar">
          <div class="table-filters inventory-filters inventory-filters--stock-warning">
            <el-input
              v-model="keyword"
              :placeholder="$t('action.search')"
              class="inventory-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="warningRowClass"
        >
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('productCode')" prop="productCode" :label="$t('field.code')" min-width="140" />
          <el-table-column v-if="canShow('productName')" prop="productName" :label="$t('field.product')" min-width="180" />
          <el-table-column v-if="canShow('categoryName')" prop="categoryName" :label="$t('field.category')" min-width="140" />
          <el-table-column v-if="canShow('unitName')" prop="unitName" :label="$t('field.unit')" min-width="120" />
          <el-table-column v-if="canShow('totalQty')" prop="totalQty" :label="$t('field.qtyOnHand')" min-width="140" />
          <el-table-column v-if="canShow('minStock')" prop="minStock" :label="$t('field.minStock')" min-width="140" />
          <el-table-column v-if="canShow('maxStock')" prop="maxStock" :label="$t('field.maxStock')" min-width="140" />
          <el-table-column v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('defaultWarehouse')" prop="defaultWarehouseName" :label="$t('field.defaultWarehouse')" min-width="160">
            <template #default="{ row }">
              {{ row.defaultWarehouseName || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('defaultLocation')" prop="defaultLocationName" :label="$t('field.defaultLocation')" min-width="160">
            <template #default="{ row }">
              {{ row.defaultLocationName || $t('field.unassignedLocation') }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleAction('replenish', row)">
                {{ $t('action.replenish') }}
              </el-button>
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
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

interface StockWarning {
  productId: number;
  productCode?: string;
  productName?: string;
  categoryName?: string;
  unitName?: string;
  defaultWarehouseId?: number;
  defaultWarehouseName?: string;
  defaultLocationId?: number;
  defaultLocationName?: string;
  totalQty?: number;
  minStock?: number;
  maxStock?: number;
  status?: string;
}

const { t } = useI18n();
const router = useRouter();
const { notifyError } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<StockWarning[]>([]);
const keyword = ref('');

const defaultColumns = ['productCode', 'productName', 'categoryName', 'unitName', 'totalQty', 'minStock', 'maxStock', 'status', 'defaultWarehouse', 'defaultLocation'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock-warning', defaultColumns);

const canShow = (key: string) => isVisible(key);

const statusLabel = (status?: string) => {
  if (!status) return '-';
  const key = `stockWarningStatus.${String(status).toLowerCase()}`;
  const translated = t(key);
  return translated === key ? status : translated;
};

const statusTagType = (status?: string) => {
  if (status === 'LOW') return 'danger';
  if (status === 'HIGH') return 'warning';
  return 'info';
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

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchList();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchList();
};

const handleAction = (action: string, row: StockWarning) => {
  if (action === 'replenish') {
    router.push({ path: '/erp/purchase-orders/create', query: { productId: row.productId } });
  }
};

onMounted(() => {
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
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
</style>
