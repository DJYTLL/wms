<template>
  <div class="product-management">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.productManagement') }}</h2>
      <div class="actions">
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchQuery" 
            :placeholder="$t('action.search') + '...'" 
            class="search-input"
          />
        </div>
        <button class="btn btn-primary" @click="openAddModal" v-permission="'product:add'">
          <span class="icon">+</span> {{ $t('action.add') }}
        </button>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th v-if="canShow('name')">{{ $t('field.name') }}</th>
            <th v-if="canShow('sku')">{{ $t('field.sku') }}</th>
            <th v-if="canShow('price')">{{ $t('field.price') }}</th>
            <th v-if="canShow('unit')">{{ $t('field.unit') }}</th>
            <th v-if="canShow('categoryLabel')">{{ $t('field.categoryLabel') }}</th>
            <th v-if="canShow('warehouseLabel')">{{ $t('field.warehouseLabel') }}</th>
            <th v-if="canShow('shelfLabel')">{{ $t('field.shelfLabel') }}</th>
            <th v-if="canShow('status')">{{ $t('field.status') }}</th>
            <th>{{ $t('table.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in filteredProducts" :key="product.id">
            <td v-if="canShow('name')">{{ product.name }}</td>
            <td v-if="canShow('sku')">{{ product.sku }}</td>
            <td v-if="canShow('price')">{{ product.price }}</td>
            <td v-if="canShow('unit')">{{ getUnitName(product.unitId) }}</td>
            <td v-if="canShow('categoryLabel')">{{ getCategoryName(product.categoryId) }}</td>
            <td v-if="canShow('warehouseLabel')">{{ getWarehouseName(product.shelfId) }}</td>
            <td v-if="canShow('shelfLabel')">{{ getShelfName(product.shelfId) }}</td>
            <td v-if="canShow('status')">
              <span class="status-badge" :class="product.status">
                {{ product.status === 'active' ? $t('status.active') : $t('status.inactive') }}
              </span>
            </td>
            <td>
              <button class="btn-icon" @click="openEditModal(product)" :title="$t('action.edit')" v-permission="'product:edit'">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
              </button>
              <button class="btn-icon delete" @click="confirmDelete(product)" :title="$t('action.delete')" v-permission="'product:delete'">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
              </button>
            </td>
          </tr>
          <tr v-if="filteredProducts.length === 0">
            <td :colspan="visibleColumnCount" class="empty-state">
              {{ $t('action.search') }} {{ $t('page.productManagement') }}...
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEditing ? $t('action.edit') : $t('action.add') }} {{ $t('page.productManagement') }}</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>{{ $t('field.name') }}</label>
            <input type="text" v-model="currentProduct.name" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ $t('field.sku') }}</label>
            <input type="text" v-model="currentProduct.sku" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ $t('field.price') }}</label>
            <input type="number" v-model="currentProduct.price" class="form-input" />
          </div>
          
          <!-- Unit Selection -->
          <div class="form-group">
            <label>{{ $t('field.unit') }}</label>
            <select v-model="currentProduct.unitId" class="form-select">
              <option :value="undefined">Select Unit</option>
              <option v-for="u in mockStore.units" :key="u.id" :value="u.id">
                {{ u.name }} ({{ u.symbol }})
              </option>
            </select>
          </div>

          <!-- Category Selection -->
          <div class="form-group">
            <label>{{ $t('field.categoryLabel') }}</label>
            <select v-model="currentProduct.categoryId" class="form-select">
              <option :value="undefined">Select Category</option>
              <option v-for="c in mockStore.categories" :key="c.id" :value="c.id">
                {{ c.name }}
              </option>
            </select>
          </div>

          <!-- Warehouse Selection -->
          <div class="form-group">
            <label>{{ $t('field.warehouseLabel') }}</label>
            <select v-model="selectedWarehouseId" class="form-select" @change="handleWarehouseChange">
              <option :value="undefined">Select Warehouse</option>
              <option v-for="w in mockStore.warehouses" :key="w.id" :value="w.id">
                {{ w.name }}
              </option>
            </select>
          </div>

          <!-- Shelf Selection -->
          <div class="form-group">
            <label>{{ $t('field.shelfLabel') }}</label>
            <select v-model="currentProduct.shelfId" class="form-select" :disabled="!selectedWarehouseId">
              <option :value="undefined">Select Shelf</option>
              <option v-for="s in availableShelves" :key="s.id" :value="s.id">
                {{ s.name }} ({{ s.code }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>{{ $t('field.status') }}</label>
            <select v-model="currentProduct.status" class="form-select">
              <option value="active">{{ $t('status.active') }}</option>
              <option value="inactive">{{ $t('status.inactive') }}</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModal">{{ $t('action.cancel') }}</button>
          <button class="btn btn-primary" @click="saveProduct">{{ $t('action.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useMockDataStore } from '@/stores/mockData';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';

const { t } = useI18n();
const mockStore = useMockDataStore();
const authStore = useAuthStore();

const defaultColumns = ['name', 'sku', 'price', 'unit', 'categoryLabel', 'warehouseLabel', 'shelfLabel', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings(
  'product-management',
  defaultColumns
);
const columnPermissionMap: Record<string, string> = {
  name: 'column:product-management:name',
  sku: 'column:product-management:sku',
  price: 'column:product-management:price',
  unit: 'column:product-management:unit',
  categoryLabel: 'column:product-management:categoryLabel',
  warehouseLabel: 'column:product-management:warehouseLabel',
  shelfLabel: 'column:product-management:shelfLabel',
  status: 'column:product-management:status'
};

onMounted(() => {
  console.log('ProductManagement loaded');
  fetchTenantKeys();
});

const searchQuery = ref('');
const showModal = ref(false);
const isEditing = ref(false);
const selectedWarehouseId = ref<number | undefined>(undefined);

// Using store directly
const filteredProducts = computed(() => {
  if (!searchQuery.value) return mockStore.products;
  const query = searchQuery.value.toLowerCase();
  return mockStore.products.filter(p => 
    p.name.toLowerCase().includes(query) || 
    p.sku.toLowerCase().includes(query)
  );
});

const visibleColumnCount = computed(() => defaultColumns.filter(canShow).length + 1);

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

// Helpers to get names from IDs
const getUnitName = (id?: number) => {
  const u = mockStore.units.find(x => x.id === id);
  return u ? u.name : '-';
};

const getCategoryName = (id?: number) => {
  const c = mockStore.categories.find(x => x.id === id);
  return c ? c.name : '-';
};

const getShelfName = (id?: number) => {
  const s = mockStore.shelves.find(x => x.id === id);
  return s ? s.name : '-';
};

const getWarehouseName = (shelfId?: number) => {
  const s = mockStore.shelves.find(x => x.id === shelfId);
  if (!s) return '-';
  const w = mockStore.warehouses.find(x => x.id === s.warehouseId);
  return w ? w.name : '-';
};

// Filter shelves based on selected warehouse
const availableShelves = computed(() => {
  if (!selectedWarehouseId.value) return [];
  return mockStore.shelves.filter(s => s.warehouseId === selectedWarehouseId.value);
});

interface ProductForm {
  id?: number;
  name: string;
  sku: string;
  price: number;
  unitId?: number;
  categoryId?: number;
  shelfId?: number;
  status: string;
}

const initialFormState: ProductForm = {
  name: '',
  sku: '',
  price: 0,
  unitId: undefined,
  categoryId: undefined,
  shelfId: undefined,
  status: 'active',
};

const currentProduct = reactive<ProductForm>({ ...initialFormState });

const handleWarehouseChange = () => {
  currentProduct.shelfId = undefined;
};

const openAddModal = () => {
  isEditing.value = false;
  selectedWarehouseId.value = undefined;
  Object.assign(currentProduct, { ...initialFormState, id: undefined });
  showModal.value = true;
};

const openEditModal = (product: any) => {
  isEditing.value = true;
  Object.assign(currentProduct, product);
  
  // Resolve warehouse from shelf to populate dropdown
  const shelf = mockStore.shelves.find(s => s.id === product.shelfId);
  selectedWarehouseId.value = shelf ? shelf.warehouseId : undefined;
  
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const saveProduct = () => {
  if (isEditing.value && currentProduct.id) {
    const index = mockStore.products.findIndex(p => p.id === currentProduct.id);
    if (index !== -1) {
      mockStore.products[index] = { ...currentProduct } as any;
    }
  } else {
    const newId = Math.max(...mockStore.products.map(p => p.id), 0) + 1;
    mockStore.products.push({ ...currentProduct, id: newId } as any);
  }
  closeModal();
};

const confirmDelete = (product: any) => {
  if (confirm(t('message.deleteConfirm'))) {
    mockStore.products = mockStore.products.filter(p => p.id !== product.id);
  }
};
</script>

<style scoped>
/* Reuse existing styles or import common styles */
.product-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #d1d1d6;
  border-radius: 6px;
  font-size: 14px;
  width: 200px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #0071e3;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-primary {
  background-color: #0071e3;
  color: white;
}

.btn-primary:hover {
  background-color: #0077ed;
}

.btn-secondary {
  background-color: #f5f5f7;
  color: #1d1d1f;
  border: 1px solid #d1d1d6;
}

.btn-secondary:hover {
  background-color: #e5e5e5;
}

.icon {
  font-size: 18px;
  line-height: 1;
}

.table-container {
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  background-color: #f5f5f7;
  padding: 12px 16px;
  font-weight: 600;
  font-size: 13px;
  color: #86868b;
  border-bottom: 1px solid #e5e5e5;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #1d1d1f;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background-color: #e3f9e5;
  color: #0f5132;
}

.status-badge.inactive {
  background-color: #f5f5f7;
  color: #666;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  margin-right: 4px;
  color: #0071e3;
  border-radius: 4px;
}

.btn-icon:hover {
  background-color: #f0f8ff;
}

.btn-icon.delete {
  color: #ff3b30;
}

.btn-icon.delete:hover {
  background-color: #fff0f0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #86868b;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 10px;
  width: 500px;
  max-width: 90%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e5e5e5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #86868b;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
}

.form-input, .form-select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d1d6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus, .form-select:focus {
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #e5e5e5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
