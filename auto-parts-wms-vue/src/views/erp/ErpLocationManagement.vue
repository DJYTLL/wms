<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpLocationManagement') }}</div>
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
          <el-select v-model="warehouseFilter" :placeholder="$t('field.warehouse')" class="table-search erp-basic-field--narrow" clearable @change="handleSearch">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" v-permission="'erp-location:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
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
          <el-table-column v-if="canShow('warehouse')" :label="$t('field.warehouse')" min-width="160">
            <template #default="{ row }">
              {{ getWarehouseName(row.warehouseId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('aisle')" prop="aisle" :label="$t('field.aisle')" min-width="100" />
          <el-table-column v-if="canShow('rack')" prop="rack" :label="$t('field.rack')" min-width="100" />
          <el-table-column v-if="canShow('bin')" prop="bin" :label="$t('field.bin')" min-width="100" />
          <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-location:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-location:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="640px" @closed="resetForm">
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="$t('field.warehouse')" required>
          <el-select v-model="formData.warehouseId" filterable clearable style="width: 100%">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item :label="$t('field.aisle')">
          <el-input v-model="formData.aisle" />
        </el-form-item>
        <el-form-item :label="$t('field.rack')">
          <el-input v-model="formData.rack" />
        </el-form-item>
        <el-form-item :label="$t('field.bin')">
          <el-input v-model="formData.bin" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </el-form>
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
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { MASTER_DATA_CODE_HINT, isValidMasterCode, normalizeMasterCode } from '@/utils/erpMasterData';

interface OptionItem {
  id: number;
  name: string;
}

interface ErpLocation {
  id: number;
  code: string;
  name: string;
  warehouseId?: number;
  aisle?: string;
  rack?: string;
  bin?: string;
  enabled: boolean;
  remark?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const warehouseFilter = ref<number | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<ErpLocation[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);

const warehouseOptions = ref<OptionItem[]>([]);

const defaultColumns = ['code', 'name', 'warehouse', 'aisle', 'rack', 'bin', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-location', defaultColumns);

const formData = reactive({
  warehouseId: null as number | null,
  code: '',
  name: '',
  aisle: '',
  rack: '',
  bin: '',
  enabled: true,
  remark: ''
});

const canShow = (key: string) => isVisible(key);

const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';

const fetchWarehouses = async () => {
  try {
    const res: any = await request.get('/erp/warehouses');
    warehouseOptions.value = res.data.data || [];
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
    if (warehouseFilter.value) params.warehouseId = warehouseFilter.value;
    if (statusFilter.value !== 'all') params.enabled = statusFilter.value === 'enabled';

    const res: any = await request.get('/erp/locations/page', { params });
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
  showModal.value = true;
};

const openEditModal = (row: ErpLocation) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.warehouseId = row.warehouseId || null;
  formData.code = row.code;
  formData.name = row.name;
  formData.aisle = row.aisle || '';
  formData.rack = row.rack || '';
  formData.bin = row.bin || '';
  formData.enabled = row.enabled;
  formData.remark = row.remark || '';
  showModal.value = true;
};

const resetForm = () => {
  formData.warehouseId = null;
  formData.code = '';
  formData.name = '';
  formData.aisle = '';
  formData.rack = '';
  formData.bin = '';
  formData.enabled = true;
  formData.remark = '';
};

const saveData = async () => {
  const code = normalizeMasterCode(formData.code);
  const name = formData.name.trim();
  if (!code || !name || !formData.warehouseId) {
    notifyWarning(t('message.required'));
    return;
  }
  if (!isValidMasterCode(code)) {
    notifyWarning(MASTER_DATA_CODE_HINT);
    return;
  }
  try {
    const payload = {
      ...formData,
      code,
      name,
      aisle: formData.aisle.trim(),
      rack: formData.rack.trim(),
      bin: formData.bin.trim(),
      remark: formData.remark.trim()
    };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/locations/${currentId.value}`, payload)
      : await request.post('/erp/locations', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpLocation) => {
  try {
    await ElMessageBox.confirm(
      `确认删除库位“${row.name || row.code}”吗？仅未被库存、商品或业务单据引用的库位允许删除，已使用库位请改为停用。`,
      t('action.delete'),
      {
        type: 'warning',
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel')
      }
    );
    await request.delete(`/erp/locations/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error !== 'cancel') {
      notifyError(error);
    }
  }
};

onMounted(() => {
  fetchWarehouses();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchWarehouses();
  fetchList();
});
</script>
