<template>
  <div class="supplier-management">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.supplierManagement') }}</h2>
      <div class="actions">
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchQuery" 
            :placeholder="$t('action.search') + '...'" 
            class="search-input"
          />
        </div>
        <button class="btn btn-primary" @click="openAddModal" v-permission="'supplier:add'">
          <span class="icon">+</span> {{ $t('action.add') }}
        </button>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th v-if="canShow('name')">{{ $t('field.name') }}</th>
            <th v-if="canShow('contactPerson')">{{ $t('field.contactPerson') }}</th>
            <th v-if="canShow('phone')">{{ $t('field.phone') }}</th>
            <th v-if="canShow('email')">{{ $t('field.email') }}</th>
            <th v-if="canShow('status')">{{ $t('field.status') }}</th>
            <th>{{ $t('table.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="supplier in filteredSuppliers" :key="supplier.id">
            <td v-if="canShow('name')">{{ supplier.name }}</td>
            <td v-if="canShow('contactPerson')">{{ supplier.contactPerson }}</td>
            <td v-if="canShow('phone')">{{ supplier.phone }}</td>
            <td v-if="canShow('email')">{{ supplier.email }}</td>
            <td v-if="canShow('status')">
              <span class="status-badge" :class="supplier.status">
                {{ supplier.status === 'active' ? $t('status.active') : $t('status.inactive') }}
              </span>
            </td>
            <td>
              <button class="btn-icon" @click="openEditModal(supplier)" :title="$t('action.edit')" v-permission="'supplier:edit'">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
              </button>
              <button class="btn-icon delete" @click="confirmDelete(supplier)" :title="$t('action.delete')" v-permission="'supplier:delete'">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
              </button>
            </td>
          </tr>
          <tr v-if="filteredSuppliers.length === 0">
            <td :colspan="visibleColumnCount" class="empty-state">
              {{ $t('action.search') }} {{ $t('page.supplierManagement') }}...
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEditing ? $t('action.edit') : $t('action.add') }} {{ $t('page.supplierManagement') }}</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>{{ $t('field.name') }}</label>
            <input type="text" v-model="currentSupplier.name" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ $t('field.contactPerson') }}</label>
            <input type="text" v-model="currentSupplier.contactPerson" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ $t('field.phone') }}</label>
            <input type="text" v-model="currentSupplier.phone" class="form-input" />
          </div>
           <div class="form-group">
            <label>{{ $t('field.email') }}</label>
            <input type="text" v-model="currentSupplier.email" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ $t('field.address') }}</label>
            <input type="text" v-model="currentSupplier.address" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ $t('field.status') }}</label>
            <select v-model="currentSupplier.status" class="form-select">
              <option value="active">{{ $t('status.active') }}</option>
              <option value="inactive">{{ $t('status.inactive') }}</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModal">{{ $t('action.cancel') }}</button>
          <button class="btn btn-primary" @click="saveSupplier">{{ $t('action.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';

const { t } = useI18n();
const authStore = useAuthStore();

const defaultColumns = ['name', 'contactPerson', 'phone', 'email', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings(
  'supplier-management',
  defaultColumns
);
const columnPermissionMap: Record<string, string> = {
  name: 'column:supplier-management:name',
  contactPerson: 'column:supplier-management:contactPerson',
  phone: 'column:supplier-management:phone',
  email: 'column:supplier-management:email',
  status: 'column:supplier-management:status'
};

interface Supplier {
  id: number;
  name: string;
  contactPerson: string;
  phone: string;
  email: string;
  address: string;
  status: 'active' | 'inactive';
}

// Mock Data
const suppliers = ref<Supplier[]>([
  { id: 1, name: 'AutoParts Inc.', contactPerson: 'John Doe', phone: '123-456-7890', email: 'john@autoparts.com', address: '123 Industry Rd', status: 'active' },
  { id: 2, name: 'Global Components', contactPerson: 'Jane Smith', phone: '987-654-3210', email: 'jane@globalcomp.com', address: '456 Tech Park', status: 'active' },
]);

const searchQuery = ref('');
const showModal = ref(false);
const isEditing = ref(false);
const visibleColumnCount = computed(() => defaultColumns.filter(canShow).length + 1);

onMounted(() => {
  fetchTenantKeys();
});

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const initialFormState: Omit<Supplier, 'id'> = {
  name: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  status: 'active',
};

const currentSupplier = reactive<{ id?: number } & typeof initialFormState>({ ...initialFormState });

const filteredSuppliers = computed(() => {
  if (!searchQuery.value) return suppliers.value;
  const query = searchQuery.value.toLowerCase();
  return suppliers.value.filter(s => 
    s.name.toLowerCase().includes(query) || 
    s.contactPerson.toLowerCase().includes(query)
  );
});

const openAddModal = () => {
  isEditing.value = false;
  Object.assign(currentSupplier, { ...initialFormState, id: undefined });
  showModal.value = true;
};

const openEditModal = (supplier: Supplier) => {
  isEditing.value = true;
  Object.assign(currentSupplier, supplier);
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const saveSupplier = () => {
  if (isEditing.value && currentSupplier.id) {
    const index = suppliers.value.findIndex(s => s.id === currentSupplier.id);
    if (index !== -1) {
      suppliers.value[index] = { ...currentSupplier } as Supplier;
    }
  } else {
    const newId = Math.max(...suppliers.value.map(s => s.id), 0) + 1;
    suppliers.value.push({ ...currentSupplier, id: newId } as Supplier);
  }
  closeModal();
};

const confirmDelete = (supplier: Supplier) => {
  if (confirm(t('message.deleteConfirm'))) {
    suppliers.value = suppliers.value.filter(s => s.id !== supplier.id);
  }
};
</script>

<style scoped>
.supplier-management {
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
