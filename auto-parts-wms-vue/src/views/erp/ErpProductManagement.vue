<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpProductManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--3">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="table-search erp-basic-field--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="categoryFilter" :placeholder="$t('field.category')" class="table-search erp-basic-field--narrow" clearable @change="handleSearch">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" v-permission="'erp-product:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" />
          <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="140" />
          <el-table-column v-if="canShow('category')" :label="$t('field.category')" min-width="140">
            <template #default="{ row }">
              {{ getCategoryName(row.categoryId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('unit')" :label="$t('field.unit')" min-width="120">
            <template #default="{ row }">
              {{ getUnitName(row.unitId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('defaultWarehouse')" :label="$t('field.defaultWarehouse')" min-width="140">
            <template #default="{ row }">
              {{ getWarehouseName(row.defaultWarehouseId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('defaultLocation')" :label="$t('field.defaultLocation')" min-width="140">
            <template #default="{ row }">
              {{ getLocationName(row.defaultLocationId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('price')" prop="salePrice" :label="$t('field.price')" min-width="120" />
          <el-table-column v-if="canShow('costPrice') && canViewCostPrice" :label="$t('field.costPrice')" min-width="120">
            <template #default="{ row }">
              {{ formatMoney(row.costPrice) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('minStock')" prop="minStock" :label="$t('field.minStock')" min-width="120" />
          <el-table-column v-if="canShow('maxStock')" prop="maxStock" :label="$t('field.maxStock')" min-width="120" />
          <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-product:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-product:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <el-drawer
      v-model="showModal"
      :title="isEditing ? $t('action.edit') : $t('action.add')"
      size="600px"
      @closed="resetForm"
      destroy-on-close
    >
      <el-form :model="formData" label-position="top" label-width="auto" class="drawer-form">
        <div class="form-mode-toggle">
          <span class="mode-label">展示方式</span>
          <el-radio-group v-model="layoutMode" size="small">
            <el-radio-button value="stacked">模块</el-radio-button>
            <el-radio-button value="tabs">标签页</el-radio-button>
          </el-radio-group>
        </div>

        <el-tabs v-if="layoutMode === 'tabs'" class="form-tabs">
          <el-tab-pane :label="$t('section.basicInfo')" name="basic">
            <div class="module-card">
              <div class="section-title">{{ $t('section.basicInfo') }}</div>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item :label="$t('field.code')" required>
                    <el-input v-model="formData.code" :placeholder="$t('placeholder.autoGenerated')" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="$t('field.name')" required>
                    <el-input v-model="formData.name" :placeholder="$t('field.name')" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item :label="$t('field.category')">
                    <el-select v-model="formData.categoryId" clearable filterable style="width: 100%" :placeholder="$t('field.category')">
                      <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="$t('field.unit')">
                    <el-select v-model="formData.unitId" clearable filterable style="width: 100%" :placeholder="$t('field.unit')">
                      <el-option v-for="item in unitOptions" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item :label="$t('field.status')">
                <el-switch
                  v-model="formData.enabled"
                  :active-text="$t('status.active')"
                  :inactive-text="$t('status.inactive')"
                  inline-prompt
                />
              </el-form-item>

              <el-form-item :label="$t('field.remark')">
                <el-input v-model="formData.remark" type="textarea" :rows="3" :placeholder="$t('field.remark')" />
              </el-form-item>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="$t('section.inventoryInfo')" name="inventory">
            <div class="module-card">
              <div class="section-title">{{ $t('section.inventoryInfo') }}</div>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item :label="$t('field.defaultWarehouse')">
                    <el-select
                      v-model="formData.defaultWarehouseId"
                      clearable
                      filterable
                      style="width: 100%"
                      :placeholder="$t('field.defaultWarehouse')"
                      @change="formData.defaultLocationId = null"
                    >
                      <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="$t('field.defaultLocation')">
                    <el-select
                      v-model="formData.defaultLocationId"
                      clearable
                      filterable
                      style="width: 100%"
                      :placeholder="$t('field.defaultLocation')"
                      :disabled="!formData.defaultWarehouseId"
                    >
                      <el-option
                        v-for="item in getLocationOptions(formData.defaultWarehouseId)"
                        :key="item.id"
                        :label="item.name"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item :label="$t('field.minStock')">
                    <DecimalInput v-model="formData.minStock" :scale="4" :placeholder="$t('field.minStock')" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="$t('field.maxStock')">
                    <DecimalInput v-model="formData.maxStock" :scale="4" :placeholder="$t('field.maxStock')" />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="$t('section.priceInfo')" name="price">
            <div class="module-card">
              <div class="section-title">{{ $t('section.priceInfo') }}</div>
              <el-form-item v-if="canViewCostPrice" :label="$t('field.costPrice')">
                <DecimalInput
                  v-model="formData.costPrice"
                  :scale="4"
                  :placeholder="$t('field.costPrice')"
                  :disabled="!canEditCostPrice"
                />
              </el-form-item>
              <el-form-item :label="$t('field.price')">
                <el-input v-model="formData.salePrice" type="number" :placeholder="$t('field.price')" />
              </el-form-item>

              <div class="section-title">{{ $t('field.customerCategoryPrice') }}</div>
              <div class="price-list-container">
                <div v-for="item in priceItems" :key="item.categoryId" class="price-item">
                  <span class="price-label">{{ item.categoryName }}</span>
                  <DecimalInput v-model="item.salePrice" :scale="2" class="price-input" />
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="$t('section.customFields')" name="custom">
            <div class="module-card">
              <div class="section-title">{{ $t('section.customFields') }}</div>
              <div class="custom-field-table">
                <el-table :data="customFields" style="width: 100%" size="small" border>
                  <el-table-column :label="$t('field.customFieldKey')" min-width="200">
                    <template #default="{ row }">
                      <el-input v-model="row.key" :placeholder="$t('field.customFieldKey')" />
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('field.customFieldValue')" min-width="240">
                    <template #default="{ row }">
                      <el-input v-model="row.value" :placeholder="$t('field.customFieldValue')" />
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('table.actions')" width="90">
                    <template #default="{ $index }">
                      <el-button link type="danger" @click="removeCustomField($index)">{{ $t('action.delete') }}</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button type="primary" plain size="small" class="add-custom-field" @click="addCustomField">
                  {{ $t('action.addField') }}
                </el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>

        <template v-else>
          <div class="form-section module-card">
            <div class="section-title">{{ $t('section.basicInfo') }}</div>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.code')" required>
                  <el-input v-model="formData.code" :placeholder="$t('placeholder.autoGenerated')" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.name')" required>
                  <el-input v-model="formData.name" :placeholder="$t('field.name')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.category')">
                  <el-select v-model="formData.categoryId" clearable filterable style="width: 100%" :placeholder="$t('field.category')">
                    <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.unit')">
                  <el-select v-model="formData.unitId" clearable filterable style="width: 100%" :placeholder="$t('field.unit')">
                    <el-option v-for="item in unitOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item :label="$t('field.status')">
              <el-switch
                v-model="formData.enabled"
                :active-text="$t('status.active')"
                :inactive-text="$t('status.inactive')"
                inline-prompt
              />
            </el-form-item>

            <el-form-item :label="$t('field.remark')">
              <el-input v-model="formData.remark" type="textarea" :rows="3" :placeholder="$t('field.remark')" />
            </el-form-item>
          </div>

          <div class="form-section module-card">
            <div class="section-title">{{ $t('section.inventoryInfo') }}</div>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.defaultWarehouse')">
                  <el-select
                    v-model="formData.defaultWarehouseId"
                    clearable
                    filterable
                    style="width: 100%"
                    :placeholder="$t('field.defaultWarehouse')"
                    @change="formData.defaultLocationId = null"
                  >
                    <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.defaultLocation')">
                  <el-select
                    v-model="formData.defaultLocationId"
                    clearable
                    filterable
                    style="width: 100%"
                    :placeholder="$t('field.defaultLocation')"
                    :disabled="!formData.defaultWarehouseId"
                  >
                    <el-option
                      v-for="item in getLocationOptions(formData.defaultWarehouseId)"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.minStock')">
                  <DecimalInput v-model="formData.minStock" :scale="4" :placeholder="$t('field.minStock')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.maxStock')">
                  <DecimalInput v-model="formData.maxStock" :scale="4" :placeholder="$t('field.maxStock')" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-section module-card">
            <div class="section-title">{{ $t('section.priceInfo') }}</div>
            <el-form-item v-if="canViewCostPrice" :label="$t('field.costPrice')">
              <DecimalInput
                v-model="formData.costPrice"
                :scale="4"
                :placeholder="$t('field.costPrice')"
                :disabled="!canEditCostPrice"
              />
            </el-form-item>
            <el-form-item :label="$t('field.price')">
              <el-input v-model="formData.salePrice" type="number" :placeholder="$t('field.price')" />
            </el-form-item>

            <div class="section-title">{{ $t('field.customerCategoryPrice') }}</div>
            <div class="price-list-container">
              <div v-for="item in priceItems" :key="item.categoryId" class="price-item">
                <span class="price-label">{{ item.categoryName }}</span>
                <DecimalInput v-model="item.salePrice" :scale="2" class="price-input" />
              </div>
            </div>
          </div>

          <div class="form-section module-card">
            <div class="section-title">{{ $t('section.customFields') }}</div>
            <div class="custom-field-table">
              <el-table :data="customFields" style="width: 100%" size="small" border>
                <el-table-column :label="$t('field.customFieldKey')" min-width="200">
                  <template #default="{ row }">
                    <el-input v-model="row.key" :placeholder="$t('field.customFieldKey')" />
                  </template>
                </el-table-column>
                <el-table-column :label="$t('field.customFieldValue')" min-width="240">
                  <template #default="{ row }">
                    <el-input v-model="row.value" :placeholder="$t('field.customFieldValue')" />
                  </template>
                </el-table-column>
                <el-table-column :label="$t('table.actions')" width="90">
                  <template #default="{ $index }">
                    <el-button link type="danger" @click="removeCustomField($index)">{{ $t('action.delete') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-button type="primary" plain size="small" class="add-custom-field" @click="addCustomField">
                {{ $t('action.addField') }}
              </el-button>
            </div>
          </div>
        </template>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import DecimalInput from '@/components/DecimalInput.vue';

interface OptionItem {
  id: number;
  name: string;
}

interface LocationOption extends OptionItem {
  warehouseId?: number;
}

interface ErpProduct {
  id: number;
  code: string;
  name: string;
  categoryId?: number;
  unitId?: number;
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  salePrice?: number;
  costPrice?: number;
  minStock?: number;
  maxStock?: number;
  enabled: boolean;
  remark?: string;
  extAttrs?: Record<string, string> | string | null;
}

interface ProductPriceItem {
  categoryId: number;
  categoryName: string;
  salePrice: string;
}

interface CustomField {
  key: string;
  value: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();
const authStore = useAuthStore();

const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const categoryFilter = ref<number | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<ErpProduct[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const layoutMode = ref<'stacked' | 'tabs'>('stacked');

const categoryOptions = ref<OptionItem[]>([]);
const customerCategoryOptions = ref<OptionItem[]>([]);
const unitOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<LocationOption[]>([]);
const priceItems = ref<ProductPriceItem[]>([]);
const currentPriceMap = ref<Map<number, string>>(new Map());
const customFields = ref<CustomField[]>([]);

const defaultColumns = ['code', 'name', 'category', 'unit', 'defaultWarehouse', 'defaultLocation', 'price', 'costPrice', 'minStock', 'maxStock', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-product', defaultColumns);

const formData = reactive({
  code: '',
  name: '',
  categoryId: null as number | null,
  unitId: null as number | null,
  defaultWarehouseId: null as number | null,
  defaultLocationId: null as number | null,
  salePrice: undefined as number | undefined,
  costPrice: '' as string,
  minStock: '' as string,
  maxStock: '' as string,
  enabled: true,
  remark: ''
});

const canShow = (key: string) => isVisible(key);
const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const canViewCostPrice = computed(() => hasPermission('erp-product:cost:view') || hasPermission('erp-product:cost:edit'));
const canEditCostPrice = computed(() => hasPermission('erp-product:cost:edit'));

const getCategoryName = (id?: number) => categoryOptions.value.find(item => item.id === id)?.name || '-';
const getUnitName = (id?: number) => unitOptions.value.find(item => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';
const getLocationName = (id?: number) => locationOptions.value.find(item => item.id === id)?.name || '-';
const formatMoney = (value?: number) => {
  if (value == null || Number.isNaN(value)) return '-';
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
};
const getLocationOptions = (warehouseId?: number | null) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const parseExtAttrs = (raw: unknown): CustomField[] => {
  if (raw == null || raw === '') return [];
  let data: unknown = raw;
  if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (!trimmed) return [];
    try {
      data = JSON.parse(trimmed);
    } catch (error) {
      console.warn('Invalid extAttrs JSON:', error);
      return [];
    }
  }
  if (Array.isArray(data)) {
    return data
      .filter(item => item && typeof item === 'object')
      .map(item => ({
        key: String((item as any).key ?? ''),
        value: (item as any).value == null ? '' : String((item as any).value)
      }))
      .filter(item => item.key);
  }
  if (data && typeof data === 'object') {
    return Object.entries(data as Record<string, unknown>).map(([key, value]) => ({
      key,
      value: value == null ? '' : String(value)
    }));
  }
  return [];
};

const buildExtAttrsPayload = (): string | null => {
  const entries = customFields.value
    .map(item => ({
      key: item.key?.trim() ?? '',
      value: item.value == null ? '' : String(item.value)
    }))
    .filter(item => item.key);
  if (!entries.length) return null;
  const payload: Record<string, string> = {};
  entries.forEach(item => {
    payload[item.key] = item.value;
  });
  return JSON.stringify(payload);
};

const addCustomField = () => {
  customFields.value.push({ key: '', value: '' });
};

const removeCustomField = (index: number) => {
  customFields.value.splice(index, 1);
};

const buildPriceItems = () => {
  priceItems.value = customerCategoryOptions.value.map(item => ({
    categoryId: item.id,
    categoryName: item.name,
    salePrice: currentPriceMap.value.get(item.id) || ''
  }));
};

const syncDefaultLocation = () => {
  if (!formData.defaultLocationId) return;
  const location = locationOptions.value.find(item => item.id === formData.defaultLocationId);
  if (location && formData.defaultWarehouseId && location.warehouseId !== formData.defaultWarehouseId) {
    formData.defaultLocationId = null;
  }
};

const fetchCategories = async () => {
  try {
    const res: any = await request.get('/erp/categories');
    categoryOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchCustomerCategories = async () => {
  try {
    const res: any = await request.get('/erp/customer-categories');
    customerCategoryOptions.value = res.data.data || [];
    buildPriceItems();
  } catch (error) {
    notifyError(error);
  }
};

const fetchUnits = async () => {
  try {
    const res: any = await request.get('/erp/units');
    unitOptions.value = res.data.data || [];
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
    syncDefaultLocation();
  } catch (error) {
    notifyError(error);
  }
};

const fetchNextCode = async () => {
  try {
    const res: any = await request.get('/erp/products/next-code');
    if (res.data.code === 200 && res.data.data) {
      formData.code = res.data.data;
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchProductPrices = async (productId: number) => {
  try {
    const res: any = await request.get('/erp/product-prices', { params: { productId } });
    const items = res.data.data || [];
    currentPriceMap.value = new Map(items.map((item: any) => [item.customerCategoryId, String(item.salePrice ?? '')]));
    buildPriceItems();
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
    if (statusFilter.value !== 'all') params.enabled = statusFilter.value === 'enabled';
    if (categoryFilter.value) params.categoryId = categoryFilter.value;

    const res: any = await request.get('/erp/products/page', { params });
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
  currentPriceMap.value = new Map();
  buildPriceItems();
  fetchNextCode();
  showModal.value = true;
};

const openEditModal = (row: ErpProduct) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.categoryId = row.categoryId || null;
  formData.unitId = row.unitId || null;
  formData.defaultWarehouseId = row.defaultWarehouseId || null;
  formData.defaultLocationId = row.defaultLocationId || null;
  syncDefaultLocation();
  formData.salePrice = row.salePrice;
  formData.costPrice = row.costPrice == null ? '' : String(row.costPrice);
  formData.minStock = row.minStock == null ? '' : String(row.minStock);
  formData.maxStock = row.maxStock == null ? '' : String(row.maxStock);
  formData.enabled = row.enabled;
  formData.remark = row.remark || '';
  customFields.value = parseExtAttrs(row.extAttrs);
  currentPriceMap.value = new Map();
  if (row.id) {
    fetchProductPrices(row.id);
  } else {
    buildPriceItems();
  }
  showModal.value = true;
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.categoryId = null;
  formData.unitId = null;
  formData.defaultWarehouseId = null;
  formData.defaultLocationId = null;
  formData.salePrice = undefined;
  formData.costPrice = '';
  formData.minStock = '';
  formData.maxStock = '';
  formData.enabled = true;
  formData.remark = '';
  customFields.value = [];
};

const normalizeNumber = (value: string | number | null | undefined) => {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  if (Number.isNaN(parsed)) return null;
  return parsed;
};

const saveProductPrices = async (productId: number) => {
  if (!priceItems.value.length) return;
  const items = priceItems.value
    .map(item => ({
      customerCategoryId: item.categoryId,
      salePrice: normalizeNumber(item.salePrice)
    }))
    .filter(item => item.salePrice != null);
  try {
    await request.put('/erp/product-prices', { items }, { params: { productId } });
  } catch (error) {
    notifyError(error);
  }
};

const saveData = async () => {
  if (!formData.code || !formData.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = {
      ...formData,
      costPrice: normalizeNumber(formData.costPrice),
      minStock: normalizeNumber(formData.minStock),
      maxStock: normalizeNumber(formData.maxStock),
      extAttrs: buildExtAttrsPayload()
    };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/products/${currentId.value}`, payload)
      : await request.post('/erp/products', payload);

    if (res.data.code === 200) {
      const productId = currentId.value || res.data.data?.id;
      if (productId) {
        await saveProductPrices(productId);
      }
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpProduct) => {
  try {
    await request.delete(`/erp/products/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchCategories();
  fetchCustomerCategories();
  fetchUnits();
  fetchWarehouses();
  fetchLocations();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchCategories();
  fetchCustomerCategories();
  fetchUnits();
  fetchWarehouses();
  fetchLocations();
  fetchList();
});
</script>

<style scoped>
.price-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 6px;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.price-label {
  width: 120px;
  color: #606266;
}

.drawer-form {
  padding-right: 16px;
}

.form-mode-toggle {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.mode-label {
  font-size: 13px;
  color: #606266;
}

.module-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.form-section {
  margin-bottom: 24px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-top: 24px;
  margin-bottom: 12px;
}

.module-card .section-title {
  margin-top: 0;
}

.price-list-container {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 4px;
}

.price-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.price-item:last-child {
  margin-bottom: 0;
}

.price-item .price-label {
  width: 120px;
  font-size: 14px;
  color: #606266;
}

.price-item .price-input {
  width: 180px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
}

.custom-field-table {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.custom-field-table :deep(.el-table) {
  width: 100%;
}

.add-custom-field {
  align-self: flex-start;
}
</style>
