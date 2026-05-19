<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCustomerDebtManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar finance-toolbar">
          <div class="table-filters finance-filters finance-filters--summary">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('field.customer')"
              class="table-search finance-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="finance-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe :empty-text="$t('table.empty')" table-key="erp-customer-debt-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('customerName')" prop="customerName" :label="$t('field.customer')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('totalDebt')" prop="totalDebt" :label="$t('field.customerDebtTotal')" min-width="160">
            <template #default="{ row }">
              {{ formatAmount(row.totalDebt) }}
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

const { notifyError } = useApiError();
const defaultColumns = ['customerName', 'totalDebt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-finance-customer-debt', defaultColumns);
const canShow = (key: string) => isVisible(key);

const tableData = ref<Array<{ customerId: number; customerName: string; totalDebt: number }>>([]);
const allTableData = ref<Array<{ customerId: number; customerName: string; totalDebt: number }>>([]);
const searchQuery = ref('');

const formatAmount = (value: number | string) => {
  const num = Number(value || 0);
  if (Number.isNaN(num)) return '0.00';
  return num.toFixed(2);
};

const fetchData = async () => {
  try {
    const res: any = await request.get('/erp/finance/customer-debts');
    if (res.data.code === 200) {
      allTableData.value = res.data.data || [];
      applySearch();
    }
  } catch (error) {
    notifyError(error);
  }
};

const applySearch = () => {
  tableData.value = filterByFuzzyKeyword(allTableData.value, searchQuery.value, row => [row.customerName]);
};

const handleSearch = () => {
  fetchData();
};

onMounted(() => {
  fetchTenantKeys();
  fetchData();
});
</script>
