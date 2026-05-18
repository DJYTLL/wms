<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCustomerManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar">
          <div class="erp-basic-filters erp-basic-filters--5">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('placeholder.keyword')"
              class="table-search erp-basic-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="contactQuery"
              :placeholder="$t('field.contactPerson')"
              class="table-search erp-basic-field--narrow"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="phoneQuery"
              :placeholder="$t('field.phone')"
              class="table-search erp-basic-field--narrow"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-select v-model="categoryFilter" :placeholder="$t('field.customerCategory')" class="table-search erp-basic-field--narrow" clearable>
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button @click="handleReset">{{ $t('action.resetDefault') }}</el-button>
            <el-button type="primary" v-permission="'erp-customer:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-customer-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('category')" :label="$t('field.customerCategory')" min-width="140" column-key="customer">
            <template #default="{ row }">
              {{ getCategoryName(row.categoryId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('contact')" prop="contact" :label="$t('field.contactPerson')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('phone')" prop="phone" :label="$t('field.phone')" min-width="130" />
          <ErpDataTableColumn v-if="canShow('email')" prop="email" :label="$t('field.email')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-customer:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-customer:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <el-dialog v-model="showModal" :title="isEditing ? $t('action.edit') : $t('action.add')" width="760px" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" class="form-grid">
        <el-form-item :label="$t('field.code')" required class="span-2">
          <el-input v-model="formData.code" :placeholder="$t('placeholder.autoGenerated')" :disabled="!isEditing" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item :label="$t('field.customerCategory')" required>
          <el-select v-model="formData.categoryId" clearable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.defaultSettlementMethod')">
          <el-select v-model="formData.defaultSettlementMethodCode" clearable style="width: 100%">
            <el-option v-for="item in settlementMethodOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.defaultReceiptMethod')">
          <el-select v-model="formData.defaultReceiptMethodCode" clearable style="width: 100%">
            <el-option v-for="item in receiptMethodOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.deliveryMethod')">
          <el-select v-model="formData.deliveryMethodCode" clearable style="width: 100%">
            <el-option v-for="item in deliveryMethodOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-divider class="span-2">{{ $t('field.contacts') }}</el-divider>
        <el-form-item label="" label-width="0px" class="span-2 no-label">
          <div class="contact-table">
            <ErpDataTable
              :data="formData.contacts"
              border
              size="small"
              :empty-text="$t('table.empty')"
             table-key="erp-customer-management-11">
              <ErpDataTableColumn :label="$t('field.contactPerson')" min-width="120" column-key="custom-10">
                <template #default="{ row }">
                  <el-input v-model="row.name" :placeholder="$t('field.contactPerson')" />
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn :label="$t('field.phone')" min-width="120" column-key="custom-11">
                <template #default="{ row }">
                  <el-input v-model="row.phone" :placeholder="$t('field.phone')" />
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn :label="$t('field.mobile')" min-width="120" column-key="custom-12">
                <template #default="{ row }">
                  <el-input v-model="row.mobile" :placeholder="$t('field.mobile')" />
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn :label="$t('field.email')" min-width="180" column-key="custom-13">
                <template #default="{ row }">
                  <el-input v-model="row.email" :placeholder="$t('field.email')" />
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn :label="$t('table.actions')" width="90" align="center" column-key="actions">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeContact($index)">{{ $t('action.delete') }}</el-button>
                </template>
              </ErpDataTableColumn>
            </ErpDataTable>
            <div class="contact-table__actions">
              <el-button type="primary" plain size="small" @click="addContact">
                {{ $t('action.addContact') }}
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="$t('field.address')" class="span-2">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')" class="span-2">
          <el-input v-model="formData.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
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
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import type { FormInstance, FormRules } from 'element-plus';

interface ErpCustomer {
  id: number;
  code: string;
  name: string;
  categoryId?: number;
  defaultSettlementMethodCode?: string;
  defaultReceiptMethodCode?: string;
  deliveryMethodCode?: string;
  contact?: string;
  phone?: string;
  mobile?: string;
  email?: string;
  address?: string;
  contacts?: string | ContactItem[];
  enabled: boolean;
  remark?: string;
}

interface OptionItem {
  id: number;
  name: string;
  isDefault?: boolean;
}

interface CodeOptionItem {
  id: number;
  code: string;
  name: string;
  isDefault?: boolean;
}

interface ContactItem {
  name?: string;
  phone?: string;
  mobile?: string;
  email?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const contactQuery = ref('');
const phoneQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const categoryFilter = ref<number | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<ErpCustomer[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const categoryOptions = ref<OptionItem[]>([]);
const settlementMethodOptions = ref<CodeOptionItem[]>([]);
const receiptMethodOptions = ref<CodeOptionItem[]>([]);
const deliveryMethodOptions = ref<CodeOptionItem[]>([]);

const defaultColumns = ['code', 'name', 'category', 'contact', 'phone', 'email', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-customer', defaultColumns);

const formData = reactive({
  code: '',
  name: '',
  categoryId: null as number | null,
  defaultSettlementMethodCode: '',
  defaultReceiptMethodCode: '',
  deliveryMethodCode: '',
  contact: '',
  phone: '',
  mobile: '',
  email: '',
  address: '',
  contacts: [] as ContactItem[],
  enabled: true,
  remark: ''
});

const rules: FormRules = {
  code: [{ required: true, message: t('message.required'), trigger: 'blur' }],
  name: [{ required: true, message: t('message.required'), trigger: 'blur' }],
  categoryId: [{ required: true, message: t('message.required'), trigger: 'change' }]
};

const canShow = (key: string) => isVisible(key);

const getCategoryName = (id?: number) => categoryOptions.value.find(item => item.id === id)?.name || '-';

const fetchCategories = async () => {
  try {
    const res: any = await request.get('/erp/customer-categories');
    categoryOptions.value = res.data.data || [];
    if (!formData.categoryId) {
      const defaultCategory = categoryOptions.value.find(item => item.isDefault);
      if (defaultCategory) {
        formData.categoryId = defaultCategory.id;
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchSettlementMethods = async () => {
  try {
    const res: any = await request.get('/erp/settlement-methods', { params: { enabled: true } });
    settlementMethodOptions.value = res.data.data || [];
    if (showModal.value && !isEditing.value) {
      applyDefaultMethods();
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchReceiptMethods = async () => {
  try {
    const res: any = await request.get('/erp/receipt-methods', { params: { enabled: true } });
    receiptMethodOptions.value = res.data.data || [];
    if (showModal.value && !isEditing.value) {
      applyDefaultMethods();
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchDeliveryMethods = async () => {
  try {
    const res: any = await request.get('/erp/delivery-methods', { params: { enabled: true } });
    deliveryMethodOptions.value = res.data.data || [];
    if (showModal.value && !isEditing.value) {
      applyDefaultMethods();
    }
  } catch (error) {
    notifyError(error);
  }
};

const applyDefaultMethods = () => {
  if (!formData.defaultSettlementMethodCode && settlementMethodOptions.value.length) {
    const defaultItem = settlementMethodOptions.value.find(item => item.isDefault) ?? settlementMethodOptions.value[0];
    if (defaultItem) {
      formData.defaultSettlementMethodCode = defaultItem.code;
    }
  }
  if (!formData.defaultReceiptMethodCode && receiptMethodOptions.value.length) {
    const defaultItem = receiptMethodOptions.value.find(item => item.isDefault) ?? receiptMethodOptions.value[0];
    if (defaultItem) {
      formData.defaultReceiptMethodCode = defaultItem.code;
    }
  }
  if (!formData.deliveryMethodCode && deliveryMethodOptions.value.length) {
    const defaultItem = deliveryMethodOptions.value.find(item => item.isDefault) ?? deliveryMethodOptions.value[0];
    if (defaultItem) {
      formData.deliveryMethodCode = defaultItem.code;
    }
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
    if (contactQuery.value) params.contact = contactQuery.value.trim();
    if (phoneQuery.value) params.phone = phoneQuery.value.trim();
    if (statusFilter.value !== 'all') params.enabled = statusFilter.value === 'enabled';
    if (categoryFilter.value) params.categoryId = categoryFilter.value;

    const res: any = await request.get('/erp/customers/page', { params });
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

const handleReset = () => {
  searchQuery.value = '';
  contactQuery.value = '';
  phoneQuery.value = '';
  statusFilter.value = 'all';
  categoryFilter.value = null;
  handleSearch();
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
  const defaultCategory = categoryOptions.value.find(item => item.isDefault);
  if (defaultCategory) {
    formData.categoryId = defaultCategory.id;
  }
  applyDefaultMethods();
  showModal.value = true;
  fetchNextCustomerCode();
};

const openEditModal = (row: ErpCustomer) => {
  isEditing.value = true;
  currentId.value = row.id;
  formData.code = row.code;
  formData.name = row.name;
  formData.categoryId = row.categoryId || null;
  formData.defaultSettlementMethodCode = row.defaultSettlementMethodCode || '';
  formData.defaultReceiptMethodCode = row.defaultReceiptMethodCode || '';
  formData.deliveryMethodCode = row.deliveryMethodCode || '';
  formData.contact = row.contact || '';
  formData.phone = row.phone || '';
  formData.mobile = row.mobile || '';
  formData.email = row.email || '';
  formData.address = row.address || '';
  formData.contacts = buildInitialContacts(row);
  formData.enabled = row.enabled;
  formData.remark = row.remark || '';
  showModal.value = true;
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.categoryId = null;
  formData.defaultSettlementMethodCode = '';
  formData.defaultReceiptMethodCode = '';
  formData.deliveryMethodCode = '';
  formData.contact = '';
  formData.phone = '';
  formData.mobile = '';
  formData.email = '';
  formData.address = '';
  formData.contacts = [];
  formData.enabled = true;
  formData.remark = '';
  formRef.value?.clearValidate();
};

const parseContacts = (raw?: unknown) => {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw as ContactItem[];
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
};

const addContact = () => {
  formData.contacts.push({ name: '', phone: '', mobile: '', email: '' });
};

const removeContact = (index: number) => {
  formData.contacts.splice(index, 1);
};

const buildContactsPayload = () => {
  const cleaned = formData.contacts
    .map(item => ({
      name: item.name?.trim() || '',
      phone: item.phone?.trim() || '',
      mobile: item.mobile?.trim() || '',
      email: item.email?.trim() || ''
    }))
    .filter(item => item.name || item.phone || item.mobile || item.email);
  return cleaned.length ? JSON.stringify(cleaned) : '';
};

const buildPrimaryContactFields = () => {
  const primaryContact = formData.contacts
    .map(item => ({
      name: item.name?.trim() || '',
      phone: item.phone?.trim() || '',
      mobile: item.mobile?.trim() || '',
      email: item.email?.trim() || ''
    }))
    .find(item => item.name || item.phone || item.mobile || item.email);

  return {
    contact: primaryContact?.name || '',
    phone: primaryContact?.phone || '',
    mobile: primaryContact?.mobile || '',
    email: primaryContact?.email || ''
  };
};

const buildInitialContacts = (row: ErpCustomer) => {
  const parsedContacts = parseContacts(row.contacts);
  if (parsedContacts.length > 0) {
    return parsedContacts;
  }

  if (row.contact || row.phone || row.mobile || row.email) {
    return [{
      name: row.contact || '',
      phone: row.phone || '',
      mobile: row.mobile || '',
      email: row.email || ''
    }];
  }

  return [];
};

const fetchNextCustomerCode = async () => {
  try {
    const res: any = await request.get('/erp/customers/next-code');
    if (res.data.code === 200) {
      formData.code = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const saveData = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    const payload = {
      ...formData,
      ...buildPrimaryContactFields(),
      contacts: buildContactsPayload()
    };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/customers/${currentId.value}`, payload)
      : await request.post('/erp/customers', payload);

    if (res.data.code === 200) {
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpCustomer) => {
  try {
    await request.delete(`/erp/customers/${row.id}`);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchCategories();
  fetchSettlementMethods();
  fetchReceiptMethods();
  fetchDeliveryMethods();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchCategories();
  fetchSettlementMethods();
  fetchReceiptMethods();
  fetchDeliveryMethods();
  fetchList();
});
</script>

<style scoped>
.contact-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.contact-table :deep(.el-table) {
  width: 100%;
  max-width: 960px;
  box-sizing: border-box;
  margin: 0 auto;
}

.contact-table :deep(.el-table .el-input__wrapper) {
  box-shadow: none;
}

.contact-table__actions {
  display: flex;
  justify-content: center;
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px 16px;
}

.form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.span-2 {
  grid-column: 1 / -1;
}

.no-label :deep(.el-form-item__label) {
  display: none;
}

.no-label :deep(.el-form-item__content) {
  margin-left: 0 !important;
  padding-left: 0;
  width: 100%;
  display: block;
}
</style>
