<template>
  <div class="inbound-management">
    <!-- 页面头部：包含标题、搜索框和操作按钮 -->
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.inboundManagement') }}</h2>
      <div class="actions">
        <!-- 搜索输入框 -->
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchQuery" 
            :placeholder="$t('action.search') + '...'" 
            class="search-input"
          />
        </div>
        <!-- 添加按钮：受 'inbound:add' 权限控制 -->
        <button class="btn btn-primary" @click="openAddModal" v-permission="'inbound:add'">
          <span class="icon">+</span> {{ $t('action.add') }}
        </button>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th v-if="canShow('orderNumber')">{{ $t('field.orderNumber') }}</th>
            <th v-if="canShow('type')">{{ $t('field.type') }}</th>
            <th v-if="canShow('supplier')">{{ $t('field.supplier') }}</th>
            <th v-if="canShow('date')">{{ $t('field.date') }}</th>
            <th v-if="canShow('status')">{{ $t('field.status') }}</th>
            <th>{{ $t('table.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <!-- 遍历并显示入库单列表 -->
          <tr v-for="order in filteredOrders" :key="order.id">
            <td v-if="canShow('orderNumber')">{{ order.orderNumber }}</td>
            <td v-if="canShow('type')">
              <span class="type-badge" :class="order.type">
                {{ order.type === 'purchase' ? $t('orderType.purchase') : $t('orderType.return') }}
              </span>
            </td>
            <td v-if="canShow('supplier')">{{ getSupplierName(order.supplierId) }}</td>
            <td v-if="canShow('date')">{{ formatDateTimeDisplay(order.date) }}</td>
            <td v-if="canShow('status')">
              <span class="status-badge" :class="order.status">
                {{ order.status === 'received' ? $t('status.received') : $t('status.pending') }}
              </span>
            </td>
            <td>
              <!-- 编辑按钮：受 'inbound:edit' 权限控制 -->
              <button class="btn-icon" @click="openEditModal(order)" :title="$t('action.edit')" v-permission="'inbound:edit'">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
              </button>
              <!-- 删除按钮：受 'inbound:delete' 权限控制 -->
              <button class="btn-icon delete" @click="confirmDelete(order)" :title="$t('action.delete')" v-permission="'inbound:delete'">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
              </button>
            </td>
          </tr>
          <!-- 空状态显示 -->
          <tr v-if="filteredOrders.length === 0">
            <td :colspan="visibleColumnCount" class="empty-state">
              {{ $t('action.search') }} {{ $t('page.inboundManagement') }}...
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 全屏模态框：用于新增或编辑入库单 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content full-screen">
        <div class="modal-header">
          <h3>{{ isEditing ? $t('action.edit') : $t('action.add') }} {{ $t('page.inboundManagement') }}</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          
          <!-- 主表单信息区域 -->
          <div class="form-grid">
            <div class="form-group">
              <label>{{ $t('field.orderNumber') }}</label>
              <input type="text" v-model="currentOrder.orderNumber" class="form-input" disabled />
            </div>
            
            <div class="form-group">
              <label>{{ $t('field.type') }}</label>
              <SearchableSelect 
                v-model="currentOrder.type" 
                :options="typeOptions" 
                :placeholder="$t('field.type')"
              />
            </div>

            <div class="form-group">
              <label>{{ $t('field.supplier') }}</label>
              <!-- 使用可搜索的选择组件 -->
              <SearchableSelect 
                v-model="currentOrder.supplierId" 
                :options="supplierOptions" 
                :placeholder="$t('field.supplier')"
              />
            </div>

            <div class="form-group">
              <label>{{ $t('field.date') }}</label>
              <el-date-picker
                v-model="currentOrder.date"
                type="datetime"
                :placeholder="$t('field.date')"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </div>

            <div class="form-group">
              <label>{{ $t('field.status') }}</label>
              <SearchableSelect 
                v-model="currentOrder.status" 
                :options="statusOptions" 
                :placeholder="$t('field.status')"
              />
            </div>
          </div>

          <!-- 明细项目区域 -->
          <div class="detail-section">
            <div class="detail-header">
              <h4>{{ $t('field.items') }}</h4>
              <el-button type="primary" plain size="small" @click="addItem">
                + {{ $t('action.addItem') }}
              </el-button>
            </div>
            
            <div class="detail-table-wrapper">
              <el-table :data="currentOrder.items" style="width: 100%" border stripe>
                <!-- 商品选择列 -->
                <el-table-column v-if="canShow('product')" :label="$t('field.product')" min-width="200">
                  <template #default="{ row }">
                    <el-select 
                      v-model="row.productId" 
                      filterable 
                      :placeholder="$t('placeholder.selectProduct')"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="p in productOptions"
                        :key="p.value"
                        :label="p.label"
                        :value="p.value"
                      >
                        <span style="float: left">{{ p.label }}</span>
                        <span style="float: right; color: #8492a6; font-size: 13px">{{ p.code }}</span>
                      </el-option>
                    </el-select>
                  </template>
                </el-table-column>

                <!-- 数量输入列 -->
                <el-table-column v-if="canShow('quantity')" :label="$t('field.quantity')" width="160">
                  <template #default="{ row }">
                    <el-input-number v-model="row.quantity" :min="1" style="width: 100%" />
                  </template>
                </el-table-column>

                <!-- 仓库选择列 -->
                <el-table-column v-if="canShow('warehouseLabel')" :label="$t('field.warehouseLabel')" min-width="150">
                  <template #default="{ row }">
                    <el-select 
                      v-model="row.warehouseId" 
                      filterable 
                      :placeholder="$t('placeholder.selectWarehouse')"
                      style="width: 100%"
                      @change="row.shelfId = undefined"
                    >
                      <el-option
                        v-for="w in warehouseOptions"
                        :key="w.value"
                        :label="w.label"
                        :value="w.value"
                      />
                    </el-select>
                  </template>
                </el-table-column>

                <!-- 货架选择列（根据仓库联动） -->
                <el-table-column v-if="canShow('shelfLabel')" :label="$t('field.shelfLabel')" min-width="150">
                  <template #default="{ row }">
                    <el-select 
                      v-model="row.shelfId" 
                      filterable 
                      :placeholder="$t('placeholder.selectShelf')"
                      style="width: 100%"
                      :disabled="!row.warehouseId"
                    >
                      <el-option
                        v-for="s in getShelfOptions(row.warehouseId)"
                        :key="s.value"
                        :label="s.label"
                        :value="s.value"
                      />
                    </el-select>
                  </template>
                </el-table-column>

                <!-- 删除行操作 -->
                <el-table-column label="" width="80" align="center">
                  <template #default="{ $index }">
                    <el-button 
                      type="danger" 
                      circle 
                      size="small"
                      @click="removeItem($index)"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModal">{{ $t('action.cancel') }}</button>
          <button class="btn btn-primary" @click="saveOrder">{{ $t('action.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useMockDataStore } from '@/stores/mockData';
import SearchableSelect from '@/components/SearchableSelect.vue';
import { useApiError } from '@/composables/useApiError';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';

const { t } = useI18n();
const { notifySuccess, notifyWarning } = useApiError();
// 引入 Mock 数据 Store，实际生产环境应替换为 API 调用
const mockStore = useMockDataStore();
const authStore = useAuthStore();

// 页面状态变量
const searchQuery = ref('');
const showModal = ref(false);
const isEditing = ref(false);
const defaultColumns = [
  'orderNumber',
  'type',
  'supplier',
  'date',
  'status',
  'product',
  'quantity',
  'warehouseLabel',
  'shelfLabel'
];
const { isVisible, fetchTenantKeys } = useColumnSettings(
  'inbound-management',
  defaultColumns
);
const columnPermissionMap: Record<string, string> = {
  orderNumber: 'column:inbound-management:orderNumber',
  type: 'column:inbound-management:type',
  supplier: 'column:inbound-management:supplier',
  date: 'column:inbound-management:date',
  status: 'column:inbound-management:status',
  product: 'column:inbound-management:product',
  quantity: 'column:inbound-management:quantity',
  warehouseLabel: 'column:inbound-management:warehouseLabel',
  shelfLabel: 'column:inbound-management:shelfLabel'
};

// 计算属性：过滤后的订单列表
const filteredOrders = computed(() => {
  if (!searchQuery.value) return mockStore.inboundOrders;
  const query = searchQuery.value.toLowerCase();
  return mockStore.inboundOrders.filter(o => 
    o.orderNumber.toLowerCase().includes(query)
  );
});

const visibleColumnCount = computed(() => {
  const mainColumns = ['orderNumber', 'type', 'supplier', 'date', 'status'];
  return mainColumns.filter(canShow).length + 1;
});

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

// --- 下拉选项数据 (从 Mock Store 转换) ---
const supplierOptions = computed(() => 
  mockStore.suppliers.map(s => ({ value: s.id, label: s.name }))
);

const productOptions = computed(() => 
  mockStore.products.map(p => ({ value: p.id, label: p.name, code: p.sku }))
);

const warehouseOptions = computed(() => 
  mockStore.warehouses.map(w => ({ value: w.id, label: w.name, code: w.code }))
);

const typeOptions = computed(() => [
  { value: 'purchase', label: t('purchase') },
  { value: 'return', label: t('return') }
]);

const statusOptions = computed(() => [
  { value: 'pending', label: t('pending') },
  { value: 'received', label: t('received') }
]);

// 辅助方法：根据仓库ID获取对应货架选项
const getShelfOptions = (warehouseId?: number) => {
  if (!warehouseId) return [];
  return mockStore.shelves
    .filter(s => s.warehouseId === warehouseId)
    .map(s => ({ value: s.id, label: s.name, code: s.code }));
};

// 辅助方法：根据ID获取供应商名称
const getSupplierName = (id?: number) => {
  const s = mockStore.suppliers.find(x => x.id === id);
  return s ? s.name : '-';
};

// --- 日期格式化 ---
const formatDateTimeDisplay = (dateStr: string) => {
  if (!dateStr) return '-';
  // 尝试解析并格式化日期
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit', 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit',
    hour12: false 
  }).replace(/\//g, '-');
};

const getNowDateTimeString = () => {
  const now = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
};

// --- 类型定义 ---
interface InboundOrderItem {
  productId?: number;
  quantity: number;
  warehouseId?: number;
  shelfId?: number;
}

interface InboundOrderForm {
  id?: number;
  orderNumber: string;
  type: string;
  status: string;
  date: string;
  supplierId?: number;
  items: InboundOrderItem[];
}

const initialFormState: InboundOrderForm = {
  orderNumber: '',
  type: 'purchase',
  status: 'pending',
  date: '',
  supplierId: undefined,
  items: []
};

// 当前操作的订单对象
const currentOrder = reactive<InboundOrderForm>({ ...initialFormState });

// 创建空明细行数据
const createEmptyItem = (): InboundOrderItem => ({
  productId: undefined,
  quantity: 1,
  warehouseId: undefined,
  shelfId: undefined
});

// --- 事件处理 ---

// 打开新增模态框
const openAddModal = () => {
  isEditing.value = false;
  // 演示逻辑：自动生成单号
  const nextId = (mockStore.inboundOrders.length + 1).toString().padStart(3, '0');
  const now = new Date();
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
  
  // 重置表单
  Object.assign(currentOrder, JSON.parse(JSON.stringify(initialFormState)));
  currentOrder.orderNumber = `IN-${dateStr}-${nextId}`;
  currentOrder.date = getNowDateTimeString(); // 设置为当前时间
  
  // 默认生成5行空明细
  currentOrder.items = Array.from({ length: 5 }, createEmptyItem);
  
  showModal.value = true;
};

// 打开编辑模态框
const openEditModal = (order: any) => {
  isEditing.value = true;
  const orderCopy = JSON.parse(JSON.stringify(order));
  Object.assign(currentOrder, orderCopy);
  
  // 保证至少有一行明细
  if (currentOrder.items.length === 0) {
    addItem();
  }
  
  showModal.value = true;
};

// 关闭模态框
const closeModal = () => {
  showModal.value = false;
};

// 添加一行明细
const addItem = () => {
  currentOrder.items.push(createEmptyItem());
};

// 移除一行明细
const removeItem = (index: number) => {
  currentOrder.items.splice(index, 1);
};

// 保存订单逻辑
const saveOrder = () => {
  // 1. 过滤掉无效行（未选产品）
  const validItems = currentOrder.items.filter(item => item.productId !== undefined);

  // 2. 校验剩余行数据
  for (const item of validItems) {
    if (!item.quantity || item.quantity <= 0) {
      notifyWarning(`${t('field.quantity')} ${t('message.mustBePositive')}`);
      return;
    }
    if (!item.warehouseId) {
      notifyWarning(t('placeholder.selectWarehouse'));
      return;
    }
    // 暂时不强制校验货架
    // if (!item.shelfId) { ElMessage.warning(t('select-shelf')); return; }
  }

  if (validItems.length === 0) {
    notifyWarning(t('message.noItems'));
    return;
  }

  const orderToSave = JSON.parse(JSON.stringify(currentOrder));
  orderToSave.items = validItems; // 仅保存有效行

  if (isEditing.value && orderToSave.id) {
    const index = mockStore.inboundOrders.findIndex(o => o.id === orderToSave.id);
    if (index !== -1) {
      mockStore.inboundOrders[index] = orderToSave;
    }
  } else {
    const newId = Math.max(...mockStore.inboundOrders.map(o => o.id), 0) + 1;
    mockStore.inboundOrders.push({ ...orderToSave, id: newId });
  }
  
  notifySuccess(`${t('action.save')} ${t('message.success')}`);
  closeModal();
};

// 确认删除逻辑
const confirmDelete = (order: any) => {
  if (confirm(t('message.deleteConfirm'))) {
    mockStore.inboundOrders = mockStore.inboundOrders.filter(o => o.id !== order.id);
  }
};
</script>

<style scoped>
.inbound-management {
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

.btn-sm {
  padding: 4px 10px;
  font-size: 13px;
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

/* Table Styles */
.table-container {
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
  /* overflow: hidden;  <-- REMOVED to allow dropdowns to show */
  overflow: visible; 
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

/* Badge Styles */
.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.received { background-color: #e3f9e5; color: #0f5132; }
.status-badge.pending { background-color: #fff8e1; color: #b45309; }

.type-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  border: 1px solid #e5e5e5;
  background-color: #fafafa;
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
.btn-icon:hover { background-color: #f0f8ff; }
.btn-icon.delete { color: #ff3b30; }
.btn-icon.delete:hover { background-color: #fff0f0; }

.empty-state { text-align: center; padding: 40px; color: #86868b; }

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.2);
}

/* Full Screen Mode */
.modal-content.full-screen {
  width: 95vw;
  height: 90vh;
  max-width: 1400px; /* Optional cap */
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e5e5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.modal-header h3 { margin: 0; font-size: 20px; }

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #86868b;
  transition: color 0.2s;
}
.close-btn:hover { color: #333; }

.modal-body {
  padding: 24px;
  overflow-y: auto; /* Scrollable content */
  flex-grow: 1;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 3 columns for wider screen */
  gap: 20px;
  margin-bottom: 30px;
  background: #fbfbfd;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
}

.form-input, .form-select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d1d6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  height: 38px; /* Match SearchableSelect height */
}

.form-input.sm {
  height: 32px;
  font-size: 13px;
}

.form-input:focus, .form-select:focus {
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #e5e5e5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-shrink: 0;
  background: white;
}

/* Details Section */
.detail-section {
  border-top: 1px solid #e5e5e5;
  padding-top: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-header h4 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.detail-table-wrapper {
  /* To allow dropdowns to overflow, we avoid overflow:hidden here unless necessary. 
     If the table is very long, we might need min-height. */
  min-height: 200px; 
}

.detail-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.detail-table th {
  text-align: left;
  font-size: 13px;
  color: #86868b;
  padding: 10px;
  border-bottom: 2px solid #f0f0f0;
  background: white;
  position: sticky;
  top: 0;
  z-index: 10;
}

.detail-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #f5f5f5;
  vertical-align: top;
}

.empty-text {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 40px;
  background: #fbfbfd;
  border-radius: 8px;
}

.text-center { text-align: center; }

/* 完全隐藏明细表格内输入组件的边框，包括 Hover 状态 */
:deep(.detail-table-wrapper .el-select__wrapper),
:deep(.detail-table-wrapper .el-input__wrapper),
:deep(.detail-table-wrapper .el-input-number) {
  box-shadow: none !important;
  border: none !important;
  background-color: transparent !important;
}

/* 移除 Input Number 的左右边框 */
:deep(.detail-table-wrapper .el-input-number__decrease),
:deep(.detail-table-wrapper .el-input-number__increase) {
  border: none !important;
  background-color: transparent !important;
}

/* 仅在 Focus 聚焦时显示淡淡的底边线，提示用户正在编辑哪一行 */
:deep(.detail-table-wrapper .el-select.is-focus .el-select__wrapper),
:deep(.detail-table-wrapper .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 1px 0 0 #409eff !important; /* 仅底边线 */
}

/* 移除 Hover 时的边框恢复逻辑 */
:deep(.detail-table-wrapper .el-select:hover .el-select__wrapper),
:deep(.detail-table-wrapper .el-input__wrapper:hover) {
  box-shadow: none !important;
}
</style>
