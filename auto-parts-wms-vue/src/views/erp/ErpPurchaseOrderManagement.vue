<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpPurchaseOrderManagement') }}</h2>
      <div class="table-toolbar">
        <div class="table-filters">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="table-search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <FuzzyProductSelect
            v-model="supplierFilter"
            :options="supplierOptions"
            :placeholder="$t('field.supplier')"
            class="table-search"
            @change="handleSearch"
          />
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search" clearable @change="handleSearch">
            <el-option :label="$t('filter.all')" value="" />
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="x"
            format="YYYY-MM-DD HH:mm:ss"
            :start-placeholder="$t('field.startTime')"
            :end-placeholder="$t('field.endTime')"
            @change="handleSearch"
            class="table-date-range--compact"
          />
        </div>
        <el-button type="primary" v-permission="'erp-purchase:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-purchase-order-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
          <ErpDataTableColumn v-if="canShow('supplier')" :label="$t('field.supplier')" min-width="160" column-key="supplier">
            <template #default="{ row }">
              {{ getSupplierName(row.supplierId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('totalAmount')" prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="240" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-purchase:edit'" :disabled="row.status !== 'DRAFT'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="success" size="small" v-permission="'erp-purchase:approve'" :disabled="row.status !== 'DRAFT'" @click="handleApprove(row)">{{ $t('action.approve') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-purchase:cancel'" :disabled="row.status === 'CANCELLED'" @click="handleCancel(row)">{{ $t('action.cancel') }}</el-button>
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

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="980px" @closed="resetForm">
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('field.orderNo')">
          <el-input v-model="formData.orderNo" :placeholder="$t('placeholder.optional')" />
        </el-form-item>
        <el-form-item :label="$t('field.supplier')" required>
          <FuzzyProductSelect
            v-model="formData.supplierId"
            :options="supplierOptions"
            :placeholder="$t('field.supplier')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </el-form>

      <div class="detail-section">
        <div class="detail-header">
          <h4>{{ $t('field.items') }}</h4>
          <el-button type="primary" plain size="small" @click="addItem">+ {{ $t('action.addItem') }}</el-button>
        </div>
        <ErpDataTable :data="formData.items" style="width: 100%" border stripe table-key="erp-purchase-order-management-9">
          <ErpDataTableColumn :label="$t('field.product')" min-width="200" column-key="product">
            <template #default="{ row }">
              <el-select v-model="row.productId" filterable clearable style="width: 100%" :placeholder="$t('placeholder.selectProduct')">
                <el-option v-for="item in productOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.warehouse')" min-width="160" column-key="warehouseLocation">
            <template #default="{ row }">
              <el-select v-model="row.warehouseId" filterable clearable style="width: 100%" :placeholder="$t('placeholder.selectWarehouse')">
                <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.location')" min-width="160" column-key="warehouseLocation">
            <template #default="{ row }">
              <el-select v-model="row.locationId" filterable clearable style="width: 100%" :placeholder="$t('placeholder.selectLocation')">
                <el-option v-for="item in getLocationOptions(row.warehouseId)" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.quantity')" width="140" column-key="quantity">
            <template #default="{ row }">
              <el-input-number v-model="row.qty" :min="0" style="width: 100%" />
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.price')" width="140" column-key="custom-12">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :step="0.01" style="width: 100%" />
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.taxRate')" width="140" column-key="custom-13">
            <template #default="{ row }">
              <el-input-number v-model="row.taxRate" :min="0" :max="1" :step="0.01" style="width: 100%" />
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.remark')" min-width="160" column-key="remark">
            <template #default="{ row }">
              <el-input v-model="row.remark" />
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn label="" width="80" align="center" column-key="actions">
            <template #default="{ $index }">
              <el-button type="danger" circle size="small" @click="removeItem($index)">x</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>

      <template #footer>
        <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { ElMessageBox } from 'element-plus';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface PurchaseOrderItem {
  id?: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qty?: number;
  price?: number;
  taxRate?: number;
  remark?: string;
  sortNo?: number;
}

interface PurchaseOrder {
  id: number;
  orderNo?: string;
  supplierId?: number;
  status: string;
  totalAmount?: number;
  createdAt?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const statusFilter = ref('');
const supplierFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<PurchaseOrder[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);

const supplierOptions = ref<OptionItem[]>([]);
const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const statusOptions = [
  { value: 'DRAFT', label: t('status.draft') },
  { value: 'APPROVED', label: t('status.approved') },
  { value: 'CANCELLED', label: t('status.cancelled') }
];

const defaultColumns = ['orderNo', 'supplier', 'status', 'totalAmount', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-purchase', defaultColumns);

const formData = reactive({
  orderNo: '',
  supplierId: null as number | null,
  remark: '',
  items: [] as PurchaseOrderItem[]
});

const canShow = (key: string) => isVisible(key);

const statusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'CANCELLED') return 'danger';
  return 'info';
};

const formatStatus = (status: string) => {
  const mapping: Record<string, string> = {
    DRAFT: t('status.draft'),
    APPROVED: t('status.approved'),
    CANCELLED: t('status.cancelled')
  };
  return mapping[status] || status;
};

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

const getSupplierName = (id?: number) => supplierOptions.value.find(item => item.id === id)?.name || '-';

const getLocationOptions = (warehouseId?: number) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const fetchSuppliers = async () => {
  try {
    const res: any = await request.get('/erp/suppliers');
    supplierOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchProducts = async () => {
  try {
    const res: any = await request.get('/erp/products');
    productOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchWarehouses = async () => {
  try {
    const res: any = await request.get('/erp/warehouses');
    warehouseOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchLocations = async () => {
  try {
    const res: any = await request.get('/erp/locations');
    locationOptions.value = res.data.data || [];
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
    if (statusFilter.value) params.status = statusFilter.value;
    if (supplierFilter.value) params.supplierId = supplierFilter.value;
    if (dateRange.value && dateRange.value.length === 2) {
      const start = Number(dateRange.value[0]);
      const end = Number(dateRange.value[1]);
      params.startAt = start;
      params.endAt = end;
    }

    const res: any = await request.get('/erp/purchase-orders/page', { params });
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

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  addItem();
  showModal.value = true;
};

const openEditModal = async (row: PurchaseOrder) => {
  isEditing.value = true;
  currentId.value = row.id;
  try {
    const res: any = await request.get(`/erp/purchase-orders/${row.id}`);
    if (res.data.code === 200) {
      const data = res.data.data || {};
      formData.orderNo = data.orderNo || '';
      formData.supplierId = data.supplierId || null;
      formData.remark = data.remark || '';
      formData.items = (data.items || []).map((item: any) => ({
        id: item.id,
        productId: item.productId,
        warehouseId: item.warehouseId,
        locationId: item.locationId,
        qty: item.qty,
        price: item.price,
        taxRate: item.taxRate,
        remark: item.remark
      }));
      if (!formData.items.length) addItem();
      showModal.value = true;
    }
  } catch (error) {
    notifyError(error);
  }
};

const resetForm = () => {
  formData.orderNo = '';
  formData.supplierId = null;
  formData.remark = '';
  formData.items = [];
};

const addItem = () => {
  formData.items.push({
    productId: undefined,
    warehouseId: undefined,
    locationId: undefined,
    qty: 1,
    price: 0,
    taxRate: 0,
    remark: ''
  });
};

const removeItem = (index: number) => {
  formData.items.splice(index, 1);
};

const saveData = async () => {
  if (!formData.supplierId) {
    notifyWarning(t('message.required'));
    return;
  }
  const validItems = formData.items.filter(item => item.productId);
  if (!validItems.length) {
    notifyWarning(t('message.noItems'));
    return;
  }
  for (const item of validItems) {
    if (!item.qty || item.qty <= 0) {
      notifyWarning(t('message.mustBePositive'));
      return;
    }
  }
  const payload = {
    orderNo: formData.orderNo || undefined,
    supplierId: formData.supplierId,
    remark: formData.remark,
    items: validItems.map((item, index) => ({
      productId: item.productId,
      warehouseId: item.warehouseId,
      locationId: item.locationId,
      qty: item.qty,
      price: item.price,
      taxRate: item.taxRate,
      remark: item.remark,
      sortNo: index + 1
    }))
  };

  try {
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/purchase-orders/${currentId.value}`, payload)
      : await request.post('/erp/purchase-orders', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleApprove = async (row: PurchaseOrder) => {
  try {
    await request.post(`/erp/purchase-orders/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const handleCancel = async (row: PurchaseOrder) => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('message.confirmRedFlush'),
      t('action.redFlush'),
      {
        inputPlaceholder: t('placeholder.required'),
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel')
      }
    );
    if (!value || !String(value).trim()) {
      return;
    }
    await request.post(`/erp/purchase-orders/${row.id}/cancel`, { reason: String(value).trim() });
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

onMounted(() => {
  fetchSuppliers();
  fetchProducts();
  fetchWarehouses();
  fetchLocations();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchSuppliers();
  fetchProducts();
  fetchWarehouses();
  fetchLocations();
  fetchList();
});
</script>

<style scoped>
:deep(.table-date-range--compact) {
  flex: 0 0 380px;
}

:deep(.table-date-range--compact.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.table-date-range--compact .el-range-input) {
  width: 132px;
}
</style>
