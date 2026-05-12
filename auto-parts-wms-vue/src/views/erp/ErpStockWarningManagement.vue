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
          <el-table-column v-if="canShow('productName')" prop="productName" :label="$t('field.product')" min-width="180">
            <template #default="{ row }">
              <el-button v-if="canEditProduct" link type="primary" @click="handleAction('editProduct', row)">
                {{ row.productName || '-' }}
              </el-button>
              <span v-else>{{ row.productName || '-' }}</span>
            </template>
          </el-table-column>
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
          <el-table-column :label="$t('table.actions')" width="190" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-product:edit'" @click="handleAction('editProduct', row)">
                {{ $t('action.editProduct') }}
              </el-button>
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

    <el-dialog
      v-model="productDialogVisible"
      :title="$t('action.editProduct')"
      width="460px"
      destroy-on-close
    >
      <el-form label-position="top" class="stock-warning-product-form" v-loading="productDialogLoading">
        <el-form-item :label="$t('field.code')">
          <el-input v-model="productForm.code" disabled />
        </el-form-item>
        <el-form-item :label="$t('field.name')">
          <el-input v-model="productForm.name" disabled />
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import DecimalInput from '@/components/DecimalInput.vue';

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

interface ErpProduct {
  id: number;
  code?: string;
  name?: string;
  minStock?: number | null;
  maxStock?: number | null;
  [key: string]: any;
}

const { t } = useI18n();
const router = useRouter();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();
const authStore = useAuthStore();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<StockWarning[]>([]);
const keyword = ref('');
const productDialogVisible = ref(false);
const productDialogLoading = ref(false);
const productSaving = ref(false);
const editingProduct = ref<ErpProduct | null>(null);
const productForm = ref({
  code: '',
  name: '',
  minStock: '',
  maxStock: ''
});

const defaultColumns = ['productCode', 'productName', 'categoryName', 'unitName', 'totalQty', 'minStock', 'maxStock', 'status', 'defaultWarehouse', 'defaultLocation'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock-warning', defaultColumns);

const canShow = (key: string) => isVisible(key);
const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const canEditProduct = computed(() => hasPermission('erp-product:edit'));

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

const normalizeNumber = (value: string | number | null | undefined) => {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

const openProductEditDialog = async (row: StockWarning) => {
  productDialogVisible.value = true;
  productDialogLoading.value = true;
  editingProduct.value = null;
  productForm.value = {
    code: row.productCode || '',
    name: row.productName || '',
    minStock: row.minStock == null ? '' : String(row.minStock),
    maxStock: row.maxStock == null ? '' : String(row.maxStock)
  };
  try {
    const res: any = await request.get(`/erp/products/${row.productId}`);
    if (res.data.code === 200 && res.data.data) {
      const product = res.data.data as ErpProduct;
      editingProduct.value = product;
      productForm.value = {
        code: product.code || row.productCode || '',
        name: product.name || row.productName || '',
        minStock: product.minStock == null ? '' : String(product.minStock),
        maxStock: product.maxStock == null ? '' : String(product.maxStock)
      };
    }
  } catch (error) {
    notifyError(error);
    productDialogVisible.value = false;
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
    const payload = {
      ...editingProduct.value,
      minStock,
      maxStock
    };
    const res: any = await request.put(`/erp/products/${editingProduct.value.id}`, payload);
    if (res.data.code === 200) {
      notifySuccess();
      productDialogVisible.value = false;
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
    router.push({ path: '/erp/purchase-orders/create', query: { productId: row.productId } });
  } else if (action === 'editProduct') {
    openProductEditDialog(row);
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

.stock-warning-product-form {
  padding-top: 4px;
}
</style>
