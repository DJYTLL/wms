<template>
  <div class="page-shell page-shell--system">
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
            <el-button type="primary" v-permission="permAdd" @click="openAddModal">{{ $t('action.add') }}</el-button>
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
          :row-class-name="rowClassName"
        >
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column prop="countNo" :label="countNoLabel" min-width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openViewModal(row)">{{ row.countNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'APPROVED' ? 'success' : row.status === 'CANCELLED' ? 'danger' : row.status === 'RED_FLUSHED' ? 'info' : 'warning'"
                size="small"
              >
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="countAt" :label="$t('field.countAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.countAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" :label="$t('field.remark')" min-width="200" />
          <el-table-column prop="createdAt" :label="$t('field.createdAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="280" fixed="right">
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

    <PrintPreviewDialog
      v-model="printDialogVisible"
      :doc-type="printDocType"
      :doc-id="printDocId"
      :title="printTitle"
    />

    <el-dialog v-model="showModal" :title="modalTitle" width="860px" @closed="resetForm">
      <el-form :model="formData" label-width="120px" class="stock-count-form">
        <el-form-item :label="countNoLabel" required>
          <el-input v-model="formData.countNo" :placeholder="$t('placeholder.autoGenerated')" disabled />
        </el-form-item>
        <el-form-item :label="$t('field.countAt')">
          <el-date-picker
            v-model="formData.countAt"
            type="datetime"
            :placeholder="$t('field.countAt')"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            :disabled="viewMode"
          />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="formData.remark" :disabled="viewMode" />
        </el-form-item>
      </el-form>

      <div class="detail-section">
        <div class="detail-header">
          <h4>{{ $t('field.items') }}</h4>
          <el-button type="primary" plain size="small" :disabled="viewMode" @click="addItem">
            + {{ $t('action.addItem') }}
          </el-button>
        </div>
        <div class="detail-table-wrapper">
          <el-table :data="formData.items" style="width: 100%" border stripe>
            <el-table-column :label="$t('field.product')" min-width="180">
              <template #default="{ row }">
                <el-select
                  v-model="row.productId"
                  filterable
                  :placeholder="$t('placeholder.selectProduct')"
                  style="width: 100%"
                  :disabled="viewMode"
                  @change="handleRowChange(row)"
                >
                  <el-option v-for="p in getSelectableProductOptions(row.productId)" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.warehouse')" min-width="150">
              <template #default="{ row }">
                <el-select
                  v-model="row.warehouseId"
                  filterable
                  :placeholder="$t('placeholder.selectWarehouse')"
                  style="width: 100%"
                  :disabled="viewMode"
                  @change="handleRowChange(row)"
                >
                  <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.location')" min-width="150">
              <template #default="{ row }">
                <el-select
                  v-model="row.locationId"
                  filterable
                  :placeholder="$t('placeholder.selectLocation')"
                  style="width: 100%"
                  :disabled="viewMode"
                  @change="handleRowChange(row)"
                >
                  <el-option :label="$t('field.unassigned')" :value="-1" />
                  <el-option v-for="l in getLocationOptions(row.warehouseId)" :key="l.id" :label="l.name" :value="l.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.systemQty')" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.systemQty" disabled />
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.countedQty')" min-width="120">
              <template #default="{ row }">
                <DecimalInput v-model="row.countedQty" :scale="4" input-mode="decimal" :disabled="viewMode" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.diffQty')" min-width="120">
              <template #default="{ row }">
                {{ calcDiff(row) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.remark')" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.remark" :disabled="viewMode" />
              </template>
            </el-table-column>
            <el-table-column label="" width="80" align="center">
              <template #default="{ $index }">
                <el-button type="danger" circle size="small" :disabled="viewMode" @click="removeItem($index)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button v-if="!viewMode" type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import DecimalInput from '@/components/DecimalInput.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';
import { mergeOptionById } from '@/utils/erpMasterData';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
  enabled?: boolean;
}

interface StockCountItem {
  productId?: number;
  warehouseId?: number;
  locationId?: number | null;
  systemQty?: string;
  countedQty?: string;
  remark?: string;
}

interface StockCount {
  id: number;
  countNo: string;
  status: string;
  countAt?: string;
  remark?: string;
  createdAt?: string;
}

const route = useRoute();
const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

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

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<StockCount[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const searchQuery = ref('');
const statusFilter = ref<'all' | 'DRAFT' | 'APPROVED' | 'CANCELLED' | 'RED_FLUSHED'>('all');

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const showModal = ref(false);
const isEditing = ref(false);
const viewMode = ref(false);
const currentId = ref<number | null>(null);

const formData = reactive({
  countNo: '',
  countAt: '',
  remark: '',
  items: [] as StockCountItem[]
});

const modalTitle = computed(() => {
  if (viewMode.value) return t('action.view');
  return isEditing.value ? t('action.edit') : t('action.add');
});

const statusLabel = (status?: string) => {
  if (status === 'APPROVED') return t('status.approved');
  if (status === 'CANCELLED') return t('status.cancelled');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return t('status.draft');
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

const getLocationOptions = (warehouseId?: number | null) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const fetchOptions = async () => {
  try {
    const [productsRes, warehousesRes, locationsRes] = await Promise.all([
      request.get('/erp/products/options'),
      request.get('/erp/warehouses/options'),
      request.get('/erp/locations/options')
    ]);
    productOptions.value = productsRes.data.data || [];
    warehouseOptions.value = warehousesRes.data.data || [];
    locationOptions.value = locationsRes.data.data || [];
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
  formData.countAt = '';
  formData.remark = '';
  formData.items = [];
  isEditing.value = false;
  viewMode.value = false;
  currentId.value = null;
};

const addItem = () => {
  formData.items.push({ productId: undefined, warehouseId: undefined, locationId: null, systemQty: '0', countedQty: '' });
};

const removeItem = (index: number) => {
  formData.items.splice(index, 1);
};

const normalizeLocationId = (value?: number | null) => {
  if (value === -1) return null;
  return value == null ? null : value;
};

const fetchBalanceForRow = async (row: StockCountItem) => {
  if (!row.productId) {
    row.systemQty = '0';
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
  } catch (error) {
    row.systemQty = '0';
  }
};

const handleRowChange = (row: StockCountItem) => {
  row.locationId = normalizeLocationId(row.locationId ?? null);
  fetchBalanceForRow(row);
};

const calcDiff = (row: StockCountItem) => {
  const system = Number(row.systemQty || 0);
  const counted = Number(row.countedQty || 0);
  const diff = counted - system;
  return Number.isFinite(diff) ? diff.toFixed(4).replace(/\.?0+$/, '') : '0';
};

const openAddModal = async () => {
  isEditing.value = false;
  viewMode.value = false;
  currentId.value = null;
  resetForm();
  showModal.value = true;
  addItem();
  await fetchNextCountNo();
};

const loadDetail = async (id: number) => {
  const res: any = await request.get(`${apiPrefix.value}/${id}`);
  if (res.data.code === 200) {
    const detail = res.data.data;
    formData.countNo = detail.count.countNo;
    formData.countAt = detail.count.countAt || '';
    formData.remark = detail.count.remark || '';
    formData.items = (detail.items || []).map((item: any) => ({
      productId: item.productId,
      warehouseId: item.warehouseId,
      locationId: item.locationId,
      systemQty: String(item.systemQty ?? 0),
      countedQty: String(item.countedQty ?? ''),
      remark: item.remark || ''
    }));
    await Promise.all(formData.items.flatMap(item => [
      ensureProductOption(item.productId),
      ensureWarehouseOption(item.warehouseId),
      ensureLocationOption(item.locationId)
    ]));
    showModal.value = true;
  }
};

const openEditModal = async (row: StockCount) => {
  isEditing.value = true;
  viewMode.value = false;
  currentId.value = row.id;
  try {
    await loadDetail(row.id);
  } catch (error) {
    notifyError(error);
  }
};

const openViewModal = async (row: StockCount) => {
  isEditing.value = false;
  viewMode.value = true;
  currentId.value = row.id;
  try {
    await loadDetail(row.id);
  } catch (error) {
    notifyError(error);
  }
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

const saveData = async () => {
  if (!formData.countNo || formData.items.length === 0) {
    notifyWarning(t('message.required'));
    return;
  }
  for (const item of formData.items) {
    if (!item.productId) {
      notifyWarning(t('message.required'));
      return;
    }
    const countedQty = item.countedQty == null || item.countedQty === '' ? null : Number(item.countedQty);
    if (countedQty == null || Number.isNaN(countedQty) || countedQty < 0) {
      notifyWarning(t('message.invalidNumber'));
      return;
    }
  }
  try {
    const payload = {
      countNo: formData.countNo,
      countAt: formData.countAt,
      remark: formData.remark,
      items: formData.items.map(item => ({
        productId: item.productId,
        warehouseId: item.warehouseId,
        locationId: normalizeLocationId(item.locationId ?? null),
        countedQty: item.countedQty ? Number(item.countedQty) : 0,
        systemQty: item.systemQty ? Number(item.systemQty) : 0,
        remark: item.remark || ''
      }))
    };

    if (isEditing.value && currentId.value) {
      await request.put(`${apiPrefix.value}/${currentId.value}`, payload);
    } else {
      await request.post(apiPrefix.value, payload);
    }
    notifySuccess();
    showModal.value = false;
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleApprove = async (row: StockCount) => {
  try {
    await request.post(`${apiPrefix.value}/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleRedFlush = async (row: StockCount) => {
  try {
    await request.post(`${apiPrefix.value}/${row.id}/red-flush`);
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

onMounted(() => {
  fetchOptions();
  fetchList();
  bindPageSizeSync(size, fetchList);
});

onActivated(() => {
  fetchOptions();
  fetchList();
});
</script>

<style scoped>
.table-card :deep(.row-red-flushed td) {
  background-color: #fff1f0;
}

.table-card :deep(.row-red-flushed:hover > td) {
  background-color: #ffe7e6;
}

.detail-section {
  margin-top: 12px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.detail-table-wrapper {
  width: 100%;
}
</style>
